package com.mallup.eduwelfare.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.mallup.eduwelfare.common.HNApplication;
import com.mallup.eduwelfare.delegator.HNCommTran;
import com.mallup.eduwelfare.delegator.HNCommTranInterface;
import com.mallup.eduwelfare.util.LogUtil;

import org.json.JSONObject;

/**
 * Created by skcrackers on 2/19/18.
 */

public class MyLocation extends Service
{
    private static final String TAG = "SeongKwon";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 30000;
    private static final float LOCATION_DISTANCE = 10f;
    private HNCommTran mHNCommTran;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            Log.e(TAG, "latitude: " + location.getLatitude());
            Log.e(TAG, "longitude: " + location.getLongitude());

            mLastLocation.set(location);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        JSONObject jObj = new JSONObject();
                        mHNCommTran = new HNCommTran(new HNCommTranInterface() {
                            @Override
                            public void recvMsg(String tranCode, String params) {
                                LogUtil.e("recv location_update1 : " + tranCode + " : " + params);
                                if (tranCode.startsWith(HNApplication.URL + "/member/location_update.asp")) {
                                    LogUtil.e("recv location_update2 : " + tranCode + " : " + params);
                                }
                            }
                        });
                        mHNCommTran.sendMsg(HNApplication.URL + "/member/location_update.asp?latitude=" + mLastLocation.getLatitude() +
                                "&longitude=" + mLastLocation.getLongitude() + "&id=&DeviceId=" + HNApplication.mDeviceId, jObj);
                        return;
                    } catch (Exception localException) {
                        localException.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}