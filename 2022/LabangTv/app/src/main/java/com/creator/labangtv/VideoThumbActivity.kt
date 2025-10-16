package com.creator.labangtv

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.creator.labangtv.helpers.Constants
import com.creator.labangtv.util.BitmapUtil
import com.creator.labangtv.util.LogUtil
import kotlin.math.roundToInt

class VideoThumbActivity: AppCompatActivity() {
    private val TAG = "VideoArchiveActivity"

    var selectedVideoPath: String? = null
    var outputVideoPath: String? = null
//    var width: String? = null
//    var height: String? = null
    var thumbnailPath: String? = null

    lateinit var thumbImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_thumb)

        initIntent()
        thumbImageView = findViewById(R.id.thumbImageView)
        findViewById<Button>(R.id.nextButton).setOnClickListener { _ ->
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
        thumbnailPath = intent.getStringExtra("thumbnailPath")
//        width = intent.getStringExtra("width")
//        height = intent.getStringExtra("height")
    }

    private fun initVideoThumbnailView() {
        selectedVideoPath?.let {
            BitmapUtil.getVideoThumbnail(this, Uri.parse(it))?.let { bitmap ->
                thumbImageView.setImageBitmap(bitmap)

                val resizeRate = if (bitmap.width > bitmap.height) {   //가로모드
                    (720F / bitmap.height.toFloat())
                } else {   //세로 또는 정사각형
                    (720F / bitmap.width.toFloat())
                }
                LogUtil.e("resizeRate : " + resizeRate)

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * resizeRate).roundToInt(), (bitmap.height * resizeRate).roundToInt(), false)

                LogUtil.e("resizedBitmap width : " + resizedBitmap.width)
                LogUtil.e("resizedBitmap height : " + resizedBitmap.height)

                val fileName = "videothumb"
                val saveDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + "/" + fileName
                thumbnailPath = BitmapUtil.bitmapToFile(resizedBitmap, saveDir, fileName).absolutePath
                Log.e(TAG, "initVideoThumbnailView thumbnailPath : ${thumbnailPath}")
            }
        }
    }
}