package com.creator.pandayaand.live.filter

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * 실무자용 얼굴 필터 미세조정 오버레이 패널.
 *
 * 화면 위에 떠 있는 작은 토글 버튼을 누르면 슬라이더 패널이 펼쳐진다.
 * 값을 바꾸면 [FaceFilterManager] 를 통해 현재 활성 필터에 **즉시** 반영된다.
 * (전환해도 값이 유지되도록 매니저가 보관)
 *
 * 사용: CameraActivity 에서
 *   val panel = FilterTuningPanel(this, faceFilterManager)
 *   addContentView(panel, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
 *
 * 루트는 투명·비클릭이라 패널/버튼 외 영역의 터치는 아래(WebView)로 통과한다.
 */
class FilterTuningPanel(
    context: Context,
    private val manager: FaceFilterManager
) : FrameLayout(context) {

    private val panel: LinearLayout
    private val rotationModes = intArrayOf(0, 90, 180, 270)

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    init {
        // 토글 버튼 (항상 표시)
        val toggle = Button(context).apply {
            text = "⚙ 필터조정"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC222222"))
            textSize = 12f
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        // 우상단 X(닫기) 버튼과 겹치지 않게 아래로 내림
        addView(toggle, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(144); rightMargin = dp(8)
        })

        // 슬라이더 패널 (초기 숨김)
        panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#D9111111"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
            isClickable = true   // 패널 위 터치는 아래로 통과시키지 않음
        }
        addView(panel, LayoutParams(dp(260), WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(180); rightMargin = dp(8)
        })

        toggle.setOnClickListener {
            panel.visibility = if (panel.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        buildControls()
    }

    private fun buildControls() {
        title("얼굴 필터 미세조정")

        // 마스크 크기: 1.00 ~ 1.40  (SeekBar 0..40)
        slider(
            label = "마스크 크기",
            min = 1.00f, max = 1.40f, step = 0.01f,
            initial = manager.getMaskExpand(),
            format = { "%.2f".format(it) }
        ) { manager.setMaskExpand(it) }

        // 페더(경계 부드러움): 0.5 ~ 5.0  (step 0.1)
        slider(
            label = "경계 부드러움",
            min = 0.5f, max = 5.0f, step = 0.1f,
            initial = manager.getBlurRadius(),
            format = { "%.1f".format(it) }
        ) { manager.setBlurRadius(it) }

        // 효과 강도: 0 ~ 1.0  (step 0.05)
        slider(
            label = "효과 강도",
            min = 0f, max = 1.0f, step = 0.05f,
            initial = manager.getIntensity(),
            format = { "%.0f%%".format(it * 100) }
        ) { manager.setIntensity(it) }

        // 회전 + 미러 (한 줄)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            lp.topMargin = dp(8); layoutParams = lp
        }
        val rotBtn = Button(context).apply {
            text = "회전 ${manager.getRotationMode()}°"
            setTextColor(Color.WHITE); textSize = 12f
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        rotBtn.setOnClickListener {
            val cur = manager.getRotationMode()
            val idx = (rotationModes.indexOf(cur).coerceAtLeast(0) + 1) % rotationModes.size
            val next = rotationModes[idx]
            manager.setRotationMode(next)
            rotBtn.text = "회전 ${next}°"
        }
        val mirBtn = Button(context).apply {
            text = if (manager.getMirrorX()) "미러 ON" else "미러 OFF"
            setTextColor(Color.WHITE); textSize = 12f
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        mirBtn.setOnClickListener {
            val next = !manager.getMirrorX()
            manager.setMirrorX(next)
            mirBtn.text = if (next) "미러 ON" else "미러 OFF"
        }
        row.addView(rotBtn, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = dp(4) })
        row.addView(mirBtn, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(4) })
        panel.addView(row)
    }

    private fun title(text: String) {
        panel.addView(TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#FFCC66"))
            textSize = 13f
            setPadding(0, 0, 0, dp(6))
        })
    }

    /**
     * 라벨 + 현재값 + SeekBar 한 묶음. 값 변경 시 onChange 즉시 호출.
     */
    private fun slider(
        label: String,
        min: Float, max: Float, step: Float,
        initial: Float,
        format: (Float) -> String,
        onChange: (Float) -> Unit
    ) {
        val steps = Math.round((max - min) / step)
        val header = TextView(context).apply {
            setTextColor(Color.WHITE); textSize = 12f
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            lp.topMargin = dp(8); layoutParams = lp
        }
        fun updateHeader(v: Float) { header.text = "$label : ${format(v)}" }
        updateHeader(initial)

        val seek = SeekBar(context).apply {
            this.max = steps
            progress = Math.round((initial - min) / step).coerceIn(0, steps)
            // 어두운 배경에서도 잘 보이도록 밝은 색으로 틴트
            val accent = Color.parseColor("#FFCC66")          // 채워진 트랙 + 핸들
            val track  = Color.parseColor("#66FFFFFF")          // 빈 트랙
            progressTintList          = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList             = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(track)
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = (min + progress * step).coerceIn(min, max)
                updateHeader(v)
                onChange(v)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        panel.addView(header)
        panel.addView(seek, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    companion object {
        /**
         * 패널 전체 On/Off 스위치 (테스트용).
         * false 로 두면 화면에 패널이 뜨지 않는다(코드는 그대로 둔 채 즉시 비활성화).
         * 완전 제거는 CameraActivity 의 [TUNING-PANEL] 마커 블록 + 이 파일 삭제.
         */
        const val ENABLED = true

        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
