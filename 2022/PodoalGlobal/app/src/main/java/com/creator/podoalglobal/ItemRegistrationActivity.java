package com.creator.podoalglobal;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.creator.podoalglobal.helpers.Constants;
import com.creator.podoalglobal.models.Image;
import com.creator.podoalglobal.util.BitmapUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by skcrackers on 10/11/17.
 */

public class ItemRegistrationActivity extends AppCompatActivity {

    private ActionBar actionBar;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_item_registration);

            // Set a toolbar to  replace to action bar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            RelativeLayout rl_add = (RelativeLayout)findViewById(R.id.rl_add);
            rl_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!hasPermissions(ItemRegistrationActivity.this, PERMISSIONS)){
                        ActivityCompat.requestPermissions(ItemRegistrationActivity.this, PERMISSIONS, PERMISSION_ALL);
                    } else {
                        Intent intent = new Intent(ItemRegistrationActivity.this, SelectImageMethodActivity.class);
                        startActivityForResult(intent, Constants.REQUEST_CODE);
                    }
                }
            });

            // set actionBar
            actionBar = getSupportActionBar();
            if (actionBar != null){
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);

                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle("상품등록");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * For NicePay
     * 계좌이체 결과값을 받아와 오류시 해당 메세지를, 성공시에는 결과 페이지를 호출한다.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            Log.d("SeongKwon", "============================================");
            Log.d("SeongKwon", "requestCode = " + requestCode);
            Log.d("SeongKwon", "resultCode = " + resultCode);
            Log.d("SeongKwon", "============================================");
            if (requestCode == Constants.REQUEST_CODE && resultCode == RESULT_OK) {
                String result = "";
                try {
                    JSONObject jObj = new JSONObject();
                    jObj.put("resultcd", "0");              // 0:성공. 1:실패

                    LinearLayout ll_select = (LinearLayout)findViewById(R.id.ll_select);
                    ll_select.removeAllViews();

                    ArrayList<Image> selectedImages = (ArrayList<Image>) data.getExtras().get(Constants.INTENT_EXTRA_IMAGES);
                    for (int i = 0; i < selectedImages.size(); i++) {
                        Log.d("SeongKwon", selectedImages.get(i).path);

                        // 회전
                        Matrix matrix = new Matrix();
                        matrix.postRotate(BitmapUtil.GetExifOrientation(selectedImages.get(i).path));

                        int dstWidth = 200;
                        int dstHeight = 200;

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        Bitmap src = BitmapFactory.decodeFile(selectedImages.get(i).path, options);
//                        Bitmap resized = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);

                        int width = src.getWidth();
                        int height = src.getHeight();
//                        Bitmap resized = Bitmap.createBitmap(src, width / 2, height / 4, width / 2, height / 2, matrix, true);
                        Bitmap resized = Bitmap.createBitmap(src, width / 2, height / 4, height / 2, height / 2, matrix, true);

                        Log.d("SeongKwon", getBase64String(src));
                        Log.d("SeongKwon", getBase64String(resized));

                        ImageView iv = new ImageView(this);
                        iv.setImageBitmap(resized);
                        ll_select.addView(iv);
                    }

                    result = jObj.toString();
                    Log.d("SeongKwon", result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//            executeJavascript(mCallback + "(" + result +")");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String getBase64String(Bitmap bitmap)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] imageBytes = baos.toByteArray();

        String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        return base64String;
    }

    public boolean hasPermissions(Context context, String... permissions) {
        try {
            Log.d("SeongKwon", "================================= // 1");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                Log.d("SeongKwon", "================================= // 2");
                for (String permission : permissions) {
                    Log.d("SeongKwon", "================================= // 3");
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
            } else {
                Log.d("SeongKwon", "================================= // 4");
                Intent intent = new Intent(ItemRegistrationActivity.this, SelectImageMethodActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 사용자가 권한을 허용했는지 거부했는지 체크
     * @param requestCode   1번
     * @param permissions   개발자가 요청한 권한들
     * @param grantResults  권한에 대한 응답들
     *                    permissions와 grantResults는 인덱스 별로 매칭된다.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("SeongKwon", "=================================" + requestCode);
        Log.d("SeongKwon", "=================================" + permissions);
        Log.d("SeongKwon", "=================================" + grantResults);
        if (requestCode == 1) {
            /* 요청한 권한을 사용자가 "허용"했다면 인텐트를 띄워라
                내가 요청한 게 하나밖에 없기 때문에. 원래 같으면 for문을 돈다.*/
/*            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);*/

            for(int i = 0 ; i < permissions.length ; i++) {
                if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        System.out.println("onRequestPermissionsResult WRITE_EXTERNAL_STORAGE ( 권한 성공 ) ");
                    }
//                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                        System.out.println("onRequestPermissionsResult ACCESS_FINE_LOCATION ( 권한 성공 ) ");
//                    }
//                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                        System.out.println("onRequestPermissionsResult ACCESS_COARSE_LOCATION ( 권한 성공 ) ");
//                    }
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        System.out.println("onRequestPermissionsResult READ_PHONE_STATE ( 권한 성공 ) ");
                    }
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        System.out.println("onRequestPermissionsResult READ_EXTERNAL_STORAGE ( 권한 성공 ) ");
                    }
                }
            }
            Intent intent = new Intent(ItemRegistrationActivity.this, SelectImageMethodActivity.class);
            startActivityForResult(intent, Constants.REQUEST_CODE);
        } else {
            System.out.println("onRequestPermissionsResult ( 권한 거부) ");
            Toast.makeText(getApplicationContext(), "요청 권한 거부", Toast.LENGTH_SHORT).show();
        }
    }
}