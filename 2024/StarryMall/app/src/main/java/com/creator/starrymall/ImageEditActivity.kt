package com.creator.starrymall

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.creator.starrymall.adapters.PageAdapter
import com.creator.starrymall.croper.GOTOConstants.IntentExtras
import com.creator.starrymall.delegator.HNSharedPreference
import com.creator.starrymall.helpers.Constants
import com.creator.starrymall.models.Image
import com.creator.starrymall.util.BitmapUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.*

/**
 * Created by skcrackers on 10/24/17.
 */
class ImageEditActivity : HelperActivity(), View.OnClickListener {
    private var actionBar: ActionBar? = null
    private val actionMode: ActionMode? = null
    private var mViewPager: ViewPager? = null
    private var mAdapter: PageAdapter? = null
    private var mCurrnetPage = 0
    private var mSelectedImages: ArrayList<Image>? = null
    private var mIsFirst = true
    private var savedImageSize = 0
    private var mToken: String? = "" // 이미지 등록 Token
    private var mImgArr: JSONArray? = null // 이미지 등록 Token
    private var mProgressDialog // 처리중
            : ProgressDialog? = null
    private var mIsChanged = false
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_image_edit)

            // 서버에서 내려온 이미지 경로
            val intent = intent
            if (intent.hasExtra("imgArr") && intent.hasExtra("token")) {
                Log.e(
                    "SeongKwon",
                    "=============================" + intent.getStringExtra("imgArr")
                )
                Log.e("SeongKwon", "=============================" + intent.getStringExtra("token"))
                Log.e("SeongKwon", "=============================" + intent.getIntExtra("pos", -1))
                mImgArr = JSONArray(intent.getStringExtra("imgArr"))
                mToken = intent.getStringExtra("token")
                mCurrnetPage = intent.getIntExtra("pos", -1)
            }

            // Set a toolbar to  replace to action bar
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)

            // get Image
            selectedImages

            // set actionBar
            actionBar = supportActionBar
            if (actionBar != null) {
                actionBar!!.setDisplayHomeAsUpEnabled(true)
                actionBar!!.setDisplayShowHomeEnabled(true)
                actionBar!!.setDisplayShowTitleEnabled(true)
                if (mAdapter != null && mViewPager != null) {
                    actionBar!!.title =
                        mAdapter!!.count.toString() + "장 중 " + (mViewPager!!.currentItem + 1) + "번째 선택"
                }
                val tv_image_count =
                    findViewById<View>(R.id.textview_header_image_count) as TextView
                val tv_header_right = findViewById<View>(R.id.textview_header_right) as TextView
                tv_header_right.setOnClickListener { sendIntent() }
                tv_image_count.visibility = View.GONE
                tv_header_right.visibility = View.GONE
            }
            val btn_edit =
                findViewById<View>(R.id.btn_register_gallery_edit) as Button // 사진편집 (rotate, crop)
            val btn_delete = findViewById<View>(R.id.btn_register_gallery_delete) as Button // 사진삭제
            val btn_order = findViewById<View>(R.id.btn_register_gallery_order) as Button // 순서변경
            btn_edit.setOnClickListener(this)
            btn_delete.setOnClickListener(this)
            btn_order.setOnClickListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mIsFirst) {
            mIsFirst = false
            if (mViewPager != null) {
                mViewPager!!.currentItem = mCurrnetPage
            }
        }
    }

    override fun onClick(view: View) {
        var intent: Intent? = null
        when (view.id) {
            R.id.btn_register_gallery_edit -> {
                //                CropImage.activity(Uri.fromFile(new File(mSelectedImages.get(mCurrnetPage).path)))
//                        .setOutputUri(Uri.fromFile(new File(mSelectedImages.get(mCurrnetPage).path)))
//                        .setGuidelines(CropImageView.Guidelines.ON)
//                        .setAutoZoomEnabled(false)
//                        .setActivityTitle("사진편집")
//                        .setCropShape(CropImageView.CropShape.RECTANGLE)
//                        .setAllowFlipping(false)
//                        .setCropMenuCropButtonTitle("완료")
//                        .setScaleType(CropImageView.ScaleType.FIT_CENTER)
//                        .start(this);
                intent = Intent(this, ImageCropActivity::class.java)
                intent.putExtra("filePath", mSelectedImages!![mCurrnetPage].path)
                startActivityForResult(intent, Constants.REQUEST_CROP_IMAGE)
            }
            R.id.btn_register_gallery_delete -> {
                val builder = AlertDialog.Builder(this@ImageEditActivity)
                builder.setPositiveButton(R.string.confirm) { dialog, id ->
                    if (BitmapUtil.deleteImage(
                            this@ImageEditActivity,
                            mSelectedImages!![mCurrnetPage].name
                        )
                    ) {
                        try {
                            // imgJson에서 삭제
                            if (mImgArr != null) {
                                val tmp = JSONArray()
                                var idx = 0
                                while (idx < mImgArr!!.length()) {
                                    val jObjItem = mImgArr!![idx] as JSONObject
                                    if (jObjItem.has("fileName")) {
                                        if (jObjItem.getString("fileName") == mSelectedImages!![mCurrnetPage].name) {
                                            idx++
                                            continue
                                        }
                                    }
                                    if (mCurrnetPage > idx + 1) {
                                        jObjItem.put("sort", idx)
                                    }
                                    tmp.put(jObjItem)
                                    idx++
                                }
                                mImgArr = tmp
                            }
                            mAdapter!!.removeItem(mCurrnetPage)
                            actionBar!!.setTitle(mAdapter!!.count.toString() + "장 중 " + (mViewPager!!.currentItem + 1) + "번째 선택")
                            val file = File(this@ImageEditActivity.filesDir.toString() + "/")
                            val flist = file.listFiles()
                            if (flist.size == 0) {
                                sendIntent()
                            } else {
                                mViewPager!!.currentItem = mCurrnetPage
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    mIsChanged = true
                }
                builder.setNegativeButton(R.string.cancel) { dialog, id -> dialog.cancel() }
                builder.setTitle("알림")
                builder.setMessage("선택하신 사진을 삭제하시겠습니까?")
                val dialog = builder.create()
                dialog.show()
            }
            R.id.btn_register_gallery_order -> {
                intent = Intent(this@ImageEditActivity, ImageEditChangeOrder::class.java)
                intent.putExtra("imgArr", mImgArr.toString())
                intent.putExtra("token", mToken)
                startActivityForResult(intent, Constants.REQUEST_EDIT_IMAGE)
            }
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

    override fun onBackPressed() {
        Log.e("SeongKwon", "onBackPressed = $mIsChanged")
        if (mIsChanged) {
            // 변경내역 있음
            val intent = Intent()
            intent.putExtra("imgArr", mImgArr.toString())
            intent.putExtra("token", mToken)
            intent.putExtra("isChanged", true)
            setResult(RESULT_OK, intent)
            finish()
        } else {
            super.onBackPressed()
        }
    }// Data변경 - Data는 SharedPreference에 저장된 순서대로 보여진다.

    // Data변경
    /**
     * 이미지 가져오기
     */
    private val selectedImages: Unit
        private get() {
            try {
                val isExist = false
                val file = File("$filesDir/")
                val flist = file.listFiles()
                Log.d("SeongKwon", "*************************************************")
                Log.d("SeongKwon", "imgcnt = " + flist.size)
                mSelectedImages = ArrayList()
                savedImageSize = flist.size
                for (i in flist.indices) {
                    val fname = flist[i].name
                    val id: Long = -1
                    val path = file.absolutePath + "/" + fname
                    val isSelected = true
                    Log.d("SeongKwon", "=========================")
                    Log.d("SeongKwon", "id = $id")
                    Log.d("SeongKwon", "name = $fname")
                    Log.d("SeongKwon", "path = $path")
                    Log.d("SeongKwon", "isSelected = $isSelected")
                    Log.d("SeongKwon", "=========================")
                    if (file.exists()) {
                        mSelectedImages!!.add(
                            Image(
                                id,
                                fname,
                                path,
                                isSelected,
                                -1
                            )
                        )
                    }
                }

                // Data변경 - Data는 SharedPreference에 저장된 순서대로 보여진다.
                val savedImage =
                    HNSharedPreference.getSharedPreference(this@ImageEditActivity, "savedImage")
                val savedImageArray = savedImage!!.split(",").toTypedArray()

                // Data변경
                var fName = ""
                val tempArray = ArrayList<Image>()
                for (i in savedImageArray.indices) {
                    fName = savedImageArray[i].split("&").toTypedArray()[0]
                    for (k in mSelectedImages!!.indices) {
                        if (fName == mSelectedImages!![k].name) {
                            tempArray.add(mSelectedImages!![k])
                            break
                        }
                    }
                }
                mSelectedImages!!.clear()
                mSelectedImages!!.addAll(tempArray)
                setViewPage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // handle result of CropImageActivity
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//            if (resultCode == RESULT_OK) {
//                // ((ImageView) findViewById(R.id.quick_start_cropped_image)).setImageURI(result.getUri());
//                Log.d("SeongKwon", result.getUri().getPath());
//
//                // 파일내용 변경
//                mAdapter.notifyDataSetChanged();
//                Toast.makeText(this, "Cropping successful, Sample: " + result.getSampleSize(), Toast.LENGTH_LONG).show();
//
//            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
//                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
//            }
//        } else
        if (requestCode == Constants.REQUEST_CROP_IMAGE) {
            if (data != null) {
                if (data.hasExtra(IntentExtras.Companion.IMAGE_PATH) && data.hasExtra("isChanged")) {
                    Log.d("SeongKwon", data.getStringExtra(IntentExtras.Companion.IMAGE_PATH)!!)
                    mIsChanged = data.getBooleanExtra("isChanged", false)

                    // 파일내용 변경
                    mAdapter!!.notifyDataSetChanged()
                }
            }
        } else if (requestCode == Constants.REQUEST_EDIT_IMAGE) {
            if (resultCode == RESULT_OK) {
                mIsChanged = data!!.getBooleanExtra("isChanged", false)
                if (mIsChanged) {
                    val savedImage =
                        HNSharedPreference.getSharedPreference(this@ImageEditActivity, "savedImage")
                    val savedImageArray = savedImage!!.split(",").toTypedArray()

                    // Data변경
                    var fName = ""
                    val tempArray = ArrayList<Image>()
                    for (i in savedImageArray.indices) {
                        fName = savedImageArray[i].split("&").toTypedArray()[0]
                        for (k in mSelectedImages!!.indices) {
                            if (fName == mSelectedImages!![k].name) {
                                tempArray.add(mSelectedImages!![k])
                                break
                            }
                        }
                    }
                    mSelectedImages!!.clear()
                    mSelectedImages!!.addAll(tempArray)

                    // 파일내용 변경
                    mAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    // DownloadImage AsyncTask
//    private inner class DownloadImage : AsyncTask<Any?, Void?, Bitmap?>() {
//        var imgUrl = ""
//        var fileName = ""
//        var sort = -1
//        var utype = 0
//        override fun onPreExecute() {
//            super.onPreExecute()
//            // Create a progressdialog
//            mProgressDialog = ProgressDialog(this@ImageEditActivity)
//            mProgressDialog!!.setTitle("알림")
//            mProgressDialog!!.setMessage("처리중입니다.\n잠시만 기다려 주세요.")
//            mProgressDialog!!.isIndeterminate = false
//            mProgressDialog!!.show()
//        }
//
//        override fun doInBackground(vararg params: Any?): Bitmap? {
//            imgUrl = params[0] as String
//            fileName = params[1] as String
//            sort = params[2] as Int
//            utype = params[3] as Int
//            var bitmap: Bitmap? = null
//            try {
//                // Download Image from URL
//                val input = URL(imgUrl).openStream()
//                // Decode Bitmap
//                bitmap = BitmapFactory.decodeStream(input)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            return bitmap
//        }
//
//        override fun onPostExecute(bitmap: Bitmap?) {
//            var outStream: OutputStream? = null
//            val file = File(fileName)
//            try {
//                outStream = FileOutputStream(file)
//                bitmap!!.compress(CompressFormat.JPEG, 100, outStream)
//                outStream.flush()
//                outStream.close()
//            } catch (e: FileNotFoundException) {
//                e.printStackTrace()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//            val id: Long = -1
//            val name = fileName
//            val path = file.absolutePath + "/" + name
//            val isSelected = true
//            Log.d("SeongKwon", "=========================")
//            Log.d("SeongKwon", "id = $id")
//            Log.d("SeongKwon", "name = $name")
//            Log.d("SeongKwon", "path = $path")
//            Log.d("SeongKwon", "isSelected = $isSelected")
//            Log.d("SeongKwon", "=========================")
//            mSelectedImages!!.add(Image(id, name, path, isSelected, sort))
//
//            // 화면 그리기
//            setViewPage()
//
//            // Close progressdialog
//            mProgressDialog!!.dismiss()
//        }
//
//    }

    private fun sendIntent() {
        val intent = Intent()
        intent.putExtra("mImgArr", mImgArr.toString())
        intent.putExtra("token", mToken)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setViewPage() {
        mViewPager = findViewById<View>(R.id.view_pager) as ViewPager
        mAdapter = PageAdapter(this, mSelectedImages)
        mViewPager!!.adapter = mAdapter
        mViewPager!!.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(arg0: Int) {}
            override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}
            override fun onPageSelected(currentPage: Int) {
                actionBar!!.title =
                    mAdapter!!.count.toString() + "장 중 " + (mViewPager!!.currentItem + 1) + "번째 선택"
                mCurrnetPage = currentPage
            }
        })
    }
}