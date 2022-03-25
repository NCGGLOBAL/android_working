package com.flexme.shop.android;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.flexme.shop.android.R;
import com.flexme.shop.android.adapters.CustomAlbumSelectAdapter;
import com.flexme.shop.android.helpers.Constants;
import com.flexme.shop.android.models.Album;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Darshan on 4/14/2015.
 */
public class AlbumSelectActivity extends HelperActivity {
    private ArrayList<Album> albums;

    private TextView errorDisplay;

    private ProgressBar progressBar;

    private ListView listView;
    private CustomAlbumSelectAdapter adapter;

    private ActionBar actionBar;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private HashMap<String, Long> albumCount = new HashMap<String, Long>();

    private final String[] projection = new String[]{
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_select);
        setView(findViewById(R.id.layout_album_select));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("폴더선택");

            TextView textview_header_right = (TextView)findViewById(R.id.textview_header_right);
            textview_header_right.setVisibility(View.GONE);
        }

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        Constants.limit = intent.getIntExtra(Constants.INTENT_EXTRA_LIMIT, Constants.DEFAULT_LIMIT);

        errorDisplay = (TextView) findViewById(R.id.text_view_error);
        errorDisplay.setVisibility(View.INVISIBLE);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar_album_select);
        listView = (ListView) findViewById(R.id.list_view_album_select);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ImageSelectActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_ALBUM, albums.get(position).name);
                startActivityForResult(intent, Constants.REQUEST_CODE);
            }
        });

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

    @Override
    protected void onStart() {
        super.onStart();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.PERMISSION_GRANTED: {
                        loadAlbums();
                        break;
                    }

                    case Constants.FETCH_STARTED: {
                        progressBar.setVisibility(View.VISIBLE);
//                        gridView.setVisibility(View.INVISIBLE);
                        listView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case Constants.FETCH_COMPLETED: {
                        if (adapter == null) {
                            adapter = new CustomAlbumSelectAdapter(getApplicationContext(), albums, albumCount);
//                            gridView.setAdapter(adapter);
                            listView.setAdapter(adapter);

                            progressBar.setVisibility(View.INVISIBLE);
//                            gridView.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.VISIBLE);
                            orientationBasedUI(getResources().getConfiguration().orientation);

                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        break;
                    }

                    case Constants.ERROR: {
                        progressBar.setVisibility(View.INVISIBLE);
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
            public void onChange(boolean selfChange, Uri uri) {
                loadAlbums();
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
        albums = null;
        if (adapter != null) {
            adapter.releaseResources();
        }
//        gridView.setOnItemClickListener(null);
        listView.setOnItemClickListener(null);
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

//        if (adapter != null) {
//            int size = orientation == Configuration.ORIENTATION_PORTRAIT ? metrics.widthPixels / 2 : metrics.widthPixels / 4;
//            adapter.setLayoutParams(size);
//        }
//        gridView.setNumColumns(orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
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

    private void loadAlbums() {
        startThread(new AlbumLoaderRunnable());
    }

    private class AlbumLoaderRunnable implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            if (adapter == null) {
                sendMessage(Constants.FETCH_STARTED);
            }

            Cursor cursor = getApplicationContext().getContentResolver()
                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED);

            if (cursor == null) {
                sendMessage(Constants.ERROR);
                return;
            }

            ArrayList<Album> temp = new ArrayList<>(cursor.getCount());
            HashSet<Long> albumSet = new HashSet<>();
            File file;

            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    long albumId = cursor.getLong(cursor.getColumnIndex(projection[0]));
                    String album = cursor.getString(cursor.getColumnIndex(projection[1]));
                    String image = cursor.getString(cursor.getColumnIndex(projection[2]));

                    // 전체앨범표시
                    if(temp.size() == 0) {
                        Album allAlbum = new Album(album, image);
                        allAlbum.name = "전체 보기";
                        albumCount.put(allAlbum.name, Long.parseLong(cursor.getCount() + ""));
                        temp.add(allAlbum);
                    }

                    // 앨범별 카운트
                    if(albumCount.containsKey(album)) {
                        albumCount.put(album, albumCount.get(album) + 1);
                    }

                    if (!albumSet.contains(albumId)) {
                        albumCount.put(album, 1L);

                        /*
                        It may happen that some image file paths are still present in cache,
                        though image file does not exist. These last as long as media
                        scanner is not run again. To avoid get such image file paths, check
                        if image file exists.
                         */
                        file = new File(image);
                        if (file.exists()) {
                            temp.add(new Album(album, image));
                            albumSet.add(albumId);
                        }
                    }

                } while (cursor.moveToPrevious());
            }
            cursor.close();

            if (albums == null) {
                albums = new ArrayList<>();
            }
            albums.clear();
            albums.addAll(temp);

            sendMessage(Constants.FETCH_COMPLETED);
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
        if (handler == null) {
            return;
        }

        Message message = handler.obtainMessage();
        message.what = what;
        message.sendToTarget();
    }

    @Override
    protected void permissionGranted() {
        Message message = handler.obtainMessage();
        message.what = Constants.PERMISSION_GRANTED;
        message.sendToTarget();
    }

    @Override
    protected void hideViews() {
        progressBar.setVisibility(View.INVISIBLE);
//        gridView.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.INVISIBLE);
    }
}
