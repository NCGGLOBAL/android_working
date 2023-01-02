package com.nechingu.tagbadmin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import com.nechingu.tagbadmin.adapters.CustomImageSelectAdapter
import com.nechingu.tagbadmin.common.HNApplication
import com.nechingu.tagbadmin.helpers.Constants
import com.nechingu.tagbadmin.models.ImageItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class SelectImageMethodActivityKt : HelperActivity() {
    private val TAG = SelectImageMethodActivity::class.java.simpleName

    private val errorDisplay: TextView? = null
    private var imageCount: TextView? = null
    private var gridView: GridView? = null
    private var adapter: CustomImageSelectAdapter? = null
    private var actionBar: ActionBar? = null

    private val mImageItems: ArrayList<ImageItem>? = arrayListOf()

    private var mToken = "" // 이미지 등록 Token
    private var mImgArr: JSONArray? = null // 이미지 Array
    private val mIsChanged = false
    private var mPageGbn = "2"
    private var mCnt = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_image_method)

        // 서버에서 내려온 이미지 경로
        val intent = intent
        if (intent.hasExtra("imgArr") && intent.hasExtra("token")) {
            mImgArr = JSONArray(intent.getStringExtra("imgArr"))
            mToken = intent.getStringExtra("token")
        }
        // 1 : 신규페이지에서 진입, 2 : 수정페이지에서 진입
        // 1 : 신규페이지에서 진입, 2 : 수정페이지에서 진입
        mPageGbn = "2"
        if (intent.hasExtra("pageGbn") && intent.hasExtra("cnt")) {
            mPageGbn = intent.getStringExtra("pageGbn")
            mCnt = intent.getStringExtra("cnt")
            if (mPageGbn == "1") {
                if (HNApplication.mImgArrForReg != "" && mCnt != "0") {
                    mImgArr = JSONArray(HNApplication.mImgArrForReg)
                } else {
                    HNApplication.mImgArrForReg = ""
                }
            } else {
                HNApplication.mImgArrForReg = mImgArr.toString()
            }
        }
        Log.d(TAG, "pageGbn = $mPageGbn")
        Log.d(TAG, "mImgArr = $mImgArr")

        setController()
    }

    private fun setController() {
        // Set a toolbar to  replace to action bar
        // Set a toolbar to  replace to action bar
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        imageCount = findViewById<TextView>(R.id.textview_header_image_count)

        setHeaderView()
        setMainView()
    }

    private fun setMainView() {
        gridView = findViewById<GridView>(R.id.grid_view_image_select)

        gridView?.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            // 수정
            val intent = Intent(this@SelectImageMethodActivityKt, ImageEditActivity::class.java)
            intent.putExtra("token", mToken)
            intent.putExtra("imgArr", mImgArr.toString())
            intent.putExtra("pos", position)
            startActivityForResult(intent, Constants.REQUEST_EDIT_IMAGE)
        }

        val cameraLayout = findViewById<View>(R.id.cameraLayout) as LinearLayout
        val folderLayout = findViewById<View>(R.id.folderLayout) as LinearLayout

//        cameraLayout.setOnClickListener(this)
//        folderLayout.setOnClickListener(this)
        mImgArr?.let {
            for (idx in 0 until it.length()) {
                val jObjItem = it[idx] as JSONObject
                val fileName = jObjItem.getString("fileName")
                val imgUrl = jObjItem.getString("imgUrl")
                val sort = jObjItem.getInt("sort")
                val utype = jObjItem.getInt("utype") // 0: 기존이미지, 1: 신규, 2: 수정

                val item = ImageItem(fileName, imgUrl, sort, utype, false)
                mImageItems?.add(item)
            }

//            imageCount?.text = "${mImageItems?.size}/8"
//            adapter = CustomImageSelectAdapter(this@SelectImageMethodActivityKt, mImageItems)
//            orientationBasedUI(Configuration.ORIENTATION_PORTRAIT)
//            gridView?.setAdapter(adapter)
        }
    }

    private fun setHeaderView() {
        // set actionBar
        // set actionBar
        actionBar = supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(true)
            setTitle("사진추가")
        }

        val textview_header_right = findViewById<View>(R.id.textview_header_right) as TextView
        textview_header_right.setOnClickListener {
            val intent = Intent()
            if (mImgArr != null) {
                mCnt = mImgArr!!.length().toString()
                Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!pageGbn = $mPageGbn")
                Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!mImgArr = $mImgArr")
                Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!mCnt = $mCnt")
                if (mIsChanged) {
                    intent.putExtra("isChanged", true)
                }
                intent.putExtra("imgArr", mImgArr.toString())
                intent.putExtra("token", mToken)
                intent.putExtra("cnt", mCnt)
                intent.putExtra("pageGbn", mPageGbn)
            } else {
//                intent.putParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES, mImageItems)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

    }

    private fun orientationBasedUI(orientation: Int) {
        val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        if (adapter != null) {
            val size = if (orientation == Configuration.ORIENTATION_PORTRAIT) metrics.widthPixels / 3 else metrics.widthPixels / 5
            adapter!!.setLayoutParams(size)
        }
        gridView!!.numColumns = if (orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5
    }
}