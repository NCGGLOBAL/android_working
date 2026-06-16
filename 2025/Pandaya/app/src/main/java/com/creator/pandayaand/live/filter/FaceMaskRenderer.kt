package com.creator.pandayaand.live.filter

import android.opengl.GLES20
import android.util.Log
import com.ksyun.media.streamer.util.gles.GlUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * GL 스레드 전용 — 얼굴 윤곽 폴리곤을 FBO 텍스처로 렌더링한다.
 *
 * 처리 순서:
 *   1) Pass 1 : 얼굴 타원 폴리곤 → fboIds[0] (흰색 채움)
 *   2) Pass 2 : 가로 가우시안 블러  fboIds[0] → fboIds[1]
 *   3) Pass 3 : 세로 가우시안 블러  fboIds[1] → fboIds[0]
 *
 * renderMask() 반환값 = 부드러운 마스크 텍스처 ID (GL_TEXTURE_2D).
 */
class FaceMaskRenderer {

    private val TAG = "FACE_FILTER"

    // ── 폴리곤 렌더 셰이더 ──────────────────────────────────────────────────
    // 입력 좌표: 정규화(0~1)  →  clip space(-1~1, Y 반전)
    private val POLY_VERTEX = """
        attribute vec2 aPosition;
        void main() {
            vec2 clip = aPosition * vec2(2.0, -2.0) + vec2(-1.0, 1.0);
            gl_Position = vec4(clip, 0.0, 1.0);
        }
    """.trimIndent()

    private val POLY_FRAGMENT = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
        }
    """.trimIndent()

    // ── 가우시안 블러 셰이더 (5-tap) ───────────────────────────────────────
    private val BLUR_VERTEX = """
        attribute vec2 aPosition;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = vec4(aPosition * 2.0 - 1.0, 0.0, 1.0);
            vTexCoord = aPosition;
        }
    """.trimIndent()

    private val BLUR_FRAGMENT = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uBlurDir;
        void main() {
            vec2 s = uBlurDir;
            float r = texture2D(uTexture, vTexCoord - s * 2.0).r * 0.0625
                    + texture2D(uTexture, vTexCoord - s       ).r * 0.25
                    + texture2D(uTexture, vTexCoord            ).r * 0.375
                    + texture2D(uTexture, vTexCoord + s       ).r * 0.25
                    + texture2D(uTexture, vTexCoord + s * 2.0 ).r * 0.0625;
            gl_FragColor = vec4(r, r, r, 1.0);
        }
    """.trimIndent()

    private var polyProgram  = -1
    private var blurProgram  = -1
    private var polyPosLoc   = -1
    private var blurPosLoc   = -1
    private var blurTexLoc   = -1
    private var blurDirLoc   = -1

    // 2개 FBO: [0] 폴리곤+블러 결과, [1] 블러 중간 버퍼
    private val fboIds  = IntArray(2)
    private val texIds  = IntArray(2)
    private var fboW = 0
    private var fboH = 0

    // 마스크는 소프트(블러)하므로 저해상도로 렌더 → 성능 대폭 절감.
    // 블렌드 셰이더가 GL_LINEAR 정규화 좌표로 샘플링하므로 해상도 무관.
    private val MASK_DOWNSCALE = 4

    // 얼굴 윤곽 대비 마스크 확장 비율 (1.0 = 윤곽 그대로). 경계가 얼굴 밖에 위치하도록.
    // 얼굴 가장자리까지 충분히 덮어 테두리 그림자(필터 미적용 영역)가 안 생기게 키움.
    // 런타임 튜닝 가능(@Volatile var) — 매 renderMask 호출 시 읽으므로 변경 즉시 반영.
    @Volatile var maskExpand = 1.18f

    // 페더(블러) 반경 배수 — 클수록 경계가 넓고 부드럽게 퍼진다. 런타임 튜닝 가능.
    @Volatile var blurRadius = 2.6f

    private var firstDetectionLogged = false

    // 센트로이드 팬용 정점 버퍼 (재사용)
    private var fanArray = FloatArray(0)

    // renderMask() 진입 시점의 GL state 백업용
    private val savedFbo      = IntArray(1)
    private val savedViewport = IntArray(4)

    // fullscreen quad (UV space 0~1)
    private val quadVerts = floatArrayOf(
        0f, 0f,  1f, 0f,  0f, 1f,
        1f, 0f,  1f, 1f,  0f, 1f
    )
    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(quadVerts.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .also { it.put(quadVerts); it.position(0) }

    // ── 초기화 (GL 스레드) ─────────────────────────────────────────────────

    /**
     * GL 컨텍스트가 재생성되면 이전 컨텍스트의 GL 오브젝트 ID는 모두 무효화된다.
     * glDelete* 없이 ID만 초기화해서 이후 init()/setupFbo()가 새로 할당하도록 한다.
     */
    fun invalidateGLResources() {
        // 기존 GL 객체 삭제 (같은 컨텍스트면 실제 해제, 새 컨텍스트면 no-op으로 안전)
        if (polyProgram != -1) { GLES20.glDeleteProgram(polyProgram); polyProgram = -1 }
        if (blurProgram != -1) { GLES20.glDeleteProgram(blurProgram); blurProgram = -1 }
        releaseFbo()  // fboW > 0이면 glDeleteTextures/Framebuffers 후 fboW=0 세팅
        firstDetectionLogged = false
        Log.d(TAG, "[MaskRenderer] GL 리소스 전체 해제, 재초기화 대기")
    }

    fun init() {
        polyProgram = GlUtil.createProgram(POLY_VERTEX, POLY_FRAGMENT)
        blurProgram = GlUtil.createProgram(BLUR_VERTEX, BLUR_FRAGMENT)
        polyPosLoc  = GLES20.glGetAttribLocation(polyProgram, "aPosition")
        blurPosLoc  = GLES20.glGetAttribLocation(blurProgram, "aPosition")
        blurTexLoc  = GLES20.glGetUniformLocation(blurProgram, "uTexture")
        blurDirLoc  = GLES20.glGetUniformLocation(blurProgram, "uBlurDir")
        Log.d(TAG, "[MaskRenderer] init: polyProg=$polyProgram blurProg=$blurProgram")
    }

    /**
     * 포맷 변경 시 FBO 크기 갱신 (GL 스레드).
     */
    fun setupFbo(frameWidth: Int, frameHeight: Int) {
        val width  = (frameWidth  / MASK_DOWNSCALE).coerceAtLeast(1)
        val height = (frameHeight / MASK_DOWNSCALE).coerceAtLeast(1)
        if (fboW == width && fboH == height) return
        releaseFbo()
        fboW = width
        fboH = height

        GLES20.glGenTextures(2, texIds, 0)
        GLES20.glGenFramebuffers(2, fboIds, 0)
        for (i in 0..1) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[i])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
            )
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboIds[i])
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texIds[i], 0
            )
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        Log.d(TAG, "[MaskRenderer] setupFbo: ${width}x${height} fbo=${fboIds.toList()} tex=${texIds.toList()}")
    }

    /**
     * 얼굴 윤곽 마스크 텍스처를 렌더링하고 텍스처 ID를 반환한다 (GL 스레드).
     * faceList == null 이면 0(검정) 마스크를 반환한다. 최대 2명 동시 처리.
     */
    fun renderMask(faceList: List<FloatArray>?): Int {
        if (polyProgram == -1 || fboW == 0) {
            Log.w(TAG, "[MaskRenderer] renderMask 스킵: polyProg=$polyProgram fboW=$fboW")
            return 0
        }
        if (!faceList.isNullOrEmpty() && !firstDetectionLogged) {
            firstDetectionLogged = true
            Log.i(TAG, "[MaskRenderer] 첫 얼굴 검출 성공! ${faceList.size}명")
        }

        // 진입 시점의 FBO/viewport 저장 (super.onDraw()가 기대하는 상태를 보존)
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, savedFbo, 0)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, savedViewport, 0)

        // Pass 1: 모든 얼굴 폴리곤 → fboIds[0] (하나의 클리어 후 루프로 그림)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboIds[0])
        GLES20.glViewport(0, 0, fboW, fboH)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (!faceList.isNullOrEmpty()) {
            GLES20.glUseProgram(polyProgram)
            GLES20.glEnableVertexAttribArray(polyPosLoc)
            for (ovalPoints in faceList) {
                if (ovalPoints.size >= 6) {
                    val fan = buildCentroidFan(ovalPoints)
                    val buf = GlUtil.createFloatBuffer(fan)
                    GLES20.glVertexAttribPointer(polyPosLoc, 2, GLES20.GL_FLOAT, false, 8, buf)
                    // TRIANGLE_FAN: [센트로이드, p0..pN, p0] → 볼록 폴리곤을 정확히 채움
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, fan.size / 2)
                }
            }
            GLES20.glDisableVertexAttribArray(polyPosLoc)
        }

        // 페더링: 가로/세로 가우시안 블러를 2회 반복(총 4패스)해 경계를 넓고 부드럽게.
        val sx = blurRadius / fboW
        val sy = blurRadius / fboH
        blurPass(texIds[0], fboIds[1], sx, 0f)   // H → tex1
        blurPass(texIds[1], fboIds[0], 0f, sy)   // V → tex0
        blurPass(texIds[0], fboIds[1], sx, 0f)   // H → tex1
        blurPass(texIds[1], fboIds[0], 0f, sy)   // V → tex0 (최종)

        // 진입 시점의 FBO/viewport 복원 → super.onDraw()가 자기 출력 대상에 정상 렌더
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, savedFbo[0])
        GLES20.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3])
        return texIds[0]
    }

    /**
     * 경계 정점(순서대로)을 [센트로이드, p0, p1, …, pN, p0] 형태의 TRIANGLE_FAN 정점으로 변환.
     * 센트로이드를 팬 중심으로 사용해 볼록 폴리곤이 빈틈없이 채워지도록 한다.
     *
     * maskExpand 만큼 센트로이드 기준으로 확장 → 마스크가 얼굴보다 약간 커져서
     * 페더링과 함께 경계가 얼굴 가장자리(턱선·헤어라인)에 도드라지지 않게 한다.
     */
    private fun buildCentroidFan(boundary: FloatArray): FloatArray {
        val n = boundary.size / 2
        var cx = 0f; var cy = 0f
        for (i in 0 until n) { cx += boundary[i * 2]; cy += boundary[i * 2 + 1] }
        cx /= n; cy /= n

        val expand = maskExpand
        // 정점 수: 1(센트로이드) + n(경계, 확장) + 1(첫 점 반복) = n + 2
        val needed = (n + 2) * 2
        if (fanArray.size != needed) fanArray = FloatArray(needed)
        fanArray[0] = cx; fanArray[1] = cy
        for (i in 0 until n) {
            fanArray[2 + i * 2]     = cx + (boundary[i * 2]     - cx) * expand
            fanArray[2 + i * 2 + 1] = cy + (boundary[i * 2 + 1] - cy) * expand
        }
        fanArray[needed - 2] = fanArray[2]
        fanArray[needed - 1] = fanArray[3]
        return fanArray
    }

    private fun blurPass(srcTex: Int, dstFbo: Int, dx: Float, dy: Float) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, dstFbo)
        GLES20.glViewport(0, 0, fboW, fboH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(blurProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, srcTex)
        GLES20.glUniform1i(blurTexLoc, 0)
        GLES20.glUniform2f(blurDirLoc, dx, dy)
        GLES20.glEnableVertexAttribArray(blurPosLoc)
        GLES20.glVertexAttribPointer(blurPosLoc, 2, GLES20.GL_FLOAT, false, 8, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(blurPosLoc)
    }

    fun release() {
        releaseFbo()
        if (polyProgram != -1) { GLES20.glDeleteProgram(polyProgram); polyProgram = -1 }
        if (blurProgram != -1) { GLES20.glDeleteProgram(blurProgram); blurProgram = -1 }
    }

    private fun releaseFbo() {
        if (fboW > 0) {
            GLES20.glDeleteTextures(2, texIds, 0)
            GLES20.glDeleteFramebuffers(2, fboIds, 0)
            fboW = 0; fboH = 0
        }
    }
}
