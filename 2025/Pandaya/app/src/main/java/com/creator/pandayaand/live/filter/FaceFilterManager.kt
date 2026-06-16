package com.creator.pandayaand.live.filter

import android.content.Context
import android.util.Log
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyDenoiseFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyIllusionFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyProFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautySkinWhitenFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautySmoothFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautySoftExtFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautySoftFilter
import com.ksyun.media.streamer.filter.imgtex.ImgBeautySoftSharpenFilter
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt
import com.ksyun.media.streamer.kit.KSYStreamer
import com.ksyun.media.streamer.util.gles.GLRender
import java.util.concurrent.atomic.AtomicLong

/**
 * MediaPipe 얼굴 인식 + "얼굴 영역에만 KSYLive 내장 필터 적용" 파이프라인의 생명주기 관리.
 *
 * 핵심 아이디어 (KSYLive 필터 체인 활용):
 *   카메라 OES
 *     → [OriginalCaptureFilter]  뷰티 적용 전 원본을 2D 로 보관 (패스스루)
 *     → [KSYLive 내장 뷰티필터]  전체 프레임에 기존 효과 그대로 적용
 *     → [FaceMaskBlendFilter]    mix(원본, 필터결과, 얼굴마스크) → 얼굴만 필터, 배경 원본
 *     → preview / encoder
 *
 *   내장 필터를 그대로 재사용하므로 각 필터타입의 효과가 기존과 픽셀 단위로 동일하다.
 *   다만 적용 범위가 "전체 프레임"이 아니라 "얼굴 영역"으로 한정된다.
 *
 * 사용 흐름:
 *   1) FaceFilterManager(context)
 *   2) attachStreamer(mStreamer)   ← initCamera() 직후
 *   3) setFilterMode(keyType)      ← ACT1029 핸들러 (서버 key_type 0~12)
 *   4) release()                   ← onDestroy()
 */
class FaceFilterManager(private val context: Context) {

    private val TAG = "FACE_FILTER"

    val landmarkDetector = FaceLandmarkDetector(context)
    private var streamer: KSYStreamer? = null
    private var frameCounter = AtomicLong(0L)
    private var detectorInitialized = false

    // ── 런타임 튜닝 상태 ───────────────────────────────────────────────────
    // 필터 전환 시 blend 인스턴스가 새로 생성되므로, 값은 여기에 보관해두고
    // 새 blend 생성 직후 주입한다. setter 는 보관값 갱신 + 현재 활성 필터에 즉시 반영.
    private var tuneMaskExpand = 1.18f
    private var tuneBlurRadius = 2.6f
    private var tuneIntensity  = 1.0f
    private var currentBlend: FaceMaskBlendFilter? = null

    fun getMaskExpand() = tuneMaskExpand
    fun getBlurRadius() = tuneBlurRadius
    fun getIntensity()  = tuneIntensity
    fun getRotationMode() = landmarkDetector.rotationMode
    fun getMirrorX()      = landmarkDetector.mirrorX

    fun setMaskExpand(v: Float) { tuneMaskExpand = v; currentBlend?.setMaskExpand(v) }
    fun setBlurRadius(v: Float) { tuneBlurRadius = v; currentBlend?.setBlurRadius(v) }
    fun setIntensity(v: Float)  { tuneIntensity = v;  currentBlend?.setIntensity(v) }
    fun setRotationMode(v: Int) { landmarkDetector.rotationMode = v }
    fun setMirrorX(v: Boolean)  { landmarkDetector.mirrorX = v }

    /**
     * 서버 key_type(1~12) → KSYLive 내장 뷰티필터 인스턴스 직접 생성.
     *
     * KSYLive 의 setFilter(GLRender, int) 가 내부적으로 생성하는 것과 동일한 클래스/인자.
     * (디컴파일로 확인한 매핑: SOFT=16…PRO4=27)
     *   직접 생성해서 [capture, builtin, blend] 체인을 단 한 번의 setFilter(List)로 등록한다.
     *   setFilter 를 두 번(int 버전 후 List 버전) 호출하면 중간 상태가 GL 스레드와 경쟁해
     *   필터 전환 시 파이프라인이 멈추는 문제가 있어, 단일 호출로 처리한다.
     */
    private fun createBuiltinFilter(keyType: Int, render: GLRender): ImgFilterBase? = when (keyType) {
        1  -> ImgBeautySoftFilter(render)
        2  -> ImgBeautySkinWhitenFilter(render)
        3  -> ImgBeautyIllusionFilter(render)
        4  -> ImgBeautyDenoiseFilter(render)
        5  -> ImgBeautySmoothFilter(render, context)
        6  -> ImgBeautySoftExtFilter(render)
        7  -> ImgBeautySoftSharpenFilter(render)
        8  -> ImgBeautyProFilter(render, context)
        9  -> ImgBeautyProFilter(render, context, 1)
        10 -> ImgBeautyProFilter(render, context, 2)
        11 -> ImgBeautyProFilter(render, context, 3)
        12 -> ImgBeautyProFilter(render, context, 4)
        else -> null
    }

    // ── 초기화 ────────────────────────────────────────────────────────────

    /**
     * KSYStreamer 연결 및 MediaPipe 초기화.  KSYStreamer 생성 직후 호출.
     */
    fun attachStreamer(streamer: KSYStreamer) {
        this.streamer = streamer

        if (!detectorInitialized) {
            try {
                landmarkDetector.initialize()
                detectorInitialized = true
                Log.i(TAG, "MediaPipe FaceLandmarker 초기화 완료")
            } catch (e: Exception) {
                Log.e(TAG, "MediaPipe 초기화 실패 — face_landmarker.task 확인 필요: ${e.message}")
            }
        }

        // 카메라 프리뷰 YUV(NV21) 프레임을 MediaPipe 에 비동기 전달
        streamer.setOnPreviewFrameListener { nv21, width, height, _ ->
            if (detectorInitialized) {
                val ts = frameCounter.addAndGet(33L)  // ~30fps 타임스탬프
                landmarkDetector.detectAsync(nv21, width, height, ts)
            }
        }
    }

    // ── 필터 전환 ─────────────────────────────────────────────────────────

    /**
     * ACT1029 핸들러에서 호출.  서버 key_type 으로 얼굴 영역 필터를 전환한다.
     *
     *   key_type 0      → 필터 OFF (원본 영상)
     *   key_type 1~12   → 해당 KSYLive 내장 필터를 **얼굴 영역에만** 적용
     */
    fun setFilterMode(keyType: Int) {
        val s = streamer ?: run {
            Log.w(TAG, "setFilterMode: streamer 미연결")
            return
        }
        val mgt    = s.imgTexFilterMgt
        val render = s.glRender

        // 0 = 필터 OFF: 체인 제거, 원본 그대로
        if (keyType == 0) {
            mgt.setFilter(render, ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE)
            currentBlend = null
            Log.i(TAG, "필터 OFF (key_type=0)")
            return
        }

        val builtin = createBuiltinFilter(keyType, render)
        if (builtin == null) {
            Log.w(TAG, "알 수 없는 key_type=$keyType → 무시")
            return
        }

        // MediaPipe 미초기화 시: 얼굴 마스킹 불가 → 내장 필터만 전체 프레임에 적용(fallback)
        if (!detectorInitialized) {
            Log.w(TAG, "MediaPipe 미초기화 — 전체 프레임 내장 필터로 fallback (key_type=$keyType)")
            mgt.setFilter(builtin)
            return
        }

        // 원본 캡처 + 내장필터 + 마스크 블렌드를 한 체인으로 구성.
        // setFilter(List) 가 이전 필터를 release 하므로 모든 필터는 매번 새로 생성.
        val holder  = OriginalFrameHolder()
        val capture = OriginalCaptureFilter(render, holder)
        val blend   = FaceMaskBlendFilter(render, landmarkDetector, holder)

        // 보관 중인 튜닝 값을 새 blend 에 주입 (전환해도 값 유지)
        blend.setMaskExpand(tuneMaskExpand)
        blend.setBlurRadius(tuneBlurRadius)
        blend.setIntensity(tuneIntensity)
        currentBlend = blend

        val chain = ArrayList<ImgFilterBase>(3)
        chain.add(capture)
        chain.add(builtin)
        chain.add(blend)

        // 단일 setFilter 호출로 원자적 등록 (전환 시 파이프라인 멈춤 방지)
        mgt.setFilter(chain)
        Log.i(TAG, "얼굴 영역 필터 등록 완료 (key_type=$keyType, 체인=${chain.size}단)")
    }

    // ── 해제 ──────────────────────────────────────────────────────────────

    fun release() {
        currentBlend = null
        streamer?.setOnPreviewFrameListener(null)
        streamer = null
        landmarkDetector.release()
        detectorInitialized = false
        Log.d(TAG, "released")
    }
}
