package com.creator.metaceleb.common;

import android.app.Activity;
import android.widget.Toast;

import com.creator.metaceleb.R;

/**
 * Created by skcrackers on 5/27/16.
 */
public class BackPressCloseHandler {
    private long mBackKeyPressedTime = 0;
    private Toast mToast;

    private Activity mActivity;

    public BackPressCloseHandler(Activity context) {
        this.mActivity = context;
    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > mBackKeyPressedTime + 2000) {
            mBackKeyPressedTime = System.currentTimeMillis();
            mToast = Toast.makeText(mActivity, mActivity.getResources().getString(R.string.exit), Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        if (System.currentTimeMillis() <= mBackKeyPressedTime + 2000) {
            mActivity.finish();
            mToast.cancel();
        }
    }
}