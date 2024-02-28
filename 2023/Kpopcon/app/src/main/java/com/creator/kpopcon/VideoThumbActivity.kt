package com.creator.kpopcon

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.creator.kpopcon.common.HNApplication
import com.creator.kpopcon.helpers.Constants
import com.creator.kpopcon.util.BitmapUtil
import kotlinx.android.synthetic.main.activity_video_thumb.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VideoThumbActivity: AppCompatActivity() {
    private val TAG = "VideoArchiveActivity"

    var selectedVideoPath: String? = null
    var outputVideoPath: String? = null
    var width: String? = null
    var height: String? = null
    var thumbnailPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_thumb)

        initIntent()
        nextButton.setOnClickListener { _ ->
            thumbnailPath?.let {
                intent.putExtra("thumbnailPath", it)
                intent.putExtra("selectedVideoPath", selectedVideoPath)
                setResult(Constants.FILECHOOSER_LOLLIPOP_REQ_VEDIO_CODE, intent)
                finish()
            }
        }
        initVideoThumbnailView()
    }

    private fun initIntent() {
        selectedVideoPath = intent.getStringExtra("selectedVideoPath")
        outputVideoPath = intent.getStringExtra("outputVideoPath")
        width = intent.getStringExtra("width")
        height = intent.getStringExtra("height")
    }

    private fun initVideoThumbnailView() {
        selectedVideoPath?.let {
            BitmapUtil.getVideoThumbnail(this, Uri.parse(it))?.let { bitmap ->
                thumbImageView.setImageBitmap(bitmap)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    stringBitmap = BitmapUtil.bitmapToString(bitmap)
                    val fileName = "videothumb"
                    val saveDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + "/" + fileName
                    thumbnailPath = BitmapUtil.bitmapToFile(bitmap, saveDir, fileName).absolutePath
                    Log.e(TAG, "initVideoThumbnailView thumbnailPath : ${thumbnailPath}")
//                }
            }
        }
    }
}