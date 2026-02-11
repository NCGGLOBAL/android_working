package com.creator.hotdeallive2.service

import android.app.*
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.creator.hotdeallive2.common.HNApplication
import com.creator.hotdeallive2.delegator.HNCommTran
import com.creator.hotdeallive2.delegator.HNCommTranInterface
import com.creator.hotdeallive2.util.*
import org.json.JSONObject

/**
 * Created by skcrackers on 2/19/18.
 */
class MyLocation : Service() {
    private var mLocationManager: LocationManager? = null
    private var mHNCommTran: HNCommTran? = null

    inner class LocationListener(provider: String) : android.location.LocationListener {
        var mLastLocation: Location

        init {
            Log.e(TAG, "LocationListener $provider")
            mLastLocation = Location(provider)
        }

        override fun onLocationChanged(location: Location) {
            Log.e(TAG, "onLocationChanged: $location")
            Log.e(TAG, "latitude: " + location.latitude)
            Log.e(TAG, "longitude: " + location.longitude)
            mLastLocation.set(location)
            Thread(Runnable {
                try {
                    val jObj = JSONObject()
                    mHNCommTran = HNCommTran(object : HNCommTranInterface {
                        override fun recvMsg(tranCode: String?, params: String) {
                            LogUtil.e("recv location_update1 : $tranCode : $params")
                            if (tranCode!!.startsWith(HNApplication.URL + "/member/location_update.asp")) {
                                LogUtil.e("recv location_update2 : $tranCode : $params")
                            }
                        }
                    })
                    mHNCommTran!!.sendMsg(
                        HNApplication.Companion.URL + "/member/location_update.asp?latitude=" + mLastLocation.latitude +
                                "&longitude=" + mLastLocation.longitude + "&id=&DeviceId=" + HNApplication.Companion.mDeviceId,
                        jObj
                    )
                    return@Runnable
                } catch (localException: Exception) {
                    localException.printStackTrace()
                }
            }).start()
        }

        override fun onProviderDisabled(provider: String) {
            Log.e(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.e(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.e(TAG, "onStatusChanged: $provider")
        }
    }

    var mLocationListeners = arrayOf<LocationListener>(
        LocationListener(LocationManager.GPS_PROVIDER),
        LocationListener(LocationManager.NETWORK_PROVIDER)
    )

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
        initializeLocationManager()
        try {
            mLocationManager!!.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL.toLong(), LOCATION_DISTANCE,
                mLocationListeners[1]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "network provider does not exist, " + ex.message)
        }
        try {
            mLocationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, LOCATION_INTERVAL.toLong(), LOCATION_DISTANCE,
                mLocationListeners[0]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "gps provider does not exist " + ex.message)
        }
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        super.onDestroy()
        if (mLocationManager != null) {
            for (i in mLocationListeners.indices) {
                try {
                    mLocationManager!!.removeUpdates(mLocationListeners[i])
                } catch (ex: Exception) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex)
                }
            }
        }
    }

    private fun initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager")
        if (mLocationManager == null) {
            mLocationManager =
                applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        }
    }

    companion object {
        private const val TAG = "SeongKwon"
        private const val LOCATION_INTERVAL = 30000
        private const val LOCATION_DISTANCE = 10f
    }
}