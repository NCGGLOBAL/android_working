package com.creator.hotsourcelive;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.creator.hotsourcelive.delegator.HNSharedPreference;
import com.creator.hotsourcelive.helpers.Constants;
import com.creator.hotsourcelive.models.Image;
import com.creator.hotsourcelive.util.BitmapUtil;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;

import sun.bob.dndgridview.DNDAdapter;
import sun.bob.dndgridview.DNDGridView;
import sun.bob.dndgridview.DNDViewHolder;

/**
 * Created by skcrackers on 12/18/17.
 */

public class ImageEditChangeOrder extends HelperActivity {
    // ============================= Album =============================
    private ArrayList<Image> images;
    private String album;

    private TextView errorDisplay;
    private TextView imageCount;

    //    private CustomImageSelectAdapter adapter;
    private DNDGridView gridView;
    private DNDAdapter adapter;

    private ActionBar actionBar;
    private ActionMode actionMode;
    private int countSelected;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private final String[] projection = new String[]{ MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA };
    // ============================= Album =============================

    private static final String TAG = ImageEditChangeOrder.class.getName();

    private String mToken = "";     // 이미지 등록 Token
    private JSONArray mImgArr = null;     // 이미지 등록 Token

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_image_edit_change_order);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // 서버에서 내려온 이미지 경로
            Intent intent = getIntent();
            if(intent.hasExtra("imgArr") && intent.hasExtra("token")) {
                Log.e("SeongKwon", "=============================" + intent.getStringExtra("imgArr"));
                Log.e("SeongKwon", "=============================" + intent.getStringExtra("token"));
                mImgArr = new JSONArray(intent.getStringExtra("imgArr"));
                mToken = intent.getStringExtra("token");
            }

            actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);

                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(getResources().getString(R.string.label_picture_change));
            }

            TextView textview_header_right = (TextView) findViewById(R.id.textview_header_right);
            textview_header_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 완료
                    Intent intent = new Intent();
                    if(mImgArr != null) {
                        intent.putExtra("imgArr", mImgArr.toString());
                        intent.putExtra("token", mToken);
                    } else {
                        intent.putParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES, images);
                    }
                    // 변경내역 있음
                    if(!adapter.getChangedImageList().equals("")) {
                        HNSharedPreference.putSharedPreference(ImageEditChangeOrder.this, "savedImage", adapter.getChangedImageList());
                        intent.putExtra("isChanged", true);
                    } else {
                        intent.putExtra("isChanged", false);
                    }
                    setResult(RESULT_OK, intent);
                    ImageEditChangeOrder.this.finish();
                }
            });
            TextView imageCount = (TextView) findViewById(R.id.textview_header_image_count);
            imageCount.setVisibility(View.GONE);

            gridView = (DNDGridView) findViewById(R.id.gridview);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.PERMISSION_GRANTED: {
                        Log.d("SeongKwon", "Constants.PERMISSION_GRANTED");
                        loadImages();
                        break;
                    }

                    case Constants.FETCH_STARTED: {
                        Log.d("SeongKwon", "Constants.FETCH_STARTED");
                        gridView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case Constants.FETCH_COMPLETED: {
                        Log.d("SeongKwon", "Constants.FETCH_COMPLETED");
                        /*
                        If adapter is null, this implies that the loaded images will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */
                        if (adapter == null) {
                            adapter = new DNDAdapter(getApplicationContext(), R.layout.grid_view_item_image_select) {
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    try {
                                        Log.d("SeongKwon", "DNDAdapter - getView");

                                        DNDViewHolder viewHolder;
                                        if (convertView == null) {
                                            convertView = View.inflate(getContext(), R.layout.grid_view_item_image_select, null);
                                            viewHolder = new DNDViewHolder(position);
                                            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view_image_select);
                                            viewHolder.textView = (TextView) convertView.findViewById(R.id.image_view_image_sequence);
                                            viewHolder.view = convertView.findViewById(R.id.view_alpha);

                                            convertView.setTag(viewHolder);
                                        } else {
                                            viewHolder = (DNDViewHolder) convertView.getTag();
                                        }
                                        viewHolder.imageView.getLayoutParams().width = size;
                                        viewHolder.imageView.getLayoutParams().height = size;

                                        viewHolder.view.getLayoutParams().width = size;
                                        viewHolder.view.getLayoutParams().height = size;

                                        viewHolder.textView.setVisibility(View.VISIBLE);
                                        viewHolder.textView.setText((position + 1) + "");
                                        viewHolder.view.setAlpha(0.0f);

                                        Glide.with(ImageEditChangeOrder.this)
                                                .load(images.get(position).path)
//                                                .transform(new BitmapUtil(ImageEditChangeOrder.this, BitmapUtil.GetExifOrientation(images.get(position).path)))
                                                .transform(new BitmapUtil(ImageEditChangeOrder.this, 0))
                                                .placeholder(R.drawable.image_placeholder).into(viewHolder.imageView);
                                        setUpDragNDrop(position, convertView);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return convertView;
                                }
                            };
                            adapter.setCustomArray(images);

                            gridView.setAdapter(adapter);
                            gridView.setVisibility(View.VISIBLE);
                            orientationBasedUI(getResources().getConfiguration().orientation);
                        } else {
                            adapter.notifyDataSetChanged();
                            /*
                            Some selected images may have been deleted
                            hence update action mode title
                             */
                            if (actionMode != null) {
                                countSelected = msg.arg1;
                                actionMode.setTitle(countSelected + " " + getString(R.string.selected));
                            }
                        }
                        break;
                    }

                    case Constants.ERROR: {
                        errorDisplay.setVisibility(View.VISIBLE);
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

        stopThread();

        getContentResolver().unregisterContentObserver(observer);
        observer = null;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        images = null;
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

    private void loadImages() {
        startThread(new ImageEditChangeOrder.ImageLoaderRunnable());
    }

    private class ImageLoaderRunnable implements Runnable {
        @Override
        public void run() {
//            android.os.Process.setThreadPriority(java.lang.Process.THREAD_PRIORITY_BACKGROUND);
            /*
            If the adapter is null, this is first time this activity's view is
            being shown, hence send FETCH_STARTED message to show progress bar
            while images are loaded from phone
             */
            if (adapter == null) {
                sendMessage(Constants.FETCH_STARTED);
            }

            int tempCountSelected = 0;
            ArrayList<Image> temp = new ArrayList<Image>();

            File file = new File(getFilesDir() + "/");
            File[] flist = file.listFiles();

            String savedImage = HNSharedPreference.getSharedPreference(ImageEditChangeOrder.this, "savedImage");
            Log.d("SeongKwon", "savedImage = " + savedImage);
            String[] savedImageArray = savedImage.split(",");
            tempCountSelected = savedImageArray.length;
            for (int i = 0; i < tempCountSelected; i++) {
                temp.add(i, null);
            }

            Log.d("SeongKwon", "*************************************************");
            for (int i = 0; i < tempCountSelected; i++) {
                String imageItem = savedImageArray[i];
                Log.d("SeongKwon", "imageItem = " + imageItem);

                int delemeter = imageItem.indexOf("&");
                Log.d("SeongKwon", "imageArray[0] = " + imageItem.substring(0, delemeter));
                Log.d("SeongKwon", "imageArray[1] = " + imageItem.substring(delemeter + 1, imageItem.length()));

                long id = -1;
                String name = imageItem.substring(0, delemeter);
                String path = file.getAbsolutePath() + "/" + name;
                boolean isSelected = false;
                int sequence = Integer.parseInt(imageItem.substring(delemeter + 1, imageItem.length()));

                Log.d("SeongKwon", "=========================");
                Log.d("SeongKwon", "id = " + id);
                Log.d("SeongKwon", "name = " + name);
                Log.d("SeongKwon", "path = " + path);
                Log.d("SeongKwon", "isSelected = " + isSelected);
                Log.d("SeongKwon", "sequencesequence = " + sequence);
                Log.d("SeongKwon", "=========================");


                if (file.exists()) {
                    temp.set(sequence - 1, new Image(id, name, path, isSelected, sequence));
                }
            }
            Log.d("SeongKwon", "*************************************************");

            if (images == null) {
                images = new ArrayList<>();
            }
            images.clear();
            images.addAll(temp);

            sendMessage(Constants.FETCH_COMPLETED, tempCountSelected);
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
}