package com.aidapps.agcmall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aidapps.agcmall.croper.GOTOConstants.IntentExtras
import com.aidapps.agcmall.croper.GOTOConstants.PicModes
import com.aidapps.agcmall.croper.PicModeSelectDialogFragment
import com.aidapps.agcmall.croper.PicModeSelectDialogFragment.IPicModeSelectListener

/**
 * @author GT
 */
class ImageViewActivity : Activity(), IPicModeSelectListener {
    private val imgUri: String? = null
    private var mBtnUpdatePic: Button? = null
    private var mImageView: ImageView? = null
    private var mCardView: CardView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)
        mBtnUpdatePic = findViewById<View>(R.id.btnUpdatePic) as Button
        mImageView = findViewById<View>(R.id.iv_user_pic) as ImageView
        mCardView = findViewById<View>(R.id.cv_image_container) as CardView
        initCardView() //Resize card view according to activity dimension
        mBtnUpdatePic!!.setOnClickListener { showAddProfilePicDialog() }
        checkPermissions()
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1234
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent) {
        if (requestCode == REQUEST_CODE_UPDATE_PIC) {
            if (resultCode == RESULT_OK) {
                val imagePath = result.getStringExtra(IntentExtras.Companion.IMAGE_PATH)
                showCroppedImage(imagePath)
            } else if (resultCode == RESULT_CANCELED) {
                //TODO : Handle case
            } else {
                val errorMsg = result.getStringExtra(ImageCropActivity.Companion.ERROR_MSG)
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCroppedImage(mImagePath: String?) {
        if (mImagePath != null) {
            val myBitmap = BitmapFactory.decodeFile(mImagePath)
            mImageView!!.setImageBitmap(myBitmap)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //--------Private methods --------
    private fun initCardView() {
        mCardView!!.preventCornerOverlap = false
        val displayMetrics = applicationContext.resources.displayMetrics
        //We are implementing this only for portrait mode so width will be always less
        val w = displayMetrics.widthPixels
        val lp = mCardView!!.layoutParams as MarginLayoutParams
        val leftMargin = lp.leftMargin
        val topMargin = lp.topMargin
        val rightMargin = lp.rightMargin
        val paddingLeft = mCardView!!.paddingLeft
        val paddingRight = mCardView!!.paddingLeft
        val ch = w - leftMargin - rightMargin + paddingLeft + paddingRight
        mCardView!!.layoutParams.height = ch
    }

    private fun showAddProfilePicDialog() {
        val dialogFragment = PicModeSelectDialogFragment()
        dialogFragment.setiPicModeSelectListener(this)
        dialogFragment.show(fragmentManager, "picModeSelector")
    }

    private fun actionProfilePic(action: String) {
        val intent = Intent(this, ImageCropActivity::class.java)
        intent.putExtra("ACTION", action)
        startActivityForResult(intent, REQUEST_CODE_UPDATE_PIC)
    }

    override fun onPicModeSelected(mode: String) {
        val action: String = if (mode.equals(
                PicModes.Companion.CAMERA,
                ignoreCase = true
            )
        ) IntentExtras.Companion.ACTION_CAMERA else IntentExtras.Companion.ACTION_GALLERY
        actionProfilePic(action)
    }

    companion object {
        const val TAG = "ImageViewActivity"
        const val TEMP_PHOTO_FILE_NAME = "temp_photo.jpg"
        const val REQUEST_CODE_UPDATE_PIC = 0x1
    }
}