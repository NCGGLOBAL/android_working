/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nechingu.benecia.youtube;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.nechingu.benecia.MainActivity;
import com.nechingu.benecia.R;
import com.nechingu.benecia.youtube.antmedia.liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import com.nechingu.benecia.youtube.util.EventData;
import com.nechingu.benecia.youtube.util.NetworkSingleton;
import com.nechingu.benecia.youtube.util.Utils;
import com.nechingu.benecia.youtube.util.YouTubeApi;

import java.util.Arrays;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         Main activity class which handles authorization and intents.
 */
public class YoutubeActivity extends Activity implements EventsListFragment.Callbacks {
    public static String RTMP_BASE_URL = "";       // SeongKwon Youtube test

    public static final String ACCOUNT_KEY = "accountName";
    public static final String APP_NAME = "Benecia";
    public static boolean mModifyResolution = false;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_GMS_ERROR_DIALOG = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int REQUEST_STREAMER = 4;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    public static GoogleAccountCredential credential;
    private String mChosenAccountName;
    private ImageLoader mImageLoader;
    private EventsListFragment mEventsListFragment;

    public static EventData mSelectedEventData;
    private boolean mEndEvent = false;

    private String mLiveTitle = ""; // 방송 타이틀
    private String mLiveDesc = "";  // 방송 설명글

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube);

        Intent intent = getIntent();
        if(intent.hasExtra("liveTitle") && intent.hasExtra("liveDesc")) {
            mLiveTitle = intent.getStringExtra("liveTitle");     // 방송 타이틀
            mLiveDesc =  intent.getStringExtra("liveDesc");      // 방송 설명글
        }

        ensureLoader();
        credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(Utils.SCOPES));
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());

        if (savedInstanceState != null) {
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
        } else {
            loadAccount();
        }

        credential.setSelectedAccountName(mChosenAccountName);
        mEventsListFragment = (EventsListFragment) getFragmentManager().findFragmentById(R.id.list_fragment);
    }

    public void startStreaming(EventData event) {
        mSelectedEventData = event;

        String broadcastId = event.getId();

//        new StartEventTask().execute(broadcastId);

        Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
        intent.putExtra(YouTubeApi.RTMP_URL_KEY, event.getIngestionAddress());
        intent.putExtra(YouTubeApi.RTMP_URL_KEY, event.getIngestionAddress());
        intent.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId);
        intent.putExtra("title", event.getTitle());
        intent.putExtra("thumbUri", event.getThumbUri());
        intent.putExtra("watchUri", event.getWatchUri());
        startActivityForResult(intent, REQUEST_STREAMER);
    }

    private void ensureLoader() {
        if (mImageLoader == null) {
            // Get the ImageLoader through your singleton class.
            mImageLoader = NetworkSingleton.getInstance(this).getImageLoader();
        }
    }

    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
        invalidateOptionsMenu();
    }

    private void saveAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).apply();
    }

    private void loadData() {
        if (mChosenAccountName == null) {
            return;
        }

        if(!mEndEvent) {
//            new CreateLiveEventTask().execute();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                loadData();
                break;
            case R.id.menu_accounts:
                chooseAccount();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GMS_ERROR_DIALOG:
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        credential.setSelectedAccountName(accountName);
                        saveAccount();
                    }
                }
                break;
            case REQUEST_STREAMER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String broadcastId = data.getStringExtra(YouTubeApi.BROADCAST_ID_KEY);
                    if (broadcastId != null) {
                        mEndEvent = true;
//                        new EndEventTask().execute(broadcastId);

                        Intent BrIntent = new Intent(MainActivity.BROADCAST_MESSAGE);
                        BrIntent.putExtra("liveUrl", mSelectedEventData.getWatchUri());
                        BrIntent.putExtra("liveStatus", "0");   // 방송종료
                        sendBroadcast(BrIntent);
                    }
                }
                break;

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACCOUNT_KEY, mChosenAccountName);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConnected(String connectedAccountName) {
        // Make API requests only when the user has successfully signed in.
        loadData();
    }

    public void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode, YoutubeActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // Showing status
        if (connectionStatusCode == ConnectionResult.SUCCESS)
            Log.d("SeongKwon", "Google Play Services are available");
        else {
            Log.d("SeongKwon", "Google Play Services are not available");
        }

        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount();
        }
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public ImageLoader onGetImageLoader() {
        ensureLoader();
        return mImageLoader;
    }

    @Override
    public void onEventSelected(EventData liveBroadcast) {
        startStreaming(liveBroadcast);
    }

