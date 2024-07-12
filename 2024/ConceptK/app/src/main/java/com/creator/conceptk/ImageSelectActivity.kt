package com.creator.conceptk

import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.creator.conceptk.adapters.CustomImageSelectAdapter
import com.creator.conceptk.common.HNApplication
import com.creator.conceptk.helpers.Constants
import com.creator.conceptk.models.Image
import java.io.File

/**
 * Created by Darshan on 4/18/2015.
 */
class ImageSelectActivity : HelperActivity() {
    private var images: ArrayList<Image>? = null
    private var album: String? = null
    private var errorDisplay: TextView? = null
    private var imageCount: TextView? = null
    private var progressBar: ProgressBar? = null
    private var gridView: GridView? = null
    private var adapter: CustomImageSelectAdapter? = null
    private var actionBar: ActionBar? = null
    private var actionMode: ActionMode? = null
    private var countSelected = 0
    private var observer: ContentObserver? = null
    private var handler: Handler? = null
    private var thread: Thread? = null
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_select)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val intent = intent
        if (intent == null) {
            finish()
        }
        album = intent!!.getStringExtra(Constants.INTENT_EXTRA_ALBUM)
        actionBar = supportActionBar
        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setDisplayShowHomeEnabled(true)
            actionBar!!.setDisplayShowTitleEnabled(true)
            actionBar!!.setTitle(album)
            val textview_header_right = findViewById<View>(R.id.textview_header_right) as TextView
            textview_header_right.setOnClickListener { sendIntent() }
        }
        imageCount = findViewById<View>(R.id.textview_header_image_count) as TextView
        errorDisplay = findViewById<View>(R.id.text_view_error) as TextView
        errorDisplay!!.visibility = View.INVISIBLE
        progressBar = findViewById<View>(R.id.progress_bar_image_select) as ProgressBar
        gridView = findViewById<View>(R.id.grid_view_image_select) as GridView
        gridView!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id -> //                if (actionMode == null) {
//                    actionMode = ImageSelectActivity.this.startActionMode(callback);
//                }
                toggleSelection(position)
                //                if (countSelected == 0) {
//                    actionMode.finish();
//                }
                imageCount!!.text =
                    countSelected.toString() + "/" + HNApplication.Companion.LIMIT_IMAGE_COUNT
            }
    }

    override fun onStart() {
        super.onStart()
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Constants.PERMISSION_GRANTED -> {
                        loadImages()
                    }
                    Constants.FETCH_STARTED -> {
                        progressBar!!.visibility = View.VISIBLE
                        gridView!!.visibility = View.INVISIBLE
                    }
                    Constants.FETCH_COMPLETED -> {

                        /*
                        If adapter is null, this implies that the loaded images will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */if (adapter == null) {
                            adapter = CustomImageSelectAdapter(applicationContext, images)
                            gridView!!.adapter = adapter
                            progressBar!!.visibility = View.INVISIBLE
                            gridView!!.visibility = View.VISIBLE
                            orientationBasedUI(resources.configuration.orientation)
                        } else {
                            adapter!!.notifyDataSetChanged()
                            /*
                            Some selected images may have been deleted
                            hence update action mode title
                             */if (actionMode != null) {
                                countSelected = msg.arg1
                                actionMode!!.title =
                                    countSelected.toString() + " " + getString(R.string.selected)
                            }
                        }
                    }
                    Constants.ERROR -> {
                        progressBar!!.visibility = View.INVISIBLE
                        errorDisplay!!.visibility = View.VISIBLE
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
        checkPermission()
    }

    override fun onStop() {
        super.onStop()
        stopThread()
        contentResolver.unregisterContentObserver(observer!!)
        observer = null
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
            handler = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
//            Toast.makeText(
//                    getApplicationContext(),
//                    String.format(getString(R.string.limit_exceeded), Constants.limit),
//                    Toast.LENGTH_SHORT)
//                    .show();
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
        adapter!!.notifyDataSetChanged()
    }

    private fun deselectAll() {
        var i = 0
        val l = images!!.size
        while (i < l) {
            images!![i].isSelected = false
            i++
        }
        countSelected = 0
        adapter!!.notifyDataSetChanged()
    }

    private val selected: ArrayList<Image>
        private get() {
            var path = ""
            val selectedImages = ArrayList<Image>(countSelected)
            selectedImages.clear()
            for (i in 0 until countSelected) {
                selectedImages.add(i, images!![0])
            }
            Log.d("SeongKwon", selectedImages.size.toString() + "//" + countSelected)
            for (i in images!!.indices) {
                if (images!![i].isSelected) {
                    path = """
                    $path${images!![i].sequence - 1}//${images!![i].path}
                    
                    
                    """.trimIndent()
                    Log.d("SeongKwon", "selected path = " + images!![i].path)
                    Log.d(
                        "SeongKwon",
                        (images!![i].sequence - 1).toString() + "//" + selectedImages.size + "//" + countSelected
                    )
                    selectedImages[images!![i].sequence - 1] = images!![i]
                }
            }
            Log.d("SeongKwon", "countSelected = $countSelected")
            Log.d("SeongKwon", "images.size() = " + images!!.size)
            Log.d("SeongKwon", "selectedImages.size() = " + selectedImages.size)
            return selectedImages
        }

    private fun sendIntent() {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES, selected)
        setResult(RESULT_OK, intent)
        finish()
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
            var file: File
            val selectedImages = HashSet<Long>()
            if (images != null) {
                var image: Image
                var i = 0
                val l = images!!.size
                while (i < l) {
                    image = images!![i]
                    file = File(image.path)
                    if (file.exists() && image.isSelected) {
                        selectedImages.add(image.id)
                    }
                    i++
                }
            }
            var cursor: Cursor? = null
            cursor = if (album == "전체 보기") {
                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED
                )
            } else {
                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " =?",
                    arrayOf(album),
                    MediaStore.Images.Media.DATE_ADDED
                )
            }
            if (cursor == null) {
                sendMessage(Constants.ERROR)
                return
            }

            /*
            In case this runnable is executed to onChange calling loadImages,
            using countSelected variable can result in a race condition. To avoid that,
            tempCountSelected keeps track of number of selected images. On handling
            FETCH_COMPLETED message, countSelected is assigned value of tempCountSelected.
             */
            var tempCountSelected = 0
            val temp = ArrayList<Image>(cursor.count)
            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return
                    }
                    val id = cursor.getLong(cursor.getColumnIndex(projection[0]))
                    val name = cursor.getString(cursor.getColumnIndex(projection[1]))
                    val path = cursor.getString(cursor.getColumnIndex(projection[2]))
                    val isSelected = selectedImages.contains(id)
                    if (isSelected) {
                        tempCountSelected++
                    }
                    file = File(path)
                    if (file.exists()) {
//                        temp.add(new Image(id, name, path, isSelected));
                        temp.add(Image(id, name, path, isSelected, -1))
                    }
                } while (cursor.moveToPrevious())
            }
            cursor.close()
            if (images == null) {
                images = ArrayList()
            }
            images!!.clear()
            images!!.addAll(temp)
            sendMessage(Constants.FETCH_COMPLETED, tempCountSelected)
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
        progressBar!!.visibility = View.INVISIBLE
        gridView!!.visibility = View.INVISIBLE
    }
}