package com.creator.ourliving

import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Process
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.creator.ourliving.adapters.CustomAlbumSelectAdapter
import com.creator.ourliving.helpers.Constants
import com.creator.ourliving.models.Album
import java.io.File

/**
 * Created by Darshan on 4/14/2015.
 */
class AlbumSelectActivity : HelperActivity() {
    private var albums: ArrayList<Album>? = null
    private var errorDisplay: TextView? = null
    private var progressBar: ProgressBar? = null
    private var listView: ListView? = null
    private var adapter: CustomAlbumSelectAdapter? = null
    private var actionBar: ActionBar? = null
    private var observer: ContentObserver? = null
    private var handler: Handler? = null
    private var thread: Thread? = null
    private val albumCount = HashMap<String?, Long>()
    private val projection = arrayOf(
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_select)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setDisplayShowHomeEnabled(true)
            actionBar!!.setDisplayShowTitleEnabled(true)
            actionBar!!.title = "폴더선택"
            val textview_header_right = findViewById<View>(R.id.textview_header_right) as TextView
            textview_header_right.visibility = View.GONE
        }
        val intent = intent
        if (intent == null) {
            finish()
        }
        Constants.limit =
            intent!!.getIntExtra(Constants.INTENT_EXTRA_LIMIT, Constants.DEFAULT_LIMIT)
        errorDisplay = findViewById<View>(R.id.text_view_error) as TextView
        errorDisplay!!.visibility = View.INVISIBLE
        progressBar = findViewById<View>(R.id.progress_bar_album_select) as ProgressBar
        listView = findViewById<View>(R.id.list_view_album_select) as ListView
        listView!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val intent = Intent(applicationContext, ImageSelectActivity::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_ALBUM, albums!![position].name)
            startActivityForResult(intent, Constants.REQUEST_CODE)
        }

//        gridView = (GridView) findViewById(R.id.grid_view_album_select);
//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(getApplicationContext(), ImageSelectActivity.class);
//                intent.putExtra(Constants.INTENT_EXTRA_ALBUM, albums.get(position).name);
//                startActivityForResult(intent, Constants.REQUEST_CODE);
//            }
//        });
    }

    override fun onStart() {
        super.onStart()
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Constants.PERMISSION_GRANTED -> {
                        loadAlbums()
                    }
                    Constants.FETCH_STARTED -> {
                        progressBar!!.visibility = View.VISIBLE
                        //                        gridView.setVisibility(View.INVISIBLE);
                        listView!!.visibility = View.INVISIBLE
                    }
                    Constants.FETCH_COMPLETED -> {
                        if (adapter == null) {
                            adapter =
                                CustomAlbumSelectAdapter(applicationContext, albums, albumCount)
                            //                            gridView.setAdapter(adapter);
                            listView!!.adapter = adapter
                            progressBar!!.visibility = View.INVISIBLE
                            //                            gridView.setVisibility(View.VISIBLE);
                            listView!!.visibility = View.VISIBLE
                            orientationBasedUI(resources.configuration.orientation)
                        } else {
                            adapter!!.notifyDataSetChanged()
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
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                loadAlbums()
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
        albums = null
        if (adapter != null) {
            adapter!!.releaseResources()
        }
        //        gridView.setOnItemClickListener(null);
        listView!!.onItemClickListener = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationBasedUI(newConfig.orientation)
    }

    private fun orientationBasedUI(orientation: Int) {
        val windowManager = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

//        if (adapter != null) {
//            int size = orientation == Configuration.ORIENTATION_PORTRAIT ? metrics.widthPixels / 2 : metrics.widthPixels / 4;
//            adapter.setLayoutParams(size);
//        }
//        gridView.setNumColumns(orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4);
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data)
            finish()
        }
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

    private fun loadAlbums() {
        startThread(AlbumLoaderRunnable())
    }

    private inner class AlbumLoaderRunnable : Runnable {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            if (adapter == null) {
                sendMessage(Constants.FETCH_STARTED)
            }
            val cursor = applicationContext.contentResolver
                .query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED
                )
            if (cursor == null) {
                sendMessage(Constants.ERROR)
                return
            }
            val temp = ArrayList<Album>(cursor.count)
            val albumSet = HashSet<Long>()
            var file: File
            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return
                    }
                    val albumId = cursor.getLong(cursor.getColumnIndex(projection[0]) ?: 0)
                    val album = cursor.getString(cursor.getColumnIndex(projection[1]) ?: 0)
                    val image = cursor.getString(cursor.getColumnIndex(projection[2]) ?: 0)

                    // 전체앨범표시
                    if (temp.size == 0) {
                        val allAlbum = Album(album, image)
                        allAlbum.name = "전체 보기"
                        albumCount[allAlbum.name] = (cursor.count.toString() + "").toLong()
                        temp.add(allAlbum)
                    }

                    // 앨범별 카운트
                    if (albumCount.containsKey(album)) {
                        albumCount[album] = albumCount[album]!! + 1
                    }
                    if (!albumSet.contains(albumId)) {
                        albumCount[album] = 1L

                        /*
                        It may happen that some image file paths are still present in cache,
                        though image file does not exist. These last as long as media
                        scanner is not run again. To avoid get such image file paths, check
                        if image file exists.
                         */file = File(image)
                        if (file.exists()) {
                            temp.add(Album(album, image))
                            albumSet.add(albumId)
                        }
                    }
                } while (cursor.moveToPrevious())
            }
            cursor.close()
            if (albums == null) {
                albums = ArrayList()
            }
            albums!!.clear()
            albums!!.addAll(temp)
            sendMessage(Constants.FETCH_COMPLETED)
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

    private fun sendMessage(what: Int) {
        if (handler == null) {
            return
        }
        val message = handler!!.obtainMessage()
        message.what = what
        message.sendToTarget()
    }

    override fun permissionGranted() {
        val message = handler!!.obtainMessage()
        message.what = Constants.PERMISSION_GRANTED
        message.sendToTarget()
    }

    override fun hideViews() {
        progressBar!!.visibility = View.INVISIBLE
        //        gridView.setVisibility(View.INVISIBLE);
        listView!!.visibility = View.INVISIBLE
    }
}