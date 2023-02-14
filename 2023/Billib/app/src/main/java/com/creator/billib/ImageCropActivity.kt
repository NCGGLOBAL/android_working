package com.creator.billib

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.creator.billib.croper.GOTOConstants.IntentExtras
import com.creator.billib.croper.InternalStorageContentProvider
import com.creator.billib.croper.L
import com.creator.billib.croper.Utils
import io.togoto.imagezoomcrop.cropoverlay.CropOverlayView
import io.togoto.imagezoomcrop.photoview.PhotoView
import java.io.*

/**
 * @author GT
 */
class ImageCropActivity : HelperActivity() {
    private var actionBar: ActionBar? = null
    var mImageView: PhotoView? = null
    var mCropOverlayView: CropOverlayView? = null

    //    Button btnRetakePic;
    //    Button btnFromGallery;
    //    Button btnDone;
    //    Button mBtnReset;
    //    View mMoveResizeText;
    //    RotationSeekBar mRotationBar;
    var mBtnUndoRotation: Button? = null
    private var mContentResolver: ContentResolver? = null
    private val IMAGE_MAX_SIZE = 1024
    private val mOutputFormat = CompressFormat.JPEG

    //Temp file to save cropped image
    private var mImagePath: String? = null
    private var mSaveUri: Uri? = null
    private var mImageUri: Uri? = null
    private val mDegree = 0f

    //File for capturing camera images
    private var mFileTemp: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)
        mContentResolver = contentResolver
        mImageView = findViewById<View>(R.id.iv_photo) as PhotoView
        mCropOverlayView = findViewById<View>(R.id.crop_overlay) as CropOverlayView
        //        btnRetakePic = (Button) findViewById(R.id.btnRetakePic);
//        btnFromGallery = (Button) findViewById(R.id.btnFromGallery);
//        btnDone = (Button) findViewById(R.id.btn_done);
//        mBtnReset = (Button) findViewById(R.id.btn_reset);
//        mMoveResizeText = findViewById(R.id.tv_move_resize_txt);
//        mRotationBar = (RotationSeekBar) findViewById(R.id.bar_rotation);
//        mBtnUndoRotation = (Button) findViewById(R.id.btn_undo);

//        btnRetakePic.setOnClickListener(btnRetakeListener);
//        btnFromGallery.setOnClickListener(btnFromGalleryListener);
//        btnDone.setOnClickListener(btnDoneListerner);
//        mBtnReset.setOnClickListener(btnResetListerner);
//        mBtnUndoRotation.setOnClickListener(btnUndoRotationListener);
        mImageView!!.setImageBoundsListener { mCropOverlayView!!.imageBounds }

        // initialize rotation seek bar
//        mRotationBar.setOnSeekBarChangeListener(new RotationSeekBar.OnRotationSeekBarChangeListener(mRotationBar) {
//
//            private float mLastAngle;
//
//            @Override
//            public void onRotationProgressChanged(@NonNull RotationSeekBar seekBar, float angle, float delta, boolean fromUser) {
//                mLastAngle = angle;
//                if (fromUser) {
//                    mImageView.setRotationBy(delta, false);
//                }
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                super.onStopTrackingTouch(seekBar);
//                if (Math.abs(mLastAngle) < ANCHOR_CENTER_DELTA) {
//                    mRotationBar.reset();
//                    mImageView.setRotationBy(0, true);
//                }
//            }
//        });

        // Set a toolbar to  replace to action bar
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // set actionBar
        actionBar = supportActionBar
        if (actionBar != null) {
            actionBar!!.setDisplayShowTitleEnabled(true)
            actionBar!!.setTitle("사진편집")
            val textview_rotation = findViewById<View>(R.id.textview_rotation) as TextView
            textview_rotation.setOnClickListener { mImageView!!.setRotationBy(mDegree + 90f) }
            val tv_header_right = findViewById<View>(R.id.textview_header_right) as TextView
            tv_header_right.setOnClickListener { saveUploadCroppedImage() }

//            tv_header_right.setVisibility(View.GONE);
        }
        // ============================================ default
        createTempFile()
        if (savedInstanceState == null || !savedInstanceState.getBoolean("restoreState")) {
            val action = intent.getStringExtra("ACTION")
            if (null != action) {
                when (action) {
                    IntentExtras.Companion.ACTION_CAMERA -> {
                        intent.removeExtra("ACTION")
                        takePic()
                        return
                    }
                    IntentExtras.Companion.ACTION_GALLERY -> {
                        intent.removeExtra("ACTION")
                        pickImage()
                        return
                    }
                }
            }
        }
        mImagePath = mFileTemp!!.path
        // ============================================
        val intent = intent
        if (intent.hasExtra("filePath")) {
            mImagePath = intent.getStringExtra("filePath")
        }
        mSaveUri = Utils.getImageUri(mImagePath)
        mImageUri = Utils.getImageUri(mImagePath)
        init()
    }

    override fun onStart() {
        super.onStart()
    }

    private fun init() {
        val bitmap = getBitmap(mImageUri)
        val drawable: Drawable = BitmapDrawable(resources, bitmap)
        val minScale = mImageView!!.setMinimumScaleToFit(drawable)
        mImageView!!.maximumScale = minScale * 3
        mImageView!!.mediumScale = minScale * 2
        mImageView!!.scale = minScale
        mImageView!!.setImageDrawable(drawable)

        //Initialize the MoveResize text
//        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mMoveResizeText.getLayoutParams();
//        lp.setMargins(0, Math.round(Edge.BOTTOM.getCoordinate()) + 20, 0, 0);
//        mMoveResizeText.setLayoutParams(lp);
    }

    private val btnDoneListerner = View.OnClickListener { saveUploadCroppedImage() }
    private fun saveUploadCroppedImage() {
        val saved = saveOutput()
        if (saved) {
            //USUALLY Upload image to server here
            val intent = Intent()
            Log.d("SeongKwon", "===========================$mImagePath")
            Log.d("SeongKwon", "===========================$mImageUri")
            Log.d("SeongKwon", "===========================$mSaveUri")
            intent.putExtra("isChanged", true)
            intent.putExtra(IntentExtras.Companion.IMAGE_PATH, mImagePath)
            setResult(RESULT_OK, intent)
            finish()
        } else {
            Toast.makeText(this, "Unable to save Image into your device.", Toast.LENGTH_LONG).show()
        }
    }

    //    private View.OnClickListener btnResetListerner = new View.OnClickListener() {
    //        @Override
    //        public void onClick(View v) {
    //            mRotationBar.reset();
    //            // init();
    //            mImageView.reset();
    //        }
    //    };
    private val btnRetakeListener = View.OnClickListener {
        if (null == mFileTemp) {
            createTempFile()
        }
        takePic()
    }

    //    private View.OnClickListener btnUndoRotationListener = new View.OnClickListener() {
    //        @Override
    //        public void onClick(View v) {
    //            mImageView.setRotationBy(0, true);
    //            mRotationBar.reset();
    //        }
    //    };
    private val btnFromGalleryListener = View.OnClickListener {
        if (null == mFileTemp) {
            createTempFile()
        }
        pickImage()
    }

    private fun createTempFile() {
        val state = Environment.getExternalStorageState()
        mFileTemp = if (Environment.MEDIA_MOUNTED == state) {
            File(Environment.getExternalStorageDirectory(), TEMP_PHOTO_FILE_NAME)
        } else {
            File(filesDir, TEMP_PHOTO_FILE_NAME)
        }
    }

    private fun takePic() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            var mImageCaptureUri: Uri? = null
            val state = Environment.getExternalStorageState()
            mImageCaptureUri = if (Environment.MEDIA_MOUNTED == state) {
                Uri.fromFile(mFileTemp)
            } else {
                /*
                 * The solution is taken from here: http://stackoverflow.com/questions/10042695/how-to-get-camera-result-as-a-uri-in-data-folder
	        	 */
                InternalStorageContentProvider.Companion.CONTENT_URI
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri)
            takePictureIntent.putExtra("return-data", true)
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Can't take picture", e)
            Toast.makeText(this, "Can't take picture", Toast.LENGTH_LONG).show()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("restoreState", true)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_GALLERY)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No image source available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        createTempFile()
        if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                mImagePath = mFileTemp!!.path
                mSaveUri = Utils.getImageUri(mImagePath)
                mImageUri = Utils.getImageUri(mImagePath)
                init()
            } else if (resultCode == RESULT_CANCELED) {
                userCancelled()
                return
            } else {
                errored("Error while opening the image file. Please try again.")
                return
            }
        } else if (requestCode == REQUEST_CODE_PICK_GALLERY) {
            if (resultCode == RESULT_CANCELED) {
                userCancelled()
                return
            } else if (resultCode == RESULT_OK) {
                try {
                    val inputStream = contentResolver.openInputStream(
                        result!!.data!!
                    ) // Got the bitmap .. Copy it to the temp file for cropping
                    val fileOutputStream = FileOutputStream(mFileTemp)
                    copyStream(inputStream, fileOutputStream)
                    fileOutputStream.close()
                    inputStream!!.close()
                    mImagePath = mFileTemp!!.path
                    mSaveUri = Utils.getImageUri(mImagePath)
                    mImageUri = Utils.getImageUri(mImagePath)
                    init()
                } catch (e: Exception) {
                    errored("Error while opening the image file. Please try again.")
                    L.e(e)
                    return
                }
            } else {
                errored("Error while opening the image file. Please try again.")
                return
            }
        }
    }

    private fun getBitmap(uri: Uri?): Bitmap? {
        var `in`: InputStream? = null
        var returnedBitmap: Bitmap? = null
        try {
            `in` = mContentResolver!!.openInputStream(uri!!)
            //Decode image size
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeStream(`in`, null, o)
            `in`!!.close()
            var scale = 1
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = Math.pow(
                    2.0,
                    Math.round(
                        Math.log(
                            IMAGE_MAX_SIZE / Math.max(o.outHeight, o.outWidth).toDouble()
                        ) / Math.log(0.5)
                    ).toInt().toDouble()
                ).toInt()
            }
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            `in` = mContentResolver!!.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(`in`, null, o2)
            `in`!!.close()

            //First check
            val ei = ExifInterface(uri.path!!)
            val orientation =
                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    returnedBitmap = rotateImage(bitmap, 90f)
                    //Free up the memory
                    bitmap!!.recycle()
                    bitmap = null
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    returnedBitmap = rotateImage(bitmap, 180f)
                    //Free up the memory
                    bitmap!!.recycle()
                    bitmap = null
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    returnedBitmap = rotateImage(bitmap, 270f)
                    //Free up the memory
                    bitmap!!.recycle()
                    bitmap = null
                }
                else -> returnedBitmap = bitmap
            }
            return returnedBitmap
        } catch (e: FileNotFoundException) {
            L.e(e)
        } catch (e: IOException) {
            L.e(e)
        }
        return null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
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

    private fun saveOutput(): Boolean {
        val croppedImage = mImageView!!.croppedImage
        if (mSaveUri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = mContentResolver!!.openOutputStream(mSaveUri!!)
                if (outputStream != null) {
                    croppedImage.compress(mOutputFormat, 90, outputStream)
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
                return false
            } finally {
                closeSilently(outputStream)
            }
        } else {
            Log.e(TAG, "not defined image url")
            return false
        }
        croppedImage.recycle()
        return true
    }

    fun closeSilently(c: Closeable?) {
        if (c == null) return
        try {
            c.close()
        } catch (t: Throwable) {
            // do nothing
        }
    }

    private fun rotateImage(source: Bitmap?, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source!!, 0, 0, source.width, source.height, matrix, true)
    }

    fun userCancelled() {
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    fun errored(msg: String?) {
        val intent = Intent()
        intent.putExtra(ERROR, true)
        if (msg != null) {
            intent.putExtra(ERROR_MSG, msg)
        }
        finish()
    }

    companion object {
        const val TAG = "ImageCropActivity"
        private const val ANCHOR_CENTER_DELTA = 10
        const val TEMP_PHOTO_FILE_NAME = "temp_photo.jpg"
        const val REQUEST_CODE_PICK_GALLERY = 0x1
        const val REQUEST_CODE_TAKE_PICTURE = 0x2
        const val REQUEST_CODE_CROPPED_PICTURE = 0x3
        const val ERROR_MSG = "error_msg"
        const val ERROR = "error"
        @Throws(IOException::class)
        private fun copyStream(input: InputStream?, output: OutputStream) {
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input!!.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
        }
    }
}