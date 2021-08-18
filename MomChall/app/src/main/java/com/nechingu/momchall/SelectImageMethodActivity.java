package com.nechingu.momchall;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nechingu.momchall.adapters.CustomImageSelectAdapter;
import com.nechingu.momchall.common.HNApplication;
import com.nechingu.momchall.delegator.HNSharedPreference;
import com.nechingu.momchall.helpers.Constants;
import com.nechingu.momchall.models.Image;
import com.nechingu.momchall.util.BitmapUtil;
import com.nechingu.momchall.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by skcrackers on 9/22/17.
 */

public class SelectImageMethodActivity extends HelperActivity implements View.OnClickListener {
    private final String TAG = SelectImageMethodActivity.class.getSimpleName();
    private int mCameraType = 0;            // 카메라 Type
    private String mCurrentPhotoPath;       // 촬영된 이미지 경로

    // ============================= Album =============================
    private ArrayList<Image> mImageItems;
    private String album;

    private TextView errorDisplay;
    private TextView imageCount;

    private GridView gridView;
    private CustomImageSelectAdapter adapter;

    private ActionBar actionBar;

    private ActionMode actionMode;
    private int countSelected;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private int savedImageSize = 0;

    private String mToken = "";     // 이미지 등록 Token
    private JSONArray mImgArr = null;     // 이미지 Array
    private boolean mIsChanged = false;
    private String mPageGbn = "2";
    private String mCnt = "0";

    private Uri mImageCaptureUri = null;

