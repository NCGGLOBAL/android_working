package kr.fanbridge.podoaltv.util;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;

import kr.fanbridge.podoaltv.R;


/**
 * Created by skcrackers on 5/27/16.
 */
public class ProgressUtil extends Dialog {
    public ProgressUtil(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_progress_util);
    }
}