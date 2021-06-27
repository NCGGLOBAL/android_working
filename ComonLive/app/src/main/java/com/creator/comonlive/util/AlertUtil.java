package com.creator.comonlive.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.creator.comonlive.R;

/**
 * Created by skcrackers on 5/27/16.
 */
public class AlertUtil {
    public static void showConfirmDialog(final Activity activity, final String title, final String message) {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(activity);
        alertbox.setTitle(title);
        alertbox.setCancelable(false);
        alertbox.setMessage(message);
        alertbox.setNeutralButton(activity.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        });

        alertbox.show();
    }
}
