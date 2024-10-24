/* Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nechingu.benecia.youtube.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.Scopes;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTubeScopes;
import com.nechingu.benecia.R;
import com.nechingu.benecia.youtube.YoutubeActivity;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    public static final String[] SCOPES = {Scopes.PROFILE, YouTubeScopes.YOUTUBE};
    private static Camera camera;

    private Utils() {
    }

    public static Camera getCamera(int cameraType) {
        if (camera == null) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == cameraType) {
                    try {
                        camera = Camera.open(i);
                    } catch (RuntimeException e) {
                        Log.e(YoutubeActivity.APP_NAME, String.format("Couldn't open camera type '%d'.", cameraType), e);
                    }
                }
            }

            if (camera == null) {
                camera = Camera.open();
            }
        }

        return camera;
    }

    public static void releaseCamera() {
        camera.release();
        camera = null;
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Logs the given throwable and shows an error alert dialog with its message.
     *
     * @param activity activity
     * @param tag      log tag to use
     * @param t        throwable to log and show
     */
    public static void logAndShow(Activity activity, String tag, Throwable t) {
        Log.e(tag, "Error", t);
        String message = t.getMessage();
        if (t instanceof GoogleJsonResponseException) {
            GoogleJsonError details = ((GoogleJsonResponseException) t).getDetails();
            if (details != null) {
                message = details.getMessage();
            }
        } else if (t.getCause() instanceof GoogleAuthException) {
            message = ((GoogleAuthException) t.getCause()).getMessage();
        }
        showError(activity, message);
    }

    /**
     * Logs the given message and shows an error alert dialog with it.
     *
     * @param activity activity
     * @param tag      log tag to use
     * @param message  message to log and show or {@code null} for none
     */
    public static void logAndShowError(Activity activity, String tag, String message) {
        String errorMessage = getErrorMessage(activity, message);
        Log.e(tag, errorMessage);
        showErrorInternal(activity, errorMessage);
    }

    /**
     * Shows an error alert dialog with the given message.
     *
     * @param activity activity
     * @param message  message to show or {@code null} for none
     */
    public static void showError(Activity activity, String message) {
        String errorMessage = getErrorMessage(activity, message);
        showErrorInternal(activity, errorMessage);
    }

    private static void showErrorInternal(final Activity activity, final String errorMessage) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String getErrorMessage(Activity activity, String message) {
        Resources resources = activity.getResources();
        if (message == null) {
            return resources.getString(R.string.error);
        }
        return resources.getString(R.string.error_format, message);
    }

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
     *
     * @see Activity#onRequestPermissionsResult(int, String[], int[])
     */
    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


}
