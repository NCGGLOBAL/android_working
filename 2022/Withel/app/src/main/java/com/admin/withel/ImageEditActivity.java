package com.admin.withel;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.admin.withel.adapters.PageAdapter;
import com.admin.withel.croper.GOTOConstants;
import com.admin.withel.delegator.HNSharedPreference;
import com.admin.withel.helpers.Constants;
import com.admin.withel.models.Image;
import com.admin.withel.util.BitmapUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by skcrackers on 10/24/17.
 */

public class ImageEditActivity extends HelperActivity implements View.OnClickListener {
    private ActionBar actionBar;

    private ActionMode actionMode;

    private ViewPager mViewPager;
    private PageAdapter mAdapter;

    private int mCurrnetPage = 0;
    private ArrayList<Image> mSelectedImages;

    private boolean mIsFirst = true;
    private int savedImageSize = 0;

    private String mToken = "";     // 이미지 등록 Token
    private JSONArray mImgArr = null;     // 이미지 등록 Token

    private ProgressDialog mProgressDialog;  // 처리중
    private boolean mIsChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_image_edit);

            // 서버에서 내려온 이미지 경로
            Intent intent = getIntent();
            if(intent.hasExtra("imgArr") && intent.hasExtra("token")) {
                Log.e("SeongKwon", "=============================" + intent.getStringExtra("imgArr"));
                Log.e("SeongKwon", "=============================" + intent.getStringExtra("token"));
                Log.e("SeongKwon", "=============================" + intent.getIntExtra("pos", -1));
                mImgArr = new JSONArray(intent.getStringExtra("imgArr"));
                mToken = intent.getStringExtra("token");
                mCurrnetPage = intent.getIntExtra("pos", -1);
            }

            // Set a toolbar to  replace to action bar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // get Image
            getSelectedImages();

            // set actionBar
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);

                actionBar.setDisplayShowTitleEnabled(true);
                if(mAdapter != null && mViewPager != null) {
                    actionBar.setTitle(mAdapter.getCount() + "장 중 " + (mViewPager.getCurrentItem() + 1) + "번째 선택");
                }
                TextView tv_image_count = (TextView) findViewById(R.id.textview_header_image_count);
                TextView tv_header_right = (TextView) findViewById(R.id.textview_header_right);
                tv_header_right.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendIntent();
                    }
                });

                tv_image_count.setVisibility(View.GONE);
                tv_header_right.setVisibility(View.GONE);
            }

            Button btn_edit = (Button)findViewById(R.id.btn_register_gallery_edit);     // 사진편집 (rotate, crop)
            Button btn_delete = (Button)findViewById(R.id.btn_register_gallery_delete); // 사진삭제
            Button btn_order = (Button)findViewById(R.id.btn_register_gallery_order);   // 순서변경

            btn_edit.setOnClickListener(this);
            btn_delete.setOnClickListener(this);
            btn_order.setOnClickListener(this);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsFirst) {
            mIsFirst = false;
            if(mViewPager != null) {
                mViewPager.setCurrentItem(mCurrnetPage);
            }
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.btn_register_gallery_edit :   // 사진편집 (rotate, crop)
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
                intent = new Intent(this, ImageCropActivity.class);
                intent.putExtra("filePath", mSelectedImages.get(mCurrnetPage).path);
                startActivityForResult(intent, Constants.REQUEST_CROP_IMAGE);

                break;
            case R.id.btn_register_gallery_delete : // 사진삭제
                AlertDialog.Builder builder = new AlertDialog.Builder(ImageEditActivity.this);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(BitmapUtil.deleteImage(ImageEditActivity.this, mSelectedImages.get(mCurrnetPage).name)) {
                            try {
                                // imgJson에서 삭제
                                if(mImgArr != null) {
                                    JSONArray tmp = new JSONArray();
                                    for (int idx = 0; idx < mImgArr.length(); idx++) {
                                        JSONObject jObjItem = (JSONObject) mImgArr.get(idx);
                                        if (jObjItem.has("fileName")) {
                                            if (jObjItem.getString("fileName").equals(mSelectedImages.get(mCurrnetPage).name)) {
                                                continue;
                                            }
                                        }
                                        if (mCurrnetPage > idx + 1) {
                                            jObjItem.put("sort", idx);
                                        }
                                        tmp.put(jObjItem);
                                    }
                                    mImgArr = tmp;
                                }
                                mAdapter.removeItem(mCurrnetPage);
                                actionBar.setTitle(mAdapter.getCount() + "장 중 " + (mViewPager.getCurrentItem() + 1) + "번째 선택");

                                File file = new File(ImageEditActivity.this.getFilesDir() + "/");
                                File[] flist = file.listFiles();

                                if(flist.length == 0) {
                                    sendIntent();
                                } else {
                                    mViewPager.setCurrentItem(mCurrnetPage);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        mIsChanged = true;
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.setTitle("알림");
                builder.setMessage("선택하신 사진을 삭제하시겠습니까?");
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case R.id.btn_register_gallery_order : // 순서변경
                intent = new Intent(ImageEditActivity.this, ImageEditChangeOrder.class);
                intent.putExtra("imgArr", mImgArr.toString());
                intent.putExtra("token", mToken);
                startActivityForResult(intent, Constants.REQUEST_EDIT_IMAGE);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }

            default: {
                return false;
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.e("SeongKwon", "onBackPressed = " + mIsChanged);
        if(mIsChanged) {
            // 변경내역 있음
            Intent intent = new Intent();
            intent.putExtra("imgArr", mImgArr.toString());
            intent.putExtra("token", mToken);
            intent.putExtra("isChanged", true);
            setResult(RESULT_OK, intent);
            ImageEditActivity.this.finish();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 이미지 가져오기
     */
    private void getSelectedImages() {
        try {
            boolean isExist = false;

            File file = new File(getFilesDir() + "/");
            File[] flist = file.listFiles();
            Log.d("SeongKwon", "*************************************************");
            Log.d("SeongKwon", "imgcnt = " + flist.length);
            mSelectedImages = new ArrayList<Image>();
            savedImageSize = flist.length;
            for (int i = 0; i < flist.length; i++) {
                String fname = flist[i].getName();

                long id = -1;
                String name = fname;
                String path = file.getAbsolutePath() + "/" + name;
                boolean isSelected = true;

                Log.d("SeongKwon", "=========================");
                Log.d("SeongKwon", "id = " + id);
                Log.d("SeongKwon", "name = " + name);
                Log.d("SeongKwon", "path = " + path);
                Log.d("SeongKwon", "isSelected = " + isSelected);
                Log.d("SeongKwon", "=========================");

                if (file.exists()) {
                    mSelectedImages.add(new Image(id, name, path, isSelected, -1));
                }
            }

            // Data변경 - Data는 SharedPreference에 저장된 순서대로 보여진다.
            String savedImage = HNSharedPreference.getSharedPreference(ImageEditActivity.this, "savedImage");
            String[] savedImageArray = savedImage.split(",");

            // Data변경
            String fName = "";
            ArrayList<Image> tempArray = new ArrayList<Image>();
            for(int i = 0; i < savedImageArray.length; i++) {
                fName = savedImageArray[i].split("&")[0];
                for(int k = 0; k <  mSelectedImages.size(); k++) {
                    if(fName.equals(mSelectedImages.get(k).name)) {
                        tempArray.add(mSelectedImages.get(k));

                        break;
                    }
                }
            }
            mSelectedImages.clear();
            mSelectedImages.addAll(tempArray);

            setViewPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

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
            if(data != null) {
                if (data.hasExtra(GOTOConstants.IntentExtras.IMAGE_PATH) && data.hasExtra("isChanged")) {
                    Log.d("SeongKwon", data.getStringExtra(GOTOConstants.IntentExtras.IMAGE_PATH));

                    mIsChanged = data.getBooleanExtra("isChanged", false);

                    // 파일내용 변경
                    mAdapter.notifyDataSetChanged();
                }
            }
        } else if(requestCode == Constants.REQUEST_EDIT_IMAGE) {
            if (resultCode == RESULT_OK) {
                mIsChanged = data.getBooleanExtra("isChanged", false);
                if(mIsChanged) {
                    String savedImage = HNSharedPreference.getSharedPreference(ImageEditActivity.this, "savedImage");
                    String[] savedImageArray = savedImage.split(",");

                    // Data변경
                    String fName = "";
                    ArrayList<Image> tempArray = new ArrayList<Image>();
                    for(int i = 0; i < savedImageArray.length; i++) {
                        fName = savedImageArray[i].split("&")[0];
                        for(int k = 0; k < mSelectedImages.size(); k++) {
                            if(fName.equals(mSelectedImages.get(k).name)) {
                                tempArray.add(mSelectedImages.get(k));

                                break;
                            }
                        }
                    }
                    mSelectedImages.clear();
                    mSelectedImages.addAll(tempArray);

                    // 파일내용 변경
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    // DownloadImage AsyncTask
    private class DownloadImage extends AsyncTask<Object, Void, Bitmap> {
        String imgUrl = "";
        String fileName = "";
        int sort = -1;
        int utype = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Create a progressdialog
            mProgressDialog = new ProgressDialog(ImageEditActivity.this);
            mProgressDialog.setTitle("알림");
            mProgressDialog.setMessage("처리중입니다.\n잠시만 기다려 주세요.");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Bitmap doInBackground(Object... params) {


            imgUrl = (String)params[0];
            fileName = (String)params[1];
            sort = (int) params[2];
            utype = (int) params[3];

            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imgUrl).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            OutputStream outStream = null;

            File file = new File(fileName);
            try {
                outStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            long id = -1;
            String name = fileName;
            String path = file.getAbsolutePath() + "/" + name;
            boolean isSelected = true;

            Log.d("SeongKwon", "=========================");
            Log.d("SeongKwon", "id = " + id);
            Log.d("SeongKwon", "name = " + name);
            Log.d("SeongKwon", "path = " + path);
            Log.d("SeongKwon", "isSelected = " + isSelected);
            Log.d("SeongKwon", "=========================");

            mSelectedImages.add(new Image(id, name, path, isSelected, sort));

            // 화면 그리기
            setViewPage();

            // Close progressdialog
            mProgressDialog.dismiss();
        }
    }

    private void sendIntent() {
        Intent intent = new Intent();
        intent.putExtra("mImgArr", mImgArr.toString());
        intent.putExtra("token", mToken);
        setResult(RESULT_OK, intent);

        ImageEditActivity.this.finish();
    }

    private void setViewPage() {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mAdapter = new PageAdapter(this, mSelectedImages);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int arg0) {
            }

            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            public void onPageSelected(int currentPage) {
                actionBar.setTitle(mAdapter.getCount() + "장 중 " + (mViewPager.getCurrentItem() + 1) + "번째 선택");

                mCurrnetPage = currentPage;
            }
        });
    }
}