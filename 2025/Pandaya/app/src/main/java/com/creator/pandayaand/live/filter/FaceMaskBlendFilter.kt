package com.creator.pandayaand.live.filter

import android.opengl.GLES20
import android.util.Log
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter
import com.ksyun.media.streamer.framework.ImgTexFormat
import com.ksyun.media.streamer.framework.ImgTexFrame
import com.ksyun.media.streamer.util.gles.GLRender

/**
 * 필터 체인의 **맨 뒤**에 위치하는 합성 필터.
 *
 *   입력 sTexture   = 내장 뷰티필터가 전체 프레임에 효과를 적용한 결과(업라이트 2D)
 *   uOriginalTex    = [OriginalCaptureFilter] 가 보관한 뷰티 적용 전 원본(업라이트 2D)
 *   uMaskTex        = MediaPipe 얼굴 윤곽으로 만든 소프트 마스크(얼굴=1, 배경=0)
 *
 *   출력 = mix(original, filtered, mask * intensity)
 *        → 얼굴 영역만 뷰티 효과, 나머지는 카메라 원본 그대로.
 *
 * 좌표계: sTexture / uOriginalTex / uMaskTex 모두 동일한 업라이트 디스플레이 좌표(0~1)이므로
 *         vTextureCoord 로 동일하게 샘플링하면 정렬된다.
 *
 * GL state 처리: 마스크 렌더(자체 FBO 변경)는 onDraw() 에서 super.onDraw() 호출 전에 수행하고,
 *               FaceMaskRenderer.renderMask() 가 진입 FBO/viewport 를 저장·복원하므로
 *               부모의 출력 렌더가 오염되지 않는다.
 */
class FaceMaskBlendFilter(
    glRender: GLRender,
    private val landmarkDetector: FaceLandmarkDetector,
    private val holder: OriginalFrameHolder
) : ImgTexFilter(glRender) {

    companion object {
        // sTexture 는 베이스가 자동 선언(2D/OES) 하므로 여기서 선언 금지.
        private const val FRAGMENT_SHADER_BODY = """
precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uOriginalTex;
uniform sampler2D uMaskTex;
uniform float uIntensity;

void main() {
    vec4 filtered = texture2D(sTexture,     vTextureCoord);
    vec4 original = texture2D(uOriginalTex, vTextureCoord);
    float m = clamp(texture2D(uMaskTex, vTextureCoord).r * uIntensity, 0.0, 1.0);
    gl_FragColor = mix(original, filtered, m);
}
"""
    }

    private val TAG = "FACE_FILTER"

    @Volatile private var intensity = 1.0f

    private val maskRenderer = FaceMaskRenderer()
    private var uOriginalLoc  = -1
    private var uMaskLoc      = -1
    private var uIntensityLoc = -1

    private var cachedMaskTexId = 0
    private var drawCount = 0L

    // onFormatChanged 가 onInitialized 보다 먼저 올 수 있어 마지막 포맷 보관
    private var lastFboW = 0
    private var lastFboH = 0

    init {
        mFragmentShaderBody = FRAGMENT_SHADER_BODY
    }

    override fun onInitialized() {
        super.onInitialized()
        maskRenderer.invalidateGLResources()
        maskRenderer.init()
        if (lastFboW > 0) maskRenderer.setupFbo(lastFboW, lastFboH)
        uOriginalLoc  = getUniformLocation("uOriginalTex")
        uMaskLoc      = getUniformLocation("uMaskTex")
        uIntensityLoc = getUniformLocation("uIntensity")
        drawCount = 0L
        Log.d(TAG, "[Blend] onInitialized: prog=$mProgramId uOrig=$uOriginalLoc uMask=$uMaskLoc uInt=$uIntensityLoc")
    }

    override fun onFormatChanged(format: ImgTexFormat) {
        super.onFormatChanged(format)
        lastFboW = format.width
        lastFboH = format.height
        maskRenderer.setupFbo(format.width, format.height)
        Log.d(TAG, "[Blend] onFormatChanged: ${format.width}x${format.height}")
    }

    override fun onDraw(frames: Array<ImgTexFrame>) {
        // 부모가 GL state 를 잡기 전에 마스크 선렌더 (renderMask 가 진입 상태 저장·복원)
        val faceList = landmarkDetector.latestOvalPoints.get()
        cachedMaskTexId = maskRenderer.renderMask(faceList)
        val n = ++drawCount
        if (n == 1L || n % 300 == 0L) {
            Log.d(TAG, "[Blend] onDraw#$n mask=$cachedMaskTexId orig=${holder.textureId} faces=${faceList?.size ?: "null"}")
        }
        super.onDraw(frames)
    }

    override fun onDrawArraysPre() {
        // 추가 텍스처 바인딩만 (FBO/프로그램 변경 금지)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, holder.textureId)
        GLES20.glUniform1i(uOriginalLoc, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cachedMaskTexId)
        GLES20.glUniform1i(uMaskLoc, 2)

        GLES20.glUniform1f(uIntensityLoc, intensity)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)  // 부모의 sTexture(필터결과) 용으로 복원
    }

    override fun onRelease() {
        Log.d(TAG, "[Blend] onRelease (frames=$drawCount)")
        maskRenderer.release()
        super.onRelease()
    }

    /** 얼굴 영역 효과 강도 [0.0~1.0]. 0=원본만, 1=뷰티 전체. */
    fun setIntensity(v: Float) { intensity = v.coerceIn(0f, 1f) }
    fun getIntensity(): Float = intensity

    /** 마스크 확장 비율(얼굴 윤곽 대비). 즉시 반영. */
    fun setMaskExpand(v: Float) { maskRenderer.maskExpand = v }

    /** 경계 페더링(블러) 반경 배수. 즉시 반영. */
    fun setBlurRadius(v: Float) { maskRenderer.blurRadius = v }
}
