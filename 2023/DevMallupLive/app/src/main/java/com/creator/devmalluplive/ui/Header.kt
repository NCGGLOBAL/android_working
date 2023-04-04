package com.creator.devmalluplive.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.creator.devmalluplive.R

class Header(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs),
    View.OnClickListener {
    private val vewLogoTitle: View
    private val llHeaderLeft: LinearLayout
    private val tvHeaderLeft: TextView
    private val btnHeaderRight: Button
    private val txtHeaderTitle: TextView
    private var mLeftButtonType: ButtonType? = null
    private var mRightButtonType: ButtonType? = null

    enum class ButtonType {
        NONE, LOGO, BACK, SETTING, LOGIN, LOGOUT
    }

    init {
        addView(
            (getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                R.layout.header,
                this,
                false
            )
        )
        vewLogoTitle = findViewById(R.id.vewLogoTitle)
        llHeaderLeft = findViewById<View>(R.id.llHeaderLeft) as LinearLayout
        tvHeaderLeft = findViewById<View>(R.id.tvHeaderLeft) as TextView
        btnHeaderRight = findViewById<View>(R.id.btnHeaderRight) as Button
        txtHeaderTitle = findViewById<View>(R.id.txtHeaderTitle) as TextView
    }

    fun setTitle(title: String?) {
        vewLogoTitle.visibility = GONE
        txtHeaderTitle.visibility = VISIBLE
        txtHeaderTitle.text = title
    }

    fun setTitle(buttonType: ButtonType?) {
        vewLogoTitle.visibility = VISIBLE
        txtHeaderTitle.visibility = GONE
    }

    fun setLeftButton(buttonType: ButtonType?) {
        setLeftButton(buttonType, this)
    }

    /**
     *
     * set header left button.
     *
     * @param leftButtonText
     * text for left button. if null or empty, left button will not be displayed. if [.LOGO] logo will
     * be displayed.
     * @param onClickListener
     * set OnclickListener to left button.
     */
    fun setLeftButton(leftButtonText: String?, onClickListener: OnClickListener?) {
        llHeaderLeft.setOnClickListener(onClickListener)
        if (leftButtonText == null || leftButtonText.trim { it <= ' ' } == "") {
            llHeaderLeft.visibility = GONE
        } else {
            llHeaderLeft.visibility = VISIBLE
            tvHeaderLeft.text = leftButtonText
        }
        if (BUTTON_TEXT_BACK == leftButtonText) {
            tvHeaderLeft.text = ""
        }
    }

    fun setLeftButton(leftButtonType: ButtonType?, onClickListener: OnClickListener?) {
        mLeftButtonType = leftButtonType
        when (leftButtonType) {
            ButtonType.BACK -> {
                llHeaderLeft.visibility = VISIBLE
                llHeaderLeft.setOnClickListener(onClickListener)
            }
            else -> {}
        }
    }

    /**
     * set header right button.
     *
     * @param rightButtonText
     * text for right button. if null or empty, right button will not be displayed.
     *
     * @param onClickListener
     * set OnclickListener to right button.
     */
    fun setRightButton(rightButtonText: String?, onClickListener: OnClickListener?) {
        btnHeaderRight.setOnClickListener(onClickListener)
        if (rightButtonText == null || rightButtonText.trim { it <= ' ' } == "") {
            btnHeaderRight.visibility = GONE
        } else {
            btnHeaderRight.visibility = VISIBLE
            btnHeaderRight.text = rightButtonText
        }
    }

    fun setRightButton(rightButtonType: ButtonType?, onClickListener: OnClickListener?) {
        mRightButtonType = rightButtonType
        when (rightButtonType) {
            ButtonType.SETTING -> {
                btnHeaderRight.visibility = VISIBLE
                btnHeaderRight.setOnClickListener(onClickListener)
            }
            else -> {}
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llHeaderLeft -> if (mLeftButtonType == ButtonType.BACK) {
            }
            R.id.btnHeaderRight -> if (mRightButtonType == ButtonType.SETTING) {
            }
        }
    }

    companion object {
        const val LOGO = "logo"
        private const val BUTTON_TEXT_BACK = "뒤로"
    }
}