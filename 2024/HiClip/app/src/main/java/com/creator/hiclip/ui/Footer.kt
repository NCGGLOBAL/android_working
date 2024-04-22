package com.creator.hiclip.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout

class Footer(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs),
    View.OnClickListener {
    private val mButtons: ArrayList<Button>? = null
    override fun onClick(v: View) {
        try {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}