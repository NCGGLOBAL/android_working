package com.creator.metaceleb

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.creator.metaceleb.adapters.CustomImageSelectAdapter
import com.creator.metaceleb.common.HNApplication
import com.creator.metaceleb.delegator.HNSharedPreference
import com.creator.metaceleb.helpers.Constants
import com.creator.metaceleb.models.Image
import com.creator.metaceleb.util.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by skcrackers on 9/22/17.
 */
class SelectImageMethodActivity : HelperActivity(), View.OnClickListener {
    private var mCameraType = 0 // 카메라 Type
    private var mCurrentPhotoPath // 촬영된 이미지 경로
            : String? = null

    // ============================= Album =============================
    private var images: ArrayList<Image>? = null
    private val album: String? = null
    private val errorDisplay: TextView? = null
    private var imageCount: TextView? = null
    private var gridView: GridView? = null
    private var adapter: CustomImageSelectAdapter? = null
    private var actionBar: ActionBar? = null
    private var actionMode: ActionMode? = null
    private var countSelected = 0
    private var observer: ContentObserver? = null
    private var handler: Handler? = null
    private var thread: Thread? = null
    private var savedImageSize = 0
    private var mToken: String? = "" // 이미지 등록 Token
    private var mImgArr: JSONArray? = null // 이미지 Array
    private var mIsChanged = false
    private var mPageGbn = "2"
    private var mCnt: String? = "0"
    private val mImageCaptureUri: Uri? = null
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA
    )

    // ============================= Album =============================
    private var mProgressDialog: ProgressDialog? = null
    private var mImgDownloadingCnt = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SeongKwon", "#onCreate()")
        try {
            setContentView(R.layout.activity_select_image_method)
            mProgressDialog = ProgressDialog(this@SelectImageMethodActivity) // 처리중

            // 서버에서 내려온 이미지 경로
            val intent = intent
            if (intent.hasExtra("imgArr") && intent.hasExtra("token")) {
                mImgArr = JSONArray(intent.getStringExtra("imgArr"))
                mToken = intent.getStringExtra("token")
            }

            // 1 : 신규페이지에서 진입, 2 : 수정페이지에서 진입
            mPageGbn = "2"
            if (intent.hasExtra("pageGbn") && intent.hasExtra("cnt")) {
                mPageGbn = intent.getStringExtra("pageGbn")!!
                mCnt = intent.getStringExtra("cnt")
                if (mPageGbn == "1") {
                    if (HNApplication.Companion.mImgArrForReg != "" && mCnt != "0") {
                        mImgArr = JSONArray(HNApplication.Companion.mImgArrForReg)
                    } else {
                        HNApplication.Companion.mImgArrForReg = ""
                    }
                } else {
                    HNApplication.Companion.mImgArrForReg = mImgArr.toString()
                }
            }
            Log.d("SeongKwon", "pageGbn = $mPageGbn")
            Log.d("SeongKwon", "mImgArr = $mImgArr")

            // Set a toolbar to  replace to action bar
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)
            imageCount = findViewById<View>(R.id.textview_header_image_count) as TextView
            gridView = findViewById<View>(R.id.grid_view_image_select) as GridView

            // set actionBar
            actionBar = supportActionBar
            if (actionBar != null) {
                actionBar!!.setDisplayHomeAsUpEnabled(true)
                actionBar!!.setDisplayShowHomeEnabled(true)
                actionBar!!.setDisplayShowTitleEnabled(true)
                actionBar!!.setTitle("사진추가")
                val textview_header_right =
                    findViewById<View>(R.id.textview_header_right) as TextView
                textview_header_right.setOnClickListener {
                    val intent = Intent()
                    if (mImgArr != null) {
                        mCnt = mImgArr!!.length().toString()
                        Log.d("SeongKwon", "!!!!!!!!!!!!!!!!!!!!!!!!!pageGbn = $mPageGbn")
                        Log.d("SeongKwon", "!!!!!!!!!!!!!!!!!!!!!!!!!mImgArr = $mImgArr")
                        Log.d("SeongKwon", "!!!!!!!!!!!!!!!!!!!!!!!!!mCnt = $mCnt")
                        if (mIsChanged) {
                            intent.putExtra("isChanged", true)
                        }
                        intent.putExtra("imgArr", mImgArr.toString())
                        intent.putExtra("token", mToken)
                        intent.putExtra("cnt", mCnt)
                        intent.putExtra("pageGbn", mPageGbn)
                    } else {
                        intent.putParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES, images)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
            gridView!!.onItemClickListener =
                OnItemClickListener { parent, view, position, id -> // 수정
                    val intent =
                        Intent(this@SelectImageMethodActivity, ImageEditActivity::class.java)
                    intent.putExtra("token", mToken)
                    intent.putExtra("imgArr", mImgArr.toString())
                    intent.putExtra("pos", position)
                    startActivityForResult(intent, Constants.REQUEST_EDIT_IMAGE)
                }
            val cameraLayout = findViewById<View>(R.id.cameraLayout) as LinearLayout
            val folderLayout = findViewById<View>(R.id.folderLayout) as LinearLayout
            cameraLayout.setOnClickListener(this)
            folderLayout.setOnClickListener(this)
            var isExist = false
            val isGarbageCount = 0
            val file = File("$filesDir/")
            val flist = file.listFiles()
            if (images == null) {
                images = ArrayList()
            }
            savedImageSize = mImgArr!!.length()
            // 이미지 존재여부 확인
            // [{"utype":"0","fileName":"Screenshot_2018-01-10-13-31-08-630_com.wavayo.soho.png","imgUrl":"http:\/\/osaka.wavayo.com\/data\/osaka\/goods\/mallshopping\/other\/201801\/Screenshot_2018-01-10-13-31-08-630_com.wavayo.soho.png","sort":"1"},{"utype":"0","fileName":"Screenshot_2017-11-23-15-11-37-635_com.miui.packageinstaller.png","imgUrl":"http:\/\/osaka.wavayo.com\/data\/osaka\/goods\/mallshopping\/other\/201801\/Screenshot_2017-11-23-15-11-37-635_com.miui.packageinstaller.png","sort":"2"},{"utype":"0","fileName":"Screenshot_2017-09-29-10-44-49-373_com.miui.gallery.png","imgUrl":"http:\/\/osaka.wavayo.com\/data\/osaka\/goods\/mallshopping\/other\/201801\/Screenshot_2017-09-29-10-44-49-373_com.miui.gallery.png","sort":"3"}]
            HNSharedPreference.putSharedPreference(this, "savedImage", "") // 초기화
            if (mImgArr != null) {
                if (mImgArr!!.length() > 0) {
                    savedImageSize = mImgArr!!.length()
                    isExist = false
                    for (idx in 0 until mImgArr!!.length()) {
                        val jObjItem = mImgArr!![idx] as JSONObject
                        val fileName = jObjItem.getString("fileName")
                        val imgUrl = jObjItem.getString("imgUrl")
                        val sort = jObjItem.getInt("sort")
                        val utype = jObjItem.getInt("utype") // 0: 기존이미지, 1: 신규, 2: 수정
                        if (flist.size > 0) {
                            for (i in flist.indices) {
                                val fname = flist[i].name
                                if (fileName == fname) {
                                    isExist = true
                                    var savedImage = HNSharedPreference.getSharedPreference(
                                        this@SelectImageMethodActivity,
                                        "savedImage"
                                    )
                                    savedImage += "$fileName&$sort,"
                                    HNSharedPreference.putSharedPreference(
                                        this@SelectImageMethodActivity,
                                        "savedImage",
                                        savedImage
                                    )
                                }
                            }
                            if (!isExist) {
                                // 이미지 다운로드
                                DownloadImage().execute(imgUrl, fileName, sort, utype)
                            }
                        } else {
                            DownloadImage().execute(imgUrl, fileName, sort, utype)
                        }
                    }
                } else {
                    // 전체삭제
                    BitmapUtil.Companion.deleteImages(this, "$filesDir/")
                }
            }
            imageCount?.text =
                savedImageSize.toString() + "/" + HNApplication.LIMIT_IMAGE_COUNT
            loadImages()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.cameraLayout -> {
                    if (images!!.size >= HNApplication.Companion.LIMIT_IMAGE_COUNT) {
                        return
                    }
                    mCameraType = 3
                    requestPermission(Constants.REQUEST_SELECT_IMAGE_CAMERA)
                }
                R.id.folderLayout -> {
                    if (images!!.size >= HNApplication.Companion.LIMIT_IMAGE_COUNT) {
                        return
                    }
                    mCameraType = 4
                    requestPermission(Constants.REQUEST_SELECT_IMAGE_ALBUM)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        if (mIsChanged) {
//            mIsChanged = false;
            return
        }
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Constants.PERMISSION_GRANTED -> {
                        Log.d("SeongKwon", "PERMISSION_GRANTED")
                        loadImages()
                    }
                    Constants.FETCH_STARTED -> {
                        Log.d("SeongKwon", "FETCH_STARTED")
                        //                        progressBar.setVisibility(View.VISIBLE);
                        gridView!!.visibility = View.INVISIBLE
                    }
                    Constants.FETCH_COMPLETED -> {

                        /*
                        If adapter is null, this implies that the loaded images will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */Log.d("SeongKwon", "FETCH_COMPLETED : " + images!!.size)
                        if (adapter == null) {
                            adapter = CustomImageSelectAdapter(applicationContext, images, false)
                            gridView!!.adapter = adapter
                            gridView!!.visibility = View.VISIBLE
                            orientationBasedUI(resources.configuration.orientation)
                            Log.d(
                                "SeongKwon",
                                "FETCH_COMPLETED : ====================================1"
                            )
                        } else {
                            adapter!!.notifyDataSetChanged()
                            /*
                            Some selected images may have been deleted
                            hence update action mode title
                             */Log.d(
                                "SeongKwon",
                                "FETCH_COMPLETED : ====================================2"
                            )
                            if (actionMode != null) {
                                countSelected = msg.arg1
                                //                                actionMode.setTitle("상품등록");
                            }
                        }
                        imageCount!!.text =
                            savedImageSize.toString() + "/" + HNApplication.Companion.LIMIT_IMAGE_COUNT
                    }
                    Constants.ERROR -> {
                        Log.d("SeongKwon", "ERROR")
                    }
                    else -> {
                        super.handleMessage(msg)
                    }
                }
            }
        }
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                loadImages()
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            false,
            observer!!
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkPermission()
        }
    }

    override fun onStop() {
        super.onStop()
        deselectAll()
        stopThread()
        if (observer != null) {
            contentResolver.unregisterContentObserver(observer!!)
            observer = null
        }
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
            handler = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 다이얼로그 닫기
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
            mProgressDialog = null
        }
        if (actionBar != null) {
            actionBar!!.setHomeAsUpIndicator(null)
        }
        images = null
        if (adapter != null) {
            adapter!!.releaseResources()
        }
        gridView!!.onItemClickListener = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationBasedUI(newConfig.orientation)
    }

    private fun orientationBasedUI(orientation: Int) {
        val windowManager = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        if (adapter != null) {
            val size =
                if (orientation == Configuration.ORIENTATION_PORTRAIT) metrics.widthPixels / 3 else metrics.widthPixels / 5
            adapter!!.setLayoutParams(size)
        }
        gridView!!.numColumns = if (orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onBackPressed() {
        if (mIsChanged) {
            mCnt = mImgArr!!.length().toString()
            val intent = Intent()
            intent.putExtra("imgArr", mImgArr.toString())
            intent.putExtra("token", mToken)
            intent.putExtra("isChanged", true)
            intent.putExtra("cnt", mCnt)
            intent.putExtra("pageGbn", mPageGbn)
            setResult(RESULT_OK, intent)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private val callback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val menuInflater = mode.menuInflater
            menuInflater.inflate(R.menu.menu_contextual_action_bar, menu)
            actionMode = mode
            countSelected = 0
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val i = item.itemId
            if (i == R.id.menu_item_add_image) {
                sendIntent()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (countSelected > 0) {
                deselectAll()
            }
            actionMode = null
        }
    }

    private fun toggleSelection(position: Int) {
        if (!images!![position].isSelected && countSelected >= Constants.limit) {
            Toast.makeText(
                applicationContext,
                String.format(getString(R.string.limit_exceeded), Constants.limit),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        images!![position].isSelected = !images!![position].isSelected
        if (images!![position].isSelected) {
            countSelected++
            images!![position].sequence = countSelected
            //            Log.d("===============", images.get(position).sequence + "//POSITION");
        } else {
            countSelected--

            // 재정렬
            val tmpSequence = images!![position].sequence
            images!![position].sequence = -1
            //            Log.d("delete===============", "toggleSelection = " + position + "// sequence = " + images.get(position).sequence);
            for (idx in images!!.indices) {
                if (images!![idx].isSelected) {
                    if (images!![idx].sequence > tmpSequence) {
                        images!![idx].sequence = images!![idx].sequence - 1
                    }
                }
                //                Log.d("update===============", "idx = " + idx + " // sequence = " + images.get(idx).sequence);
            }
        }
        adapter?.notifyDataSetChanged()
    }

    private fun deselectAll() {
        var i = 0
        val l = images!!.size
        while (i < l) {
            images!![i].isSelected = false
            i++
        }
        countSelected = 0
        adapter?.notifyDataSetChanged()
    }// ACT1011 CALLBACK

    // utype =  0: 기존이미지, 1: 신규, 2: 수정
    // 마지막에 추가
    // Image 저장
    private val selected: ArrayList<Image>
        private get() {
            var path = ""
            val selectedImages = ArrayList<Image>(countSelected)
            selectedImages.clear()
            for (i in 0 until countSelected) {
                selectedImages.add(i, images!![0])
            }
            for (i in images!!.indices) {
                if (images!![i].isSelected) {
                    path = """
                         $path${images!![i].sequence - 1}//${images!![i].path}
                         
                         
                         """.trimIndent()
                    Log.e("SeongKwon", "selected path = " + images!![i].path)
                    Log.e(
                        "SeongKwon",
                        (images!![i].sequence - 1).toString() + "//" + selectedImages.size + "//" + countSelected
                    )
                    selectedImages[images!![i].sequence - 1] = images!![i]

                    // Image 저장
                    if (BitmapUtil.Companion.saveImage(
                            this,
                            images!![i].path,
                            images!![i].name, images!![i].sequence.toString()
                        )
                    ) {
                        try {
                            // ACT1011 CALLBACK
                            val jObjItem = JSONObject()
                            jObjItem.put("imgUrl", "")
                            jObjItem.put("fileName", images!![i].name)
                            jObjItem.put("utype", 1) // utype =  0: 기존이미지, 1: 신규, 2: 수정
                            jObjItem.put("sort", images!![i].sequence) // 마지막에 추가
                            mImgArr!!.put(jObjItem)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            Log.d("SeongKwon", "countSelected = $countSelected")
            Log.d("SeongKwon", "images.size() = " + images!!.size)
            Log.d("SeongKwon", "selectedImages.size() = " + selectedImages.size)
            return selectedImages
        }

    private fun sendIntent() {
        saveImagesAsyncTask().execute()
    }

    private fun loadImages() {
        startThread(ImageLoaderRunnable())
    }

    private inner class ImageLoaderRunnable : Runnable {
        override fun run() {
//            android.os.Process.setThreadPriority(java.lang.Process.THREAD_PRIORITY_BACKGROUND);
            /*
            If the adapter is null, this is first time this activity's view is
            being shown, hence send FETCH_STARTED message to show progress bar
            while images are loaded from phone
             */
            if (adapter == null) {
                sendMessage(Constants.FETCH_STARTED)
            }

            // ================== 2018.01.08 아이폰과 동일하게 적용하기 위해 변경 ==================
            try {
                val isExist = false
                val file = File("$filesDir/")
                val flist = file.listFiles()
                Log.d("SeongKwon", "*************************************************")
                Log.d("SeongKwon", filesDir.toString() + "/imgcnt = " + flist.size)
                Log.d("SeongKwon", "*************************************************")
                if (images == null) {
                    images = ArrayList()
                }
                images!!.clear()
                savedImageSize = mImgArr!!.length()
                for (i in flist.indices) {
                    val fname = flist[i].name
                    val id: Long = -1
                    val path = file.absolutePath + "/" + fname
                    val isSelected = true

                    // getSequence
                    var sort = -1
                    val savedImage = HNSharedPreference.getSharedPreference(
                        this@SelectImageMethodActivity,
                        "savedImage"
                    )
                    val savedImageArray = savedImage!!.split(",").toTypedArray()
                    var fName = ""
                    val tempArray = ArrayList<Image>()
                    for (j in savedImageArray.indices) {
                        fName = savedImageArray[j].split("&").toTypedArray()[0]
                        if (fName == fname) {
                            sort = savedImageArray[j].split("&").toTypedArray()[1].toInt()
                        }
                    }
                    Log.d("SeongKwon", "=========================")
                    Log.d("SeongKwon", "id = $id")
                    Log.d("SeongKwon", "name = $fname")
                    Log.d("SeongKwon", "path = $path")
                    Log.d("SeongKwon", "isSelected = $isSelected")
                    Log.d("SeongKwon", "=========================")
                    if (file.exists()) {
                        images!!.add(
                            Image(
                                id,
                                fname,
                                path,
                                isSelected,
                                sort
                            )
                        )
                    }
                }
                val savedImage = HNSharedPreference.getSharedPreference(
                    this@SelectImageMethodActivity,
                    "savedImage"
                )
                val savedImageArray = savedImage!!.split(",").toTypedArray()

                // Data변경 - Data는 SharedPreference에 저장된 순서대로 보여진다.
                var fName = ""
                val tempArray = ArrayList<Image>()
                for (j in savedImageArray.indices) {
                    fName = savedImageArray[j].split("&").toTypedArray()[0]
                    for (k in images!!.indices) {
                        if (fName == images!![k].name) {
                            tempArray.add(images!![k])
                            break
                        }
                    }
                }
                images!!.clear()
                images!!.addAll(tempArray)
            } catch (e: Exception) {
                e.printStackTrace()
                sendMessage(Constants.ERROR)
            }
            sendMessage(Constants.FETCH_COMPLETED, savedImageSize)
        }
    }

    private fun startThread(runnable: Runnable) {
        stopThread()
        thread = Thread(runnable)
        thread!!.start()
    }

    private fun stopThread() {
        if (thread == null || !thread!!.isAlive) {
            return
        }
        thread!!.interrupt()
        try {
            thread!!.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun sendMessage(what: Int, arg1: Int = 0) {
        if (handler == null) {
            return
        }
        val message = handler!!.obtainMessage()
        message.what = what
        message.arg1 = arg1
        message.sendToTarget()
    }

    override fun permissionGranted() {
        sendMessage(Constants.PERMISSION_GRANTED)
    }

    override fun hideViews() {
//        progressBar.setVisibility(View.INVISIBLE);
        gridView!!.visibility = View.INVISIBLE
    }

    /**
     * 권한체크 요청
     *
     * @param requestPermissionId
     */
    private fun requestPermission(requestPermissionId: Int) {
        LogUtil.d("$requestPermissionId :: permission has NOT been granted. Requesting permission.")

        if (requestPermissionId == Constants.REQUEST_CAMERA || requestPermissionId == Constants.REQUEST_SELECT_IMAGE_CAMERA) {
            // 카메라 선택 - 카메라 권한만 필요
            if (mCameraType == 3) {
                TedPermission.create()
                    .setPermissionListener(object : PermissionListener {
                        override fun onPermissionGranted() {
                            dispatchTakePictureIntent()
                        }
                        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                            Toast.makeText(this@SelectImageMethodActivity, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    })
                    .setDeniedMessage("권한을 허용해주세요.")
                    .setPermissions(Manifest.permission.CAMERA)
                    .check()
            }
        } else if (requestPermissionId == Constants.REQUEST_WRITE_EXTERNAL_STORAGE || requestPermissionId == Constants.REQUEST_SELECT_IMAGE_ALBUM) {
            // 앨범 선택 - Photo Picker 사용 (권한 불필요)
            if (mCameraType == 4) {
                galleryAddPic()
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CAMERA || requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA) {

            // Check if the only required permission has been granted
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mCameraType == 3) {
                    dispatchTakePictureIntent()
                }
                LogUtil.i("SEND_SMS permission has now been granted. Showing preview.")
            } else {
                LogUtil.i("SEND_SMS permission was NOT granted.")
            }
        } else if (requestCode == Constants.REQUEST_WRITE_EXTERNAL_STORAGE || requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM) {
            LogUtil.i("Received response for getting Location Info permission request.")
            if (mCameraType == 4) {
                galleryAddPic()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // 사진촬영
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoUri = FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".fileprovider",
                    photoFile
                )
                //                this.grantUriPermission("com.android.camera", photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                //                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, Constants.REQUEST_SELECT_IMAGE_CAMERA)
            }
        }
    }

    // 사진 앨범선택
    private fun galleryAddPic() {
        val intent = Intent(applicationContext, AlbumSelectActivity::class.java)
        startActivityForResult(intent, Constants.REQUEST_SELECT_IMAGE_ALBUM)
    }

    // 사진저장
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //        deleteDir(storageDir.getPath());

        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        //        File storageDir = getFilesDir();
        val imageFileName = timeStamp + "_"
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        Log.d("mCurrentPhotoPath", mCurrentPhotoPath ?: "")
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("SeongKwon", "============================================")
        Log.d("SeongKwon", "requestCode = $requestCode")
        Log.d("SeongKwon", "resultCode = $resultCode")
        Log.d("SeongKwon", "mCurrentPhotoPath = $mCurrentPhotoPath")
        Log.d("SeongKwon", "data = $data")
        Log.d("SeongKwon", "============================================")
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            val extras = data.extras
            if (extras != null) {
                for (_key in extras.keySet()) {
                    Log.d("SeongKwon", "key=" + _key + " : " + extras[_key])
                }
            }
        }
        if (requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM && resultCode == RESULT_OK && data != null) {
            Log.d("SeongKwon", "////" + images!!.size)
            val addimages: ArrayList<Image>

            // Photo Picker 결과 처리 (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && data.data != null) {
                addimages = ArrayList()
                val uris = ArrayList<Uri>()

                // ClipData에서 여러 이미지 가져오기 (다중 선택 시)
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        uris.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else if (data.data != null) {
                    uris.add(data.data!!)
                }

                // Uri를 Image 객체로 변환
                for ((index, uri) in uris.withIndex()) {
                    val path = RealPathUtil.getRealPath(this, uri) ?: uri.toString()
                    val fileName = File(path).name
                    addimages.add(Image(
                        index.toLong(),
                        fileName,
                        path,
                        true,
                        images!!.size + index + 1
                    ))
                }
            } else {
                // AlbumSelectActivity 결과 처리 (Android 12 이하)
                addimages = data.getParcelableArrayListExtra<Image>("images") ?: ArrayList()
            }

            Log.d("SeongKwon", "////" + addimages.size)
            if (addimages.size > 0) {
                mIsChanged = true
            }
            for (i in addimages.indices) {
                val image = addimages[i]
                image.sequence = images!!.size + 1
                image.isSelected = true
                images!!.add(image)
                Log.e("SeongKwon", "//// sequence = " + image.sequence)
            }
            countSelected = images!!.size
            Log.e("SeongKwon", "//// countSelected = $countSelected")

            // 이미지 저장
            saveImagesAsyncTask().execute(addimages)
        } else if (requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "카메라 촬영에 실패했습니다.", Toast.LENGTH_LONG).show()
                return
            }
            //            Uri currImageURI = data.getData();
//            if(data.getData() == null) {
//                currImageURI = mImageCaptureUri;
//            }
//            currImageURI = mImageCaptureUri;
//            mCurrentPhotoPath = getRealPathFromURI(currImageURI);
            mIsChanged = true
            val addImage = Image(
                0,
                mCurrentPhotoPath!!.substring(mCurrentPhotoPath!!.lastIndexOf("/") + 1),
                mCurrentPhotoPath,
                true,
                images!!.size + 1
            )
            val addimages = ArrayList<Image>()
            addimages.add(addImage)
            images!!.add(addImage)
            countSelected = images!!.size

            // 이미지 저장
            Log.e("SeongKwon", "//// saveImagesAsyncTask")
            saveImagesAsyncTask().execute(addimages)
        } else if (requestCode == Constants.REQUEST_EDIT_IMAGE) {
            if (data == null) return
            //            if (resultCode == RESULT_OK) {
            if (data.hasExtra("isChanged")) {
                mIsChanged = data.getBooleanExtra("isChanged", false)
                Log.d(
                    "SeongKwon",
                    "Constants.REQUEST_EDIT_IMAGE ******************************************* isChanged = " + data.getBooleanExtra(
                        "isChanged",
                        false
                    )
                )
            }
            try {
                if (data.hasExtra("imgArr")) {
                    mImgArr = JSONArray(data.getStringExtra("imgArr"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mToken = data.getStringExtra("token")
            if (mIsChanged) {
                val savedImage = HNSharedPreference.getSharedPreference(
                    this@SelectImageMethodActivity,
                    "savedImage"
                )
                val savedImageArray = savedImage!!.split(",").toTypedArray()

                // Data변경
                var fName = ""
                val tempArray = ArrayList<Image>()
                for (i in savedImageArray.indices) {
                    fName = savedImageArray[i].split("&").toTypedArray()[0]
                    for (k in images!!.indices) {
                        if (fName == images!![k].name) {
                            tempArray.add(images!![k])
                            break
                        }
                    }
                }
                images!!.clear()
                images!!.addAll(tempArray)

                // 파일내용 변경
                adapter!!.notifyDataSetChanged()
                Log.d(
                    "SeongKwon",
                    "Constants.REQUEST_EDIT_IMAGE ******************************************* adapter.notifyDataSetChanged();"
                )
                imageCount?.text = images?.size.toString() + "/" + HNApplication.LIMIT_IMAGE_COUNT
            }
            //            }
        }
    }

    inner class saveImagesAsyncTask : AsyncTask<Any?, Void?, ArrayList<Image>>() {
        var result: String? = null
        override fun onPreExecute() {
            super.onPreExecute()
            mProgressDialog!!.setTitle("알림")
            mProgressDialog!!.setMessage("처리중입니다.\n잠시만 기다려 주세요.")
            if (!mProgressDialog!!.isShowing) mProgressDialog!!.show()
            Log.e("SeongKwon", "//// saveImagesAsyncTask onPreExecute")
        }

        override fun doInBackground(vararg params: Any?): ArrayList<Image> {
//            ArrayList<Image> selectedImages = (ArrayList<Image>) params[0];
            return selected
        }
//
//        protected override fun onProgressUpdate(vararg values: Void) {
//            super.onProgressUpdate(*values)
//        }

        override fun onPostExecute(img: ArrayList<Image>) {
//            images = img;

            // 화면 그리기
            Log.e("SeongKwon", "//// saveImagesAsyncTask onPostExecute")
            //            sendMessage(Constants.FETCH_COMPLETED, countSelected);
            mProgressDialog!!.dismiss()
            savedImageSize = images!!.size
            imageCount?.text =
                savedImageSize.toString() + "/" + HNApplication.Companion.LIMIT_IMAGE_COUNT

            loadImages()
            // Close progressdialog
            mProgressDialog!!.dismiss()
        }
    }

    // DownloadImage AsyncTask
    private inner class DownloadImage : AsyncTask<Any?, Void?, Bitmap?>() {
        var imgUrl = ""
        var fileName = ""
        var sort = -1
        var utype = 0
        override fun onPreExecute() {
            super.onPreExecute()
            mImgDownloadingCnt++

            // Create a progressdialog
            mProgressDialog!!.setTitle("알림")
            mProgressDialog!!.setMessage("처리중입니다.\n잠시만 기다려 주세요.")
            mProgressDialog!!.isIndeterminate = false
            if (!mProgressDialog!!.isShowing) mProgressDialog!!.show()
        }

        override fun doInBackground(vararg params: Any?): Bitmap? {
            imgUrl = params[0] as String
            fileName = params[1] as String
            sort = params[2] as Int
            utype = params[3] as Int
            var bitmap: Bitmap? = null
            try {
                // Download Image from URL
                val input = URL(imgUrl).openStream()
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            var outStream: OutputStream? = null
            val file = File("$filesDir/$fileName")
            try {
                outStream = FileOutputStream(file)
                bitmap?.compress(CompressFormat.JPEG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val id: Long = -1
            val name = fileName
            val path = file.absolutePath
            val isSelected = true
            Log.d("SeongKwon", "=========================")
            Log.d("SeongKwon", "id = $id")
            Log.d("SeongKwon", "name = $name")
            Log.d("SeongKwon", "path = $path")
            Log.d("SeongKwon", "isSelected = $isSelected")
            Log.d("SeongKwon", "=========================")
            images!!.add(Image(id, name, path, isSelected, sort))

            // 저장된 이미지 리스트를 preference에 저장
            var savedImage =
                HNSharedPreference.getSharedPreference(this@SelectImageMethodActivity, "savedImage")
            savedImage += "$fileName&$sort,"
            HNSharedPreference.putSharedPreference(
                this@SelectImageMethodActivity,
                "savedImage",
                savedImage
            )

            // 화면 그리기
            sendMessage(Constants.FETCH_COMPLETED, savedImageSize)

            // Close progressdialog
            mImgDownloadingCnt--
            if (mImgDownloadingCnt == 0) {
                mProgressDialog!!.dismiss()
            }
        }
    }

    fun getRealPathFromURI(contentUri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(contentUri, proj, null, null, null)
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }
}