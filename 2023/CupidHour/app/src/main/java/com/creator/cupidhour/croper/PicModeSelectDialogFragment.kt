package com.creator.cupidhour.croper

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import com.creator.cupidhour.croper.GOTOConstants.PicModes

/**
 * @author GT
 */
class PicModeSelectDialogFragment : DialogFragment() {
    private val picMode = arrayOf<String>(PicModes.Companion.CAMERA, PicModes.Companion.GALLERY)
    private var iPicModeSelectListener: IPicModeSelectListener? = null
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Select Mode")
            .setItems(picMode) { dialog, which ->
                if (iPicModeSelectListener != null) iPicModeSelectListener!!.onPicModeSelected(
                    picMode[which]
                )
            }
        return builder.create()
    }

    fun setiPicModeSelectListener(iPicModeSelectListener: IPicModeSelectListener?) {
        this.iPicModeSelectListener = iPicModeSelectListener
    }

    interface IPicModeSelectListener {
        fun onPicModeSelected(mode: String)
    }
}