    private final String[] projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA};
    // ============================= Album =============================

    private ProgressDialog mProgressDialog;
    private int mImgDownloadingCnt = 0;

    private final String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "ncg/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "#onCreate()");
        try {
            setContentView(R.layout.activity_select_image_method);

            mProgressDialog = new ProgressDialog(SelectImageMethodActivity.this);  // 처리중

            // 서버에서 내려온 이미지 경로
            Intent intent = getIntent();
            if(intent.hasExtra("imgArr") && intent.hasExtra("token")) {
                mImgArr = new JSONArray(intent.getStringExtra("imgArr"));
                mToken = intent.getStringExtra("token");
            }

            // 1 : 신규페이지에서 진입, 2 : 수정페이지에서 진입
            mPageGbn = "2";
            if(intent.hasExtra("pageGbn") && intent.hasExtra("cnt")) {
                mPageGbn = intent.getStringExtra("pageGbn");
                mCnt = intent.getStringExtra("cnt");

                if(mPageGbn.equals("1")) {
                    if (!HNApplication.mImgArrForReg.equals("") && !mCnt.equals("0")) {
                        mImgArr = new JSONArray(HNApplication.mImgArrForReg);
                    } else {
                        HNApplication.mImgArrForReg = "";
                    }
                } else{
                    HNApplication.mImgArrForReg = mImgArr.toString();

                }
            }
            Log.d(TAG, "pageGbn = " + mPageGbn);
            Log.d(TAG, "mImgArr = " + mImgArr);

            // Set a toolbar to  replace to action bar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            imageCount = (TextView) findViewById(R.id.textview_header_image_count);
            gridView = (GridView) findViewById(R.id.grid_view_image_select);

            // set actionBar
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);

                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle("사진추가");

                TextView textview_header_right = (TextView) findViewById(R.id.textview_header_right);
                textview_header_right.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        if(mImgArr != null) {
                            mCnt = String.valueOf(mImgArr.length());
                            Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!pageGbn = " + mPageGbn);
                            Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!mImgArr = " + mImgArr);
                            Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!mCnt = " + mCnt);
                            if(mIsChanged) {
                                intent.putExtra("isChanged", true);
                            }
                            intent.putExtra("imgArr", mImgArr.toString());
                            intent.putExtra("token", mToken);
                            intent.putExtra("cnt", mCnt);
                            intent.putExtra("pageGbn", mPageGbn);
                        } else {
                            intent.putParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES, mImageItems);
                        }
                        setResult(RESULT_OK, intent);
                        SelectImageMethodActivity.this.finish();
                    }
                });
            }
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // 수정
                    Intent intent = new Intent(SelectImageMethodActivity.this, ImageEditActivity.class);
                    intent.putExtra("token", mToken);
                    intent.putExtra("imgArr", mImgArr.toString());
                    intent.putExtra("pos", position);
                    startActivityForResult(intent, Constants.REQUEST_EDIT_IMAGE);
                }
            });

            LinearLayout cameraLayout = (LinearLayout) findViewById(R.id.cameraLayout);
            LinearLayout folderLayout = (LinearLayout) findViewById(R.id.folderLayout);

            cameraLayout.setOnClickListener(this);
            folderLayout.setOnClickListener(this);

            boolean isExist = false;
            int isGarbageCount = 0;

            File file = new File(mFilePath);
            File[] flist = file.listFiles();
            if (mImageItems == null) {
                mImageItems = new ArrayList<Image>();
            }
            savedImageSize = flist.length;
            // 이미지 존재여부 확인
            // [{"utype":"0","fileName":"Screenshot_2018-01-10-13-31-08-630_com.wavayo.soho.png","imgUrl":"http:\/\/osaka.wavayo.com\/data\/osaka\/goods\/mallshopping\/other\/201801\/Screenshot_2018-01-10-13-31-08-630_com.wavayo.soho.png","sort":"1"},{"utype":"0","fileName":"Screenshot_2017-11-23-15-11-37-635_com.miui.packageinstaller.png","imgUrl":"http:\/\/osaka.wavayo.com\/data\/osaka\/goods\/mallshopping\/other\/201801\/Screenshot_2017-11-23-15-11-37-635_com.miui.packageinstaller.png","sort":"2"},{"utype":"0","fileName":"Screenshot_2017-09-29-10-44-49-373_com.miui.gallery.png","imgUrl":"http:\/\/osaka.wavayo.com\/data\/osaka\/goods\/mallshopping\/other\/201801\/Screenshot_2017-09-29-10-44-49-373_com.miui.gallery.png","sort":"3"}]
            HNSharedPreference.putSharedPreference(this, "savedImage" , "");        // 초기화
            if (mImgArr != null) {
                if (mImgArr.length() > 0) {
                    savedImageSize = mImgArr.length();
                    isExist = false;
                    for (int idx = 0; idx < mImgArr.length(); idx++) {
                        JSONObject jObjItem = (JSONObject) mImgArr.get(idx);
                        String fileName = jObjItem.getString("fileName");
                        String imgUrl = jObjItem.getString("imgUrl");
                        int sort = jObjItem.getInt("sort");
                        int utype = jObjItem.getInt("utype");   // 0: 기존이미지, 1: 신규, 2: 수정

                        if(flist.length > 0) {
                            for (int i = 0; i < flist.length; i++) {
                                String fname = flist[i].getName();

                                if (fileName.equals(fname)) {
                                    isExist = true;

                                    String savedImage = HNSharedPreference.getSharedPreference(SelectImageMethodActivity.this, "savedImage");
                                    savedImage += fileName + "&" + sort + ",";
                                    HNSharedPreference.putSharedPreference(SelectImageMethodActivity.this, "savedImage", savedImage);

                                }
                            }
                            if (!isExist) {
                                // 이미지 다운로드
                                new SelectImageMethodActivity.DownloadImage().execute(imgUrl, fileName, sort, utype);
                            }
                        } else {
                            new SelectImageMethodActivity.DownloadImage().execute(imgUrl, fileName, sort, utype);
                        }
                    }
                } else {
                    // 전체삭제
                    BitmapUtil.deleteImages(this, mFilePath);
                }
            }
            imageCount.setText(savedImageSize + "/8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.cameraLayout:
                    mCameraType = 3;
                    requestPermission(Constants.REQUEST_SELECT_IMAGE_CAMERA);
                    break;
                case R.id.folderLayout:
                    mCameraType = 4;
                    requestPermission(Constants.REQUEST_SELECT_IMAGE_ALBUM);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mIsChanged) {
//            mIsChanged = false;
            return;
        }

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.PERMISSION_GRANTED: {
                        Log.d(TAG, "PERMISSION_GRANTED");

                        loadImages();
                        break;
                    }

                    case Constants.FETCH_STARTED: {
                        Log.d(TAG, "FETCH_STARTED");
//                        progressBar.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case Constants.FETCH_COMPLETED: {
                        /*
                        If adapter is null, this implies that the loaded mImageItems will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */
                        Log.d(TAG, "FETCH_COMPLETED mImageItems.size() : " + mImageItems.size());
                        if (adapter == null) {
                            adapter = new CustomImageSelectAdapter(getApplicationContext(), mImageItems, false);
                            gridView.setAdapter(adapter);

                            gridView.setVisibility(View.VISIBLE);
                            orientationBasedUI(getResources().getConfiguration().orientation);
                            Log.d(TAG, "FETCH_COMPLETED : ====================================1");
                        } else {
                            adapter.notifyDataSetChanged();
                            /*
                            Some selected mImageItems may have been deleted
                            hence update action mode title
                             */
                            Log.d(TAG, "FETCH_COMPLETED : ====================================2");
                            if (actionMode != null) {
                                countSelected = msg.arg1;
//                                actionMode.setTitle("상품등록");
                            }
                        }
                        imageCount.setText(savedImageSize + "/8");
                        break;
                    }

                    case Constants.ERROR: {
                        Log.d(TAG, "ERROR");
//                        progressBar.setVisibility(View.INVISIBLE);
//                        errorDisplay.setVisibility(View.VISIBLE);
                        break;
                    }

                    default: {
                        super.handleMessage(msg);
                    }
                }
            }
        };
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                loadImages();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);
        checkPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();

        deselectAll();

        stopThread();

        if(observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 다이얼로그 닫기
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        mImageItems = null;
        if (adapter != null) {
            adapter.releaseResources();
        }
        gridView.setOnItemClickListener(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationBasedUI(newConfig.orientation);
    }

    private void orientationBasedUI(int orientation) {
        final WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        if (adapter != null) {
            int size = orientation == Configuration.ORIENTATION_PORTRAIT ? metrics.widthPixels / 3 : metrics.widthPixels / 5;
            adapter.setLayoutParams(size);
        }
        gridView.setNumColumns(orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5);
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
        if(mIsChanged) {
            mCnt = String.valueOf(mImgArr.length());

            Intent intent = new Intent();
            intent.putExtra("imgArr", mImgArr.toString());
            intent.putExtra("token", mToken);
            intent.putExtra("isChanged", true);
            intent.putExtra("cnt", mCnt);
            intent.putExtra("pageGbn", mPageGbn);
            setResult(RESULT_OK, intent);
            SelectImageMethodActivity.this.finish();
        } else {
            super.onBackPressed();
        }
    }

    private ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_contextual_action_bar, menu);

            actionMode = mode;
            countSelected = 0;

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int i = item.getItemId();
            if (i == R.id.menu_item_add_image) {
                sendIntent();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (countSelected > 0) {
                deselectAll();
            }
            actionMode = null;
        }
    };

    private void toggleSelection(int position) {
        if (!mImageItems.get(position).isSelected && countSelected >= Constants.limit) {
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.limit_exceeded), Constants.limit), Toast.LENGTH_SHORT).show();
            return;
        }

        mImageItems.get(position).isSelected = !mImageItems.get(position).isSelected;
        if (mImageItems.get(position).isSelected) {
            countSelected++;

            mImageItems.get(position).sequence = countSelected;
//            Log.d("===============", mImageItems.get(position).sequence + "//POSITION");
        } else {
            countSelected--;

            // 재정렬
            int tmpSequence = mImageItems.get(position).sequence;
            mImageItems.get(position).sequence = -1;
//            Log.d("delete===============", "toggleSelection = " + position + "// sequence = " + mImageItems.get(position).sequence);
            for (int idx = 0; idx < mImageItems.size(); idx++) {
                if (mImageItems.get(idx).isSelected) {
                    if (mImageItems.get(idx).sequence > tmpSequence) {
                        mImageItems.get(idx).sequence = mImageItems.get(idx).sequence - 1;
                    }
                }
//                Log.d("update===============", "idx = " + idx + " // sequence = " + mImageItems.get(idx).sequence);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void deselectAll() {
        for (int i = 0, l = mImageItems.size(); i < l; i++) {
            mImageItems.get(i).isSelected = false;
        }
        countSelected = 0;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private ArrayList<Image> getSelected() {
        String path = "";

        ArrayList<Image> selectedImages = new ArrayList<>(countSelected);
        selectedImages.clear();
        for (int i = 0; i < countSelected; i++) {
            selectedImages.add(i, mImageItems.get(0));
        }

        for (int i = 0; i < mImageItems.size(); i++) {
            if (mImageItems.get(i).isSelected) {
                path = path + ((mImageItems.get(i).sequence - 1)) + "//" + mImageItems.get(i).path + "\n\n";
                Log.e(TAG, "getSelected path = " + mImageItems.get(i).path);
                Log.e(TAG, "getSelected " + (mImageItems.get(i).sequence - 1) + "//" + selectedImages.size() + "//" + countSelected);
                selectedImages.set(mImageItems.get(i).sequence - 1, mImageItems.get(i));

                try {
                    // ACT1011 CALLBACK
                    JSONObject jObjItem = new JSONObject();
                    jObjItem.put("imgUrl", "");
                    jObjItem.put("fileName", mImageItems.get(i).name);
                    jObjItem.put("utype", 1);   // utype =  0: 기존이미지, 1: 신규, 2: 수정
                    jObjItem.put("sort", mImageItems.get(i).sequence);    // 마지막에 추가
                    mImgArr.put(jObjItem);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Image 저장
                if(BitmapUtil.saveImage(this,
                        mImageItems.get(i).path,
                        mImageItems.get(i).name,
                        String.valueOf(mImageItems.get(i).sequence))) {
                }
            }
        }

        Log.d(TAG, "getSelected countSelected = " + countSelected);
        Log.d(TAG, "getSelected mImageItems.size() = " + mImageItems.size());
        Log.d(TAG, "getSelected selectedImages.size() = " + selectedImages.size());

        return selectedImages;
    }

    private void sendIntent() {
        new saveImagesAsyncTask().execute();
    }

    private void loadImages() {
        Handler handler = new Handler();
        handler.post(new ImageLoaderRunnable());
    }

    private class ImageLoaderRunnable implements Runnable {
        @Override
        public void run() {
//            android.os.Process.setThreadPriority(java.lang.Process.THREAD_PRIORITY_BACKGROUND);
            /*
            If the adapter is null, this is first time this activity's view is
            being shown, hence send FETCH_STARTED message to show progress bar
            while mImageItems are loaded from phone
             */
            if (adapter == null) {
                sendMessage(Constants.FETCH_STARTED);
            }

            // ================== 2018.01.08 아이폰과 동일하게 적용하기 위해 변경 ==================
            try {
                boolean isExist = false;

                File file = new File(mFilePath);
                File[] flist = file.listFiles();
                Log.d(TAG, "ImageLoaderRunnable : " + mFilePath + "/imgcnt = " + flist.length);
                if (mImageItems == null) {
                    mImageItems = new ArrayList<Image>();
                }
                mImageItems.clear();
                savedImageSize = flist.length;

                for (int i = 0; i < flist.length; i++) {
                    String fname = flist[i].getName();

                    long id = -1;
                    String name = fname;
                    String path = file.getAbsolutePath() + "/" + name;
                    boolean isSelected = true;

                    // getSequence
                    int sort = -1;
                    String savedImage = HNSharedPreference.getSharedPreference(SelectImageMethodActivity.this, "savedImage");
                    String[] savedImageArray = savedImage.split(",");
                    String fName = "";
                    ArrayList<Image> tempArray = new ArrayList<Image>();
                    for(int j = 0; j < savedImageArray.length; j++) {
                        fName = savedImageArray[j].split("&")[0];
                        if(fName.equals(name)) {
                            sort = Integer.parseInt(savedImageArray[j].split("&")[1]);
                        }
                    }

                    Log.d(TAG, "ImageLoaderRunnable =========================");
                    Log.d(TAG, "id = " + id);
                    Log.d(TAG, "name = " + name);
                    Log.d(TAG, "path = " + path);
                    Log.d(TAG, "isSelected = " + isSelected);
                    Log.d(TAG, "========================= ImageLoaderRunnable");

                    if (file.exists()) {
                        mImageItems.add(new Image(id, name, path, isSelected, sort));
                    }
                }

                String savedImage = HNSharedPreference.getSharedPreference(SelectImageMethodActivity.this, "savedImage");
                String[] savedImageArray = savedImage.split(",");

                // Data변경 - Data는 SharedPreference에 저장된 순서대로 보여진다.
                String fName = "";
                ArrayList<Image> tempArray = new ArrayList<Image>();
                for(int j = 0; j < savedImageArray.length; j++) {
                    fName = savedImageArray[j].split("&")[0];
                    for(int k = 0; k <  mImageItems.size(); k++) {
                        if(fName.equals(mImageItems.get(k).name)) {
                            tempArray.add(mImageItems.get(k));
                            break;
                        }
                    }
                }
                mImageItems.clear();
                mImageItems.addAll(tempArray);
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(Constants.ERROR);
            }
            sendMessage(Constants.FETCH_COMPLETED, savedImageSize);
        }
    }

    private void startThread(Runnable runnable) {
        stopThread();
        thread = new Thread(runnable);
        thread.start();
    }

    private void stopThread() {
        if (thread == null || !thread.isAlive()) {
            return;
        }

        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(int what) {
        sendMessage(what, 0);
    }

    private void sendMessage(int what, int arg1) {
        if (handler == null) {
            return;
        }

        Message message = handler.obtainMessage();
        message.what = what;
        message.arg1 = arg1;
        message.sendToTarget();
    }

    @Override
    protected void permissionGranted() {
        sendMessage(Constants.PERMISSION_GRANTED);
    }

    @Override
    protected void hideViews() {
//        progressBar.setVisibility(View.INVISIBLE);
        gridView.setVisibility(View.INVISIBLE);
    }

    /**
     * 권한체크 요청
     *
     * @param requestPermissionId
     */
    private void requestPermission(int requestPermissionId) {
        LogUtil.d(requestPermissionId + " :: permission has NOT been granted. Requesting permission.");

        String permission = "";
        String title = "Request Message";
        String message = "";
        if (requestPermissionId == Constants.REQUEST_CAMERA || requestPermissionId == Constants.REQUEST_SELECT_IMAGE_CAMERA) {
            permission = android.Manifest.permission.CAMERA;
            message = "Allow access camera?";
        } else if (requestPermissionId == Constants.REQUEST_WRITE_EXTERNAL_STORAGE) {
            permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            message = "Allow write external storage?";
        }

        final String finalPermission = permission;

        // 권한체크가 필요한 버전인지 확인 || 권한 체크가 필요한 상태인지 확인
        int permissionCheck = ContextCompat.checkSelfPermission(this, finalPermission);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // 이미 사용자가 권한을 허용하여 앱이 권한을 가지고 있는 상태이므로 하고자 하는 기능 수행.
            if (requestPermissionId == Constants.REQUEST_CAMERA || requestPermissionId == Constants.REQUEST_SELECT_IMAGE_CAMERA) {
                if (mCameraType == 3) {
                    dispatchTakePictureIntent();
                }
            } else if (requestPermissionId == Constants.REQUEST_WRITE_EXTERNAL_STORAGE || requestPermissionId == Constants.REQUEST_SELECT_IMAGE_ALBUM) {
                if (mCameraType == 4) {
                    galleryAddPic();
                }
            }

            return;
        }

        // 사용자에게 지금 이 앱이 권한을 왜 요청하고 있는지 설명하는 페이지를 보여줄 것인지 말것인지 확인
        // 사용자가 권한 설정 허용 팝업에서 거절했을 경우 return true, 다시 묻지 않음을 체크한 경우엔 return false;
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, finalPermission)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(SelectImageMethodActivity.this);
            dialog.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[]{finalPermission}, 0);
                            }
                        }
                    })
                    .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(SelectImageMethodActivity.this, "Cancel permission", Toast.LENGTH_SHORT).show();
                        }
                    }).create().show();
        } else {
            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{finalPermission}, requestPermissionId);
        }
        // END_INCLUDE(camera_permission_request)
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_CAMERA || requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA) {

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mCameraType == 3) {
                    dispatchTakePictureIntent();
                }
                LogUtil.i("SEND_SMS permission has now been granted. Showing preview.");
            } else {
                LogUtil.i("SEND_SMS permission was NOT granted.");
            }
        } else if (requestCode == Constants.REQUEST_WRITE_EXTERNAL_STORAGE || requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM) {
            LogUtil.i("Received response for getting Location Info permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mCameraType == 4) {
                    galleryAddPic();
                }
                LogUtil.i("ACCESS_FINE_LOCATION permission has now been granted. Showing preview.");
            } else {
                LogUtil.i("ACCESS_FINE_LOCATION permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // 사진촬영
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
//                this.grantUriPermission("com.android.camera", photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, Constants.REQUEST_SELECT_IMAGE_CAMERA);
            }
        }
    }

    // 사진 앨범선택
    private void galleryAddPic() {
        Intent intent = new Intent(getApplicationContext(), AlbumSelectActivity.class);
        startActivityForResult(intent, Constants.REQUEST_SELECT_IMAGE_ALBUM);
    }

    // 사진저장
    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        deleteDir(storageDir.getPath());

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        File storageDir = getFilesDir();
        String imageFileName = timeStamp + "_";
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        Log.d("mCurrentPhotoPath", mCurrentPhotoPath);

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "============================================");
        Log.d(TAG, "requestCode = " + requestCode);
        Log.d(TAG, "resultCode = " + resultCode);
        Log.d(TAG, "mCurrentPhotoPath = " + mCurrentPhotoPath);
        Log.d(TAG, "data = " + data);
        Log.d(TAG, "============================================");

        super.onActivityResult(requestCode, resultCode, data);
        try {
            if(data != null) {
                Bundle extras = data.getExtras();
                if(extras != null) {
                    for(String _key : extras.keySet()) {
                        Log.d(TAG, "key=" + _key + " : " + extras.get(_key));
                    }
                }
            }
            if (requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM && resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "////" + mImageItems.size());
                ArrayList<Image> addimages = data.getParcelableArrayListExtra("mImageItems");
                Log.d(TAG, "////" + addimages.size());
                if(addimages.size() > 0) {
                    mIsChanged = true;
                }
                for (int i = 0; i < addimages.size(); i++) {
                    Image image = addimages.get(i);
                    image.sequence = mImageItems.size() + 1;
                    image.isSelected = true;
                    mImageItems.add(image);
                    Log.e(TAG, "//// sequence = " + image.sequence);
                }
                countSelected = mImageItems.size();
                Log.e(TAG, "//// countSelected = " + countSelected);

                // 이미지 저장
                new saveImagesAsyncTask().execute(addimages);
            } else if (requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA) {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "카메라 촬영에 실패했습니다.", Toast.LENGTH_LONG).show();
                    return;
                }
