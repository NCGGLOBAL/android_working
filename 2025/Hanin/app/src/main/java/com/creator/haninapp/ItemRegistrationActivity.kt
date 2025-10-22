package com.creator.haninapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.creator.haninapp.helpers.Constants
import com.creator.haninapp.models.Image
import com.creator.haninapp.util.BitmapUtil
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Created by skcrackers on 10/11/17.
 */
class ItemRegistrationActivity : AppCompatActivity() {
    private var actionBar: ActionBar? = null
    var PERMISSION_ALL = 1
    var PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA  // Photo Picker 사용으로 스토리지 권한 불필요
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_item_registration)

            // Set a toolbar to  replace to action bar
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)
            val rl_add = findViewById<View>(R.id.rl_add) as RelativeLayout
            rl_add.setOnClickListener {
                if (!hasPermissions(this@ItemRegistrationActivity, *PERMISSIONS)) {
                    ActivityCompat.requestPermissions(
                        this@ItemRegistrationActivity,
                        PERMISSIONS,
                        PERMISSION_ALL
                    )
                } else {
                    val intent =
                        Intent(this@ItemRegistrationActivity, SelectImageMethodActivity::class.java)
                    startActivityForResult(intent, Constants.REQUEST_CODE)
                }
            }

            // set actionBar
            actionBar = supportActionBar
            if (actionBar != null) {
                actionBar!!.setDisplayHomeAsUpEnabled(true)
                actionBar!!.setDisplayShowHomeEnabled(true)
                actionBar!!.setDisplayShowTitleEnabled(true)
                actionBar!!.title = "상품등록"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * For NicePay
     * 계좌이체 결과값을 받아와 오류시 해당 메세지를, 성공시에는 결과 페이지를 호출한다.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            Log.d("SeongKwon", "============================================")
            Log.d("SeongKwon", "requestCode = $requestCode")
            Log.d("SeongKwon", "resultCode = $resultCode")
            Log.d("SeongKwon", "============================================")
            if (requestCode == Constants.REQUEST_CODE && resultCode == RESULT_OK) {
                var result = ""
                try {
                    val jObj = JSONObject()
                    jObj.put("resultcd", "0") // 0:성공. 1:실패
                    val ll_select = findViewById<View>(R.id.ll_select) as LinearLayout
                    ll_select.removeAllViews()
                    val selectedImages =
                        data!!.extras!![Constants.INTENT_EXTRA_IMAGES] as ArrayList<Image>?
                    for (i in selectedImages!!.indices) {
                        Log.d("SeongKwon", selectedImages[i].path!!)

                        // 회전
                        val matrix = Matrix()
                        matrix.postRotate(
                            BitmapUtil.Companion.GetExifOrientation(selectedImages[i].path)
                                .toFloat()
                        )
                        val dstWidth = 200
                        val dstHeight = 200
                        val options = BitmapFactory.Options()
                        options.inSampleSize = 4
                        val src = BitmapFactory.decodeFile(selectedImages[i].path, options)
                        //                        Bitmap resized = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);
                        val width = src.width
                        val height = src.height
                        //                        Bitmap resized = Bitmap.createBitmap(src, width / 2, height / 4, width / 2, height / 2, matrix, true);
                        val resized = Bitmap.createBitmap(
                            src,
                            width / 2,
                            height / 4,
                            height / 2,
                            height / 2,
                            matrix,
                            true
                        )
                        Log.d("SeongKwon", getBase64String(src))
                        Log.d("SeongKwon", getBase64String(resized))
                        val iv = ImageView(this)
                        iv.setImageBitmap(resized)
                        ll_select.addView(iv)
                    }
                    result = jObj.toString()
                    Log.d("SeongKwon", result)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                //            executeJavascript(mCallback + "(" + result +")");
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBase64String(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }

    fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
        try {
            Log.d("SeongKwon", "================================= // 1")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                Log.d("SeongKwon", "================================= // 2")
                for (permission in permissions) {
                    Log.d("SeongKwon", "================================= // 3")
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            permission!!
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            } else {
                Log.d("SeongKwon", "================================= // 4")
                val intent =
                    Intent(this@ItemRegistrationActivity, SelectImageMethodActivity::class.java)
                startActivityForResult(intent, Constants.REQUEST_CODE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    /**
     * 사용자가 권한을 허용했는지 거부했는지 체크
     * @param requestCode   1번
     * @param permissions   개발자가 요청한 권한들
     * @param grantResults  권한에 대한 응답들
     * permissions와 grantResults는 인덱스 별로 매칭된다.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("SeongKwon", "=================================$requestCode")
        Log.d("SeongKwon", "=================================$permissions")
        Log.d("SeongKwon", "=================================$grantResults")
        if (requestCode == 1) {
            /* 요청한 권한을 사용자가 "허용"했다면 인텐트를 띄워라
                내가 요청한 게 하나밖에 없기 때문에. 원래 같으면 for문을 돈다.*/
/*            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);*/
            for (i in permissions.indices) {
                if (grantResults.size > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        println("onRequestPermissionsResult WRITE_EXTERNAL_STORAGE ( 권한 성공 ) ")
                    }
                    //                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                        System.out.println("onRequestPermissionsResult ACCESS_FINE_LOCATION ( 권한 성공 ) ");
//                    }
//                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                        System.out.println("onRequestPermissionsResult ACCESS_COARSE_LOCATION ( 권한 성공 ) ");
//                    }
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        println("onRequestPermissionsResult READ_PHONE_STATE ( 권한 성공 ) ")
                    }
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        println("onRequestPermissionsResult READ_EXTERNAL_STORAGE ( 권한 성공 ) ")
                    }
                }
            }
            val intent =
                Intent(this@ItemRegistrationActivity, SelectImageMethodActivity::class.java)
            startActivityForResult(intent, Constants.REQUEST_CODE)
        } else {
            println("onRequestPermissionsResult ( 권한 거부) ")
            Toast.makeText(applicationContext, "요청 권한 거부", Toast.LENGTH_SHORT).show()
        }
    }
}