package com.creator.starrymall

import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.creator.starrymall.delegator.HNSharedPreference
import com.creator.starrymall.helpers.Constants
import com.creator.starrymall.models.Image
import com.creator.starrymall.util.BitmapUtil
import org.json.JSONArray
import sun.bob.dndgridview.DNDAdapter
import sun.bob.dndgridview.DNDGridView
import sun.bob.dndgridview.DNDViewHolder
import java.io.File

/**
 * Created by skcrackers on 12/18/17.
 */
class ImageEditChangeOrder : HelperActivity() {
    // ============================= Album =============================
    private var images: ArrayList<Image?>? = null
    private val album: String? = null
    private val errorDisplay: TextView? = null
    private val imageCount: TextView? = null

    //    private CustomImageSelectAdapter adapter;
    private var gridView: DNDGridView? = null
    private var adapter: DNDAdapter? = null
    private var actionBar: ActionBar? = null
    private val actionMode: ActionMode? = null
    private var countSelected = 0
    private var observer: ContentObserver? = null
    private var handler: Handler? = null
    private var thread: Thread? = null
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA
    )
    private var mToken: String? = "" // 이미지 등록 Token
    private var mImgArr: JSONArray? = null // 이미지 등록 Token
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_image_edit_change_order)
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)

            // 서버에서 내려온 이미지 경로
            val intent = intent
            if (intent.hasExtra("imgArr") && intent.hasExtra("token")) {
                Log.e(
                    "SeongKwon",
                    "=============================" + intent.getStringExtra("imgArr")
                )
                Log.e("SeongKwon", "=============================" + intent.getStringExtra("token"))
                mImgArr = JSONArray(intent.getStringExtra("imgArr"))
                mToken = intent.getStringExtra("token")
            }
            actionBar = supportActionBar
            if (actionBar != null) {
                actionBar!!.setDisplayHomeAsUpEnabled(true)
                actionBar!!.setDisplayShowHomeEnabled(true)
                actionBar!!.setDisplayShowTitleEnabled(true)
                actionBar!!.title = resources.getString(R.string.label_picture_change)
            }
            val textview_header_right = findViewById<View>(R.id.textview_header_right) as TextView
            textview_header_right.setOnClickListener { // 완료
                val intent = Intent()
                if (mImgArr != null) {
                    intent.putExtra("imgArr", mImgArr.toString())
                    intent.putExtra("token", mToken)
                } else {
                    intent.putParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES, images)
                }
                // 변경내역 있음
                if (adapter!!.changedImageList != "") {
                    HNSharedPreference.putSharedPreference(
                        this@ImageEditChangeOrder,
                        "savedImage",
                        adapter!!.changedImageList
                    )
                    intent.putExtra("isChanged", true)
                } else {
                    intent.putExtra("isChanged", false)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
            val imageCount = findViewById<View>(R.id.textview_header_image_count) as TextView
            imageCount.visibility = View.GONE
            gridView = findViewById<View>(R.id.gridview) as DNDGridView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Constants.PERMISSION_GRANTED -> {
                        Log.d("SeongKwon", "Constants.PERMISSION_GRANTED")
                        loadImages()
                    }
                    Constants.FETCH_STARTED -> {
                        Log.d("SeongKwon", "Constants.FETCH_STARTED")
                        gridView!!.visibility = View.INVISIBLE
                    }
                    Constants.FETCH_COMPLETED -> {
                        Log.d("SeongKwon", "Constants.FETCH_COMPLETED")
                        /*
                        If adapter is null, this implies that the loaded images will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */if (adapter == null) {
                            adapter = object : DNDAdapter(
                                applicationContext,
                                R.layout.grid_view_item_image_select
                            ) {
                                override fun getView(
                                    position: Int,
                                    convertView: View?,
                                    parent: ViewGroup
                                ): View {
                                    var convertView = convertView
                                    try {
                                        Log.d("SeongKwon", "DNDAdapter - getView")
                                        val viewHolder: DNDViewHolder
                                        if (convertView == null) {
                                            convertView = View.inflate(
                                                context,
                                                R.layout.grid_view_item_image_select,
                                                null
                                            )
                                            viewHolder = DNDViewHolder(position)
                                            viewHolder.imageView =
                                                convertView.findViewById<View>(R.id.image_view_image_select) as ImageView
                                            viewHolder.textView =
                                                convertView.findViewById<View>(R.id.image_view_image_sequence) as TextView
                                            viewHolder.view =
                                                convertView.findViewById(R.id.view_alpha)
                                            convertView.tag = viewHolder
                                        } else {
                                            viewHolder = convertView.tag as DNDViewHolder
                                        }
                                        viewHolder.imageView.layoutParams.width = size
                                        viewHolder.imageView.layoutParams.height = size
                                        viewHolder.view.layoutParams.width = size
                                        viewHolder.view.layoutParams.height = size
                                        viewHolder.textView.visibility = View.VISIBLE
                                        viewHolder.textView.text = (position + 1).toString() + ""
                                        viewHolder.view.alpha = 0.0f
                                        Glide.with(this@ImageEditChangeOrder)
                                            .load(images!![position]!!.path) //                                                .transform(new BitmapUtil(ImageEditChangeOrder.this, BitmapUtil.GetExifOrientation(images.get(position).path)))
                                            .transform(BitmapUtil(this@ImageEditChangeOrder, 0f))
                                            .placeholder(R.drawable.image_placeholder)
                                            .into(viewHolder.imageView)
                                        setUpDragNDrop(position, convertView)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    return convertView!!
                                }
                            }
                            adapter?.setCustomArray(images)
                            gridView!!.adapter = adapter
                            gridView!!.visibility = View.VISIBLE
                            orientationBasedUI(resources.configuration.orientation)
                        } else {
                            adapter!!.notifyDataSetChanged()
                            /*
                            Some selected images may have been deleted
                            hence update action mode title
                             */if (actionMode != null) {
                                countSelected = msg.arg1
                                actionMode.title =
                                    countSelected.toString() + " " + getString(R.string.selected)
                            }
                        }
                    }
                    Constants.ERROR -> {
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
            var tempCountSelected = 0
            val temp = ArrayList<Image?>()
            val file = File("$filesDir/")
            val flist = file.listFiles()
            val savedImage =
                HNSharedPreference.getSharedPreference(this@ImageEditChangeOrder, "savedImage")
            Log.d("SeongKwon", "savedImage = $savedImage")
            val savedImageArray = savedImage!!.split(",").toTypedArray()
            tempCountSelected = savedImageArray.size
            for (i in 0 until tempCountSelected) {
                temp.add(i, null)
            }
            Log.d("SeongKwon", "*************************************************")
            for (i in 0 until tempCountSelected) {
                val imageItem = savedImageArray[i]
                Log.d("SeongKwon", "imageItem = $imageItem")
                val delemeter = imageItem.indexOf("&")
                if (imageItem.isNotEmpty()) {
                    val id: Long = -1
                    val name = imageItem.substring(0, delemeter)
                    val path = file.absolutePath + "/" + name
                    val isSelected = false
                    val sequence = imageItem.substring(delemeter + 1, imageItem.length).toInt()
                    Log.d("SeongKwon", "=========================")
                    Log.d("SeongKwon", "id = $id")
                    Log.d("SeongKwon", "name = $name")
                    Log.d("SeongKwon", "path = $path")
                    Log.d("SeongKwon", "isSelected = $isSelected")
                    Log.d("SeongKwon", "sequencesequence = $sequence")
                    Log.d("SeongKwon", "=========================")
                    if (file.exists()) {
                        temp[sequence - 1] = Image(id, name, path, isSelected, sequence)
                    }
                }
            }
            Log.d("SeongKwon", "*************************************************")
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

    companion object {
        // ============================= Album =============================
        private val TAG = ImageEditChangeOrder::class.java.name
    }
}