//    private class GetLiveEventsTask extends
//            AsyncTask<Void, Void, List<EventData>> {
//        private ProgressDialog progressDialog;
//
//        @Override
//        protected void onPreExecute() {
//            progressDialog = ProgressDialog.show(YoutubeActivity.this, null,
//                    getResources().getText(R.string.loadingEvents), true);
//        }
//
//        @Override
//        protected List<EventData> doInBackground(Void... params) {
//            YouTube youtube = new YouTube.Builder(transport, jsonFactory, credential).setApplicationName(APP_NAME).build();
//            try {
////                return YouTubeApi.getLiveEvents(youtube);
//                return YouTubeApi.getLiveEvents(youtube, "");
//            } catch (UserRecoverableAuthIOException e) {
//                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
//            } catch (IOException e) {
//                Log.e(YoutubeActivity.APP_NAME, "", e);
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(
//                List<EventData> fetchedEvents) {
//            if (fetchedEvents == null) {
//                progressDialog.dismiss();
//                return;
//            }
//
//            mEventsListFragment.setEvents(fetchedEvents);
//            progressDialog.dismiss();
//        }
//    }

//    private class CreateLiveEventTask extends AsyncTask<Void, Void, List<EventData>> {
//        private ProgressDialog progressDialog;
//        private String mBroadcastId;
//
//        @Override
//        protected void onPreExecute() {
//            progressDialog = ProgressDialog.show(YoutubeActivity.this, null, getResources().getText(R.string.creatingEvent), true);
//        }
//
//        @Override
//        protected List<EventData> doInBackground(Void... params) {
//            YouTube youtube = new YouTube.Builder(transport, jsonFactory, credential).setApplicationName(APP_NAME).build();
//            try {
//                Log.e("SeongKwon", "CreateLiveEventTask doInBackground ================ mLiveTitle : " + mLiveTitle);
//                Log.e("SeongKwon", "CreateLiveEventTask doInBackground ================ mLiveDesc : " + mLiveDesc);
//
//                mBroadcastId = YouTubeApi.createLiveEvent(youtube, mLiveDesc, mLiveTitle);
//
//                return YouTubeApi.getLiveEvents(youtube, mBroadcastId);
//            } catch (UserRecoverableAuthIOException e) {
//                return null;
//                // 권한 허용을 해도 별도로 신청을 해줘야함. 로그인 페이지로 이동
//                // The user is not enabled for live streaming.
////                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
//            } catch (IOException e) {
//                Log.e(YoutubeActivity.APP_NAME, "", e);
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(List<EventData> fetchedEvents) {
//            // Event List
//            if(fetchedEvents != null) {
//                if (fetchedEvents.size() > 0) {
//                    for (int idx = 0; idx < fetchedEvents.size(); idx++) {
//                        Log.e("SeongKwon", "CreateLiveEventTask onPostExecute ================ fetchedEvents List");
//                        Log.e("SeongKwon", "getId = " + fetchedEvents.get(idx).getId());
//                        Log.e("SeongKwon", "getIngestionAddress = " + fetchedEvents.get(idx).getIngestionAddress());
//                        Log.e("SeongKwon", "getThumbUri = " + fetchedEvents.get(idx).getThumbUri());
//                        Log.e("SeongKwon", "getTitle = " + fetchedEvents.get(idx).getTitle());
//                        Log.e("SeongKwon", "getWatchUri" + fetchedEvents.get(idx).getWatchUri());
//                        if (mBroadcastId.equals(fetchedEvents.get(idx).getId())) {
//                            Log.e("SeongKwon", "새로 생성된 stream");
//                            mSelectedEventData = fetchedEvents.get(idx);
//                            RTMP_BASE_URL = mSelectedEventData.getIngestionAddress();
//                        }
//                        Log.e("SeongKwon", "CreateLiveEventTask onPostExecute ================ fetchedEvents List");
//                    }
//
//                    startStreaming(mSelectedEventData);
//                    progressDialog.dismiss();
//                } else {
//                    progressDialog.dismiss();
//
//                    AlertUtil.showConfirmDialog(YoutubeActivity.this, getString(R.string.alert_title), "You can not create an event with this account!");
//                }
//            } else {
//                progressDialog.dismiss();
//
//                AlertDialog.Builder alertbox = new AlertDialog.Builder(YoutubeActivity.this);
//                alertbox.setTitle(getString(R.string.alert_title));
//                alertbox.setCancelable(false);
//                alertbox.setMessage("유튜브라이브 승인계정이 아닙니다. 승인신청해주세요. 신청후 24시간이후 승인완료됩니다. \n" +
//                        "확인클릭시 승인신청안내페이지로 이동됩니다.");
//                alertbox.setNeutralButton(YoutubeActivity.this.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        Intent intent = new Intent();
//                        intent.putExtra("redirect","/m/liveinfo.asp");
//                        setResult(RESULT_OK, intent);
//                        YoutubeActivity.this.finish();
//                    }
//                });
//                alertbox.show();
//            }
//        }
//    }

//    private class EndEventTask extends AsyncTask<String, Void, Void> {
//        private ProgressDialog progressDialog;
//
//        @Override
//        protected void onPreExecute() {
//            progressDialog = ProgressDialog.show(YoutubeActivity.this, null, getResources().getText(R.string.endingEvent), true);
//        }
//
//        @Override
//        protected Void doInBackground(String... params) {
//            YouTube youtube = new YouTube.Builder(transport, jsonFactory, credential).setApplicationName(APP_NAME).build();
//            try {
//                if (params.length >= 1) {
//                    YouTubeApi.endEvent(youtube, params[0]);
//                }
//            } catch (UserRecoverableAuthIOException e) {
//                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
//            } catch (IOException e) {
//                Log.e(YoutubeActivity.APP_NAME, "", e);
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void param) {
//            progressDialog.dismiss();
//
//            YoutubeActivity.this.finish();
//        }
//    }
}