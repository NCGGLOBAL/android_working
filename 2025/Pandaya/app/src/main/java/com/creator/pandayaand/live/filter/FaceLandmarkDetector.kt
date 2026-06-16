package com.creator.pandayaand.live.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * MediaPipe FaceLandmarker wrapper.
 *
 * 사용법:
 *   1) initialize() — 앱 or Activity 시작 시 한 번 호출
 *   2) detectAsync(nv21, w, h, timestampMs) — KSYLive OnPreviewFrameListener 에서 호출
 *   3) latestOvalPoints.get() — GL 스레드에서 마스크 좌표 읽기
 *   4) release() — onDestroy 에서 호출
 *
 * 모델 파일: app/src/main/assets/face_landmarker.task
 * 다운로드: https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task
 */
class FaceLandmarkDetector(private val context: Context) {

    // MediaPipe face mesh face-oval indices (36 points, 반시계 방향)
    private val FACE_OVAL_INDICES = intArrayOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
        397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
        172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109
    )

    private var faceLandmarker: FaceLandmarker? = null

    /**
     * 최신 얼굴 윤곽선 좌표 목록 (최대 2명).
     * 각 항목: [x0,y0, x1,y1, ...] 형식, 값 범위 [0, 1].
     * null = 얼굴 미검출.  GL 스레드에서 volatile 하게 읽어도 안전.
     */
    val latestOvalPoints: AtomicReference<List<FloatArray>?> = AtomicReference(null)

    private val lastDetectTimestamp = AtomicLong(0L)
    private val DETECT_INTERVAL_MS = 33L  // ~30 fps 검출 상한

    /**
     * 센서(NV21) 정규화 좌표 → 화면 표시(업라이트 디스플레이) 텍스처 좌표 변환 파라미터.
     *
     * MediaPipe 는 NV21 센서 이미지(가로 방향)의 정규화 좌표를 주지만, 필터 체인의 텍스처는
     * 세로 업라이트로 회전·미러된 상태이므로 좌표를 맞춰줘야 마스크가 얼굴에 정렬된다.
     *
     * 전면 카메라 세로 모드 기본값. 마스크가 어긋나면 이 두 값만 조정:
     *   rotationMode ∈ {0,90,180,270}  (센서→화면 시계방향 회전각)
     *   mirrorX = true/false           (좌우 반전, 전면 카메라 셀피는 보통 미러)
     */
    @Volatile var rotationMode = 270
    @Volatile var mirrorX = true

    /** 이미지 좌표계(좌상단 원점, y 아래로 증가)에서 회전 + 미러 적용. */
    private fun transform(sx: Float, sy: Float): Pair<Float, Float> {
        val rx: Float; val ry: Float
        when (rotationMode) {
            90   -> { rx = 1f - sy; ry = sx }        // 90° CW
            180  -> { rx = 1f - sx; ry = 1f - sy }
            270  -> { rx = sy;      ry = 1f - sx }    // 90° CCW (= 270° CW)
            else -> { rx = sx;      ry = sy }         // 0°
        }
        val fx = if (mirrorX) 1f - rx else rx
        return Pair(fx, ry)
    }

    fun initialize() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .setDelegate(Delegate.GPU)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(3)
            .setOutputFaceBlendshapes(false)
            .setOutputFacialTransformationMatrixes(false)
            .setResultListener { result, _ -> onResult(result) }
            .setErrorListener { error ->
                android.util.Log.e("FaceLandmarkDetector", "MediaPipe error: ${error.message}")
            }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    private fun onResult(result: FaceLandmarkerResult) {
        if (result.faceLandmarks().isEmpty()) {
            latestOvalPoints.set(null)
            return
        }
        val faceList = result.faceLandmarks().map { landmarks ->
            val points = FloatArray(FACE_OVAL_INDICES.size * 2)
            FACE_OVAL_INDICES.forEachIndexed { i, idx ->
                if (idx < landmarks.size) {
                    val (tx, ty) = transform(landmarks[idx].x(), landmarks[idx].y())
                    points[i * 2]     = tx
                    points[i * 2 + 1] = ty
                }
            }
            points
        }
        latestOvalPoints.set(faceList)
    }

    /**
     * KSYLive OnPreviewFrameListener 에서 호출.
     * NV21 바이트를 비동기로 MediaPipe 에 전달한다.
     * 내부적으로 프레임 속도를 DETECT_INTERVAL_MS 로 제한한다.
     */
    fun detectAsync(nv21: ByteArray, width: Int, height: Int, timestampMs: Long) {
        val last = lastDetectTimestamp.get()
        if (timestampMs - last < DETECT_INTERVAL_MS) return
        if (!lastDetectTimestamp.compareAndSet(last, timestampMs)) return

        val bitmap = nv21ToBitmapScaled(nv21, width, height) ?: return
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            faceLandmarker?.detectAsync(mpImage, timestampMs)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * NV21 → Bitmap 변환 후 320 px 이하로 축소 (검출 속도 최적화).
     * 좌표는 정규화(0~1)이므로 축소해도 landmark 정확도 유지.
     */
    private fun nv21ToBitmapScaled(nv21: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream(nv21.size / 4)
            // JPEG 압축 품질 70 — 얼굴 검출에는 충분
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 70, out)
            val bytes = out.toByteArray()
            val full = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            val maxDim = 320
            val scale = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
            if (scale >= 1f) return full

            val scaled = Bitmap.createScaledBitmap(
                full, (width * scale).toInt(), (height * scale).toInt(), false
            )
            full.recycle()
            scaled
        } catch (e: Exception) {
            android.util.Log.w("FaceLandmarkDetector", "nv21ToBitmapScaled failed: ${e.message}")
            null
        }
    }

    fun release() {
        faceLandmarker?.close()
        faceLandmarker = null
        latestOvalPoints.set(null)
    }
}
