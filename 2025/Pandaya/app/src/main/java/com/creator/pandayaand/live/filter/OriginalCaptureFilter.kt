package com.creator.pandayaand.live.filter

import android.opengl.GLES20
import android.util.Log
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter
import com.ksyun.media.streamer.framework.ImgTexFrame
import com.ksyun.media.streamer.util.gles.GLRender

/**
 * 필터 체인의 **맨 앞**에 위치하는 패스스루 필터.
 *
 * 역할: 카메라 원본(OES)을 업라이트 2D 로 렌더(= ImgTexFilter 기본 동작)한 뒤,
 *       그 결과를 **자기 소유의 독립 텍스처(ownTex)에 GPU 복사**하여 [OriginalFrameHolder] 에 등록한다.
 *       다음 단계(내장 뷰티필터 → FaceMaskBlendFilter)에서 "뷰티 적용 전 원본"으로 사용된다.
 *
 * 왜 복사하나:
 *   KSYLive 출력 텍스처(mOutTexture)는 FboManager 풀 소유라, 다운스트림에서 다른 필터의
 *   출력 FBO 로 재사용될 수 있다(원본↔출력 충돌 → 화면 검정). 풀과 무관한 ownTex 로 복사하면
 *   blend 단계까지 원본이 안전하게 보존되고 어떤 FBO 와도 충돌하지 않는다.
 *
 * 이미지 자체는 변형하지 않는다(픽셀 패스스루). 효과는 뒤따르는 내장 필터가 담당.
 */
class OriginalCaptureFilter(
    glRender: GLRender,
    private val holder: OriginalFrameHolder
) : ImgTexFilter(glRender) {

    private val TAG = "FACE_FILTER"
    private var logged = false

    private var ownTex = 0
    private var ownW = 0
    private var ownH = 0
    private val savedFbo = IntArray(1)

    override fun onInitialized() {
        super.onInitialized()
        // GL 컨텍스트 재생성 대비: 소유 텍스처 ID 무효화 → 다음 onDraw 에서 재생성
        ownTex = 0; ownW = 0; ownH = 0
    }

    override fun onDraw(frames: Array<ImgTexFrame>) {
        // 부모가 OES→2D 업라이트 렌더 수행 → mOutTexture 에 원본 2D 생성
        super.onDraw(frames)

        val fmt = if (frames.isNotEmpty()) frames[0].format else null
        val w = fmt?.width ?: 0
        val h = fmt?.height ?: 0
        if (w <= 0 || h <= 0 || mOutTexture == 0) return

        ensureOwnTex(w, h)

        // mOutTexture 가 attach 된 FBO 에서 ownTex 로 GPU 복사 (풀 수명과 분리)
        val srcFbo = mGLRender.fboManager.getFramebuffer(mOutTexture)
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, savedFbo, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, srcFbo)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ownTex)
        GLES20.glCopyTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, savedFbo[0])

        holder.textureId = ownTex
        holder.width = w
        holder.height = h

        if (!logged) {
            logged = true
            Log.d(TAG, "[Capture] 원본 복사: mOut=$mOutTexture → own=$ownTex ${w}x${h}")
        }
    }

    private fun ensureOwnTex(w: Int, h: Int) {
        if (ownTex != 0 && ownW == w && ownH == h) return
        deleteOwnTex()
        val arr = IntArray(1)
        GLES20.glGenTextures(1, arr, 0)
        ownTex = arr[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ownTex)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        ownW = w; ownH = h
    }

    private fun deleteOwnTex() {
        if (ownTex != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(ownTex), 0)
            ownTex = 0
        }
    }

    override fun onRelease() {
        deleteOwnTex()
        holder.clear()
        super.onRelease()
        Log.d(TAG, "[Capture] onRelease")
    }
}