//            Uri currImageURI = data.getData();
//            if(data.getData() == null) {
//                currImageURI = mImageCaptureUri;
//            }
//            currImageURI = mImageCaptureUri;
//            mCurrentPhotoPath = getRealPathFromURI(currImageURI);
                mIsChanged = true;
                Image addImage = new Image(
                        0,
                        mCurrentPhotoPath.substring(mCurrentPhotoPath.lastIndexOf("/") + 1),
                        mCurrentPhotoPath,
                        true,
                        mImageItems.size() + 1);
                ArrayList<Image> addimages = new ArrayList<Image>();
                addimages.add(addImage);
                mImageItems.add(addImage);
                countSelected = mImageItems.size();

                // 이미지 저장
                Log.e(TAG, "//// saveImagesAsyncTask");
                new saveImagesAsyncTask().execute(addimages);
            } else if(requestCode == Constants.REQUEST_EDIT_IMAGE) {
                if(data == null)    return;
//            if (resultCode == RESULT_OK) {
                if(data.hasExtra("isChanged")) {
                    mIsChanged = data.getBooleanExtra("isChanged", false);
                    Log.d(TAG, "Constants.REQUEST_EDIT_IMAGE ******************************************* isChanged = " + data.getBooleanExtra("isChanged", false));
                }

                try {
                    if(data.hasExtra("imgArr")) {
                        mImgArr = new JSONArray(data.getStringExtra("imgArr"));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                mToken = data.getStringExtra("token");
                if(mIsChanged) {
                    String savedImage = HNSharedPreference.getSharedPreference(SelectImageMethodActivity.this, "savedImage");
                    String[] savedImageArray = savedImage.split(",");

                    // Data변경
                    String fName = "";
                    ArrayList<Image> tempArray = new ArrayList<Image>();
                    for(int i = 0; i < savedImageArray.length; i++) {
                        fName = savedImageArray[i].split("&")[0];
                        for(int k = 0; k <  mImageItems.size(); k++) {
                            if(fName.equals(mImageItems.get(k).name)) {
                                tempArray.add(mImageItems.get(k));

                                break;
                            }
                        }
                    }
                    mImageItems.clear();
                    mImageItems.addAll(tempArray);

                    // 파일내용 변경
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Constants.REQUEST_EDIT_IMAGE ******************************************* adapter.notifyDataSetChanged();");
                }
//            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class saveImagesAsyncTask extends AsyncTask<Object, Void, ArrayList<Image>> {
        public String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog.setTitle("알림");
            mProgressDialog.setMessage("처리중입니다.\n잠시만 기다려 주세요.");
            if(!mProgressDialog.isShowing())
                mProgressDialog.show();
            Log.e(TAG, "//// saveImagesAsyncTask onPreExecute");
        }

        @Override
        protected ArrayList<Image> doInBackground(Object... params) {
//            ArrayList<Image> selectedImages = (ArrayList<Image>) params[0];
            ArrayList<Image> selectedImages = getSelected();

            return selectedImages;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList<Image> img) {
//            mImageItems = img;

            // 화면 그리기
            Log.e(TAG, "//// saveImagesAsyncTask onPostExecute");
            sendMessage(Constants.FETCH_COMPLETED, countSelected);
            mProgressDialog.dismiss();
            savedImageSize = mImageItems.size();
            imageCount.setText(savedImageSize + "/8");

            // Close progressdialog
            mProgressDialog.dismiss();
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
            mImgDownloadingCnt++;

            // Create a progressdialog
            mProgressDialog.setTitle("알림");
            mProgressDialog.setMessage("처리중입니다.\n잠시만 기다려 주세요.");
            mProgressDialog.setIndeterminate(false);
            if(!mProgressDialog.isShowing())
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

            File file = new File(mFilePath + fileName);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
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
            String path = file.getAbsolutePath();
            boolean isSelected = true;

            Log.d(TAG, "DownloadImage =========================");
            Log.d(TAG, "id = " + id);
            Log.d(TAG, "name = " + name);
            Log.d(TAG, "path = " + path);
            Log.d(TAG, "isSelected = " + isSelected);
            Log.d(TAG, "========================= DownloadImage");

            mImageItems.add(new Image(id, name, path, isSelected, sort));

            // 저장된 이미지 리스트를 preference에 저장
            String savedImage = HNSharedPreference.getSharedPreference(SelectImageMethodActivity.this, "savedImage");
            savedImage += fileName + "&" + sort + ",";

            HNSharedPreference.putSharedPreference(SelectImageMethodActivity.this, "savedImage", savedImage);

            // 화면 그리기
            sendMessage(Constants.FETCH_COMPLETED, savedImageSize);

            // Close progressdialog
            mImgDownloadingCnt--;
            if(mImgDownloadingCnt == 0) {
                mProgressDialog.dismiss();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri){
        String[] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery( contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}