package com.creator.pandayaand.live.filter

import android.content.Context
import android.graphics.Color
import android.util.Log
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
 *   // 프리셋 전환 후 UI 갱신:
 *   panel.refresh()
 *
 * 루트는 투명·비클릭이라 패널/버튼 외 영역의 터치는 아래(WebView)로 통과한다.
 */
class FilterTuningPanel(
    context: Context,
    private val manager: FaceFilterManager
) : FrameLayout(context) {

    private val panel: LinearLayout
    private val rotationModes = intArrayOf(0, 90, 180, 270)
    private val refreshers = mutableListOf<() -> Unit>()

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
        addView(toggle, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(180); rightMargin = dp(8)
        })

        // 슬라이더 패널 (초기 숨김)
        panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#D9111111"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
            isClickable = true
        }
        addView(panel, LayoutParams(dp(260), WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(216); rightMargin = dp(8)
        })

        toggle.setOnClickListener {
            if (panel.visibility == View.GONE) {
                refresh()   // 패널 열 때 항상 최신값으로 동기화
                panel.visibility = View.VISIBLE
            } else {
                panel.visibility = View.GONE
            }
        }

        buildControls()
    }

    /**
     * 모든 슬라이더를 manager 현재값으로 갱신하고 Logcat 에 값 덤프.
     * 프리셋 전환(setFilterMode) 후 호출해야 UI 가 동기화된다.
     */
    fun refresh() {
        refreshers.forEach { it() }
        Log.i("FACE_FILTER", buildString {
            appendLine("[패널 갱신] 현재 필터 파라미터 ─────────────────")
            appendLine("  마스크 크기   : ${"%.2f".format(manager.getMaskExpand())}")
            appendLine("  경계 부드러움 : ${"%.1f".format(manager.getBlurRadius())}")
            appendLine("  효과 강도     : ${"%.0f%%".format(manager.getIntensity() * 100)}")
            appendLine("  보정 강도     : ${"%.0f%%".format(manager.getToneStrength() * 100)}")
            appendLine("  하이라이트 압축: ${"%.2f".format(manager.getToneHighlight())}")
            appendLine("  섀도우 복원   : ${"%.2f".format(manager.getToneShadow())}")
            appendLine("  압축 시작점   : ${"%.2f".format(manager.getToneKnee())}")
            appendLine("  감마          : ${"%.2f".format(manager.getToneGamma())}")
            appendLine("  얼굴 우선     : ${"%.0f%%".format(manager.getFacePriority() * 100)}")
            append(  "  얼굴 밝기     : ${"%.2f".format(manager.getFaceBrighten())}")
        })
    }

    private fun buildControls() {
        title("얼굴 필터 미세조정")

        refreshers += slider("마스크 크기",   1.00f, 1.40f, 0.01f, { manager.getMaskExpand()  }, { "%.2f".format(it)         }) { manager.setMaskExpand(it)  }
        refreshers += slider("경계 부드러움", 0.5f,  5.0f,  0.1f,  { manager.getBlurRadius()  }, { "%.1f".format(it)         }) { manager.setBlurRadius(it)  }
        refreshers += slider("효과 강도",     0f,    1.0f,  0.05f, { manager.getIntensity()   }, { "%.0f%%".format(it * 100) }) { manager.setIntensity(it)   }

        // 회전 + 미러
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) }
        }
        val rotBtn = Button(context).apply {
            text = "회전 ${manager.getRotationMode()}°"
            setTextColor(Color.WHITE); textSize = 12f
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        rotBtn.setOnClickListener {
            val next = rotationModes[(rotationModes.indexOf(manager.getRotationMode()).coerceAtLeast(0) + 1) % rotationModes.size]
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
        row.addView(mirBtn, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin  = dp(4) })
        panel.addView(row)

        title("역광·빛번짐 보정")

        refreshers += slider("보정 강도",     0f,   1.0f, 0.05f, { manager.getToneStrength()  }, { "%.0f%%".format(it * 100) }) { manager.setToneStrength(it)  }
        refreshers += slider("하이라이트 압축", 0.4f, 1.0f, 0.01f, { manager.getToneHighlight() }, { "%.2f".format(it)          }) { manager.setToneHighlight(it) }
        refreshers += slider("섀도우 복원",   1.0f, 1.8f, 0.01f, { manager.getToneShadow()    }, { "%.2f".format(it)          }) { manager.setToneShadow(it)    }
        refreshers += slider("압축 시작점",   0.4f, 0.9f, 0.01f, { manager.getToneKnee()      }, { "%.2f".format(it)          }) { manager.setToneKnee(it)      }
        refreshers += slider("감마",          0.7f, 1.3f, 0.01f, { manager.getToneGamma()     }, { "%.2f".format(it)          }) { manager.setToneGamma(it)     }
        refreshers += slider("얼굴 우선",     0f,   1.0f, 0.05f, { manager.getFacePriority()  }, { "%.0f%%".format(it * 100) }) { manager.setFacePriority(it)  }
        refreshers += slider("얼굴 밝기",     1.0f, 1.5f, 0.01f, { manager.getFaceBrighten()  }, { "%.2f".format(it)          }) { manager.setFaceBrighten(it)  }
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
     * getter 로 manager 현재값을 읽어 초기 표시 및 refresh() 에서 재동기화.
     * 반환값은 refresh 시 호출할 람다.
     */
    private fun slider(
        label: String,
        min: Float, max: Float, step: Float,
        getter: () -> Float,
        format: (Float) -> String,
        onChange: (Float) -> Unit
    ): () -> Unit {
        val steps = Math.round((max - min) / step)
        val header = TextView(context).apply {
            setTextColor(Color.WHITE); textSize = 12f
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) }
        }

        fun updateUI(v: Float) { header.text = "$label : ${format(v)}" }

        val seek = SeekBar(context).apply {
            this.max = steps
            val accent = Color.parseColor("#FFCC66")
            val track  = Color.parseColor("#66FFFFFF")
            progressTintList           = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList              = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(track)
        }

        val refresher = {
            val v = getter().coerceIn(min, max)
            updateUI(v)
            seek.progress = Math.round((v - min) / step).coerceIn(0, steps)
        }
        refresher() // 초기값 세팅

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val v = (min + progress * step).coerceIn(min, max)
                updateUI(v)
                onChange(v)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        panel.addView(header)
        panel.addView(seek, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return refresher
    }

    companion object {
        const val ENABLED = true

        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
