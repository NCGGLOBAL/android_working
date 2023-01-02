package com.creator.liven.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class Footer extends RelativeLayout implements OnClickListener {
	private ArrayList<Button> mButtons;

	public Footer(Context context, AttributeSet attrs) {
		super(context, attrs);

//		addView(((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
//				R.layout.footer, this, false));
//
//		mButtons = new ArrayList<Button>();
//		mButtons.add((Button) findViewById(R.id.btnFooter01));
//		mButtons.add((Button) findViewById(R.id.btnFooter02));
//		mButtons.add((Button) findViewById(R.id.btnFooter03));
//		mButtons.add((Button) findViewById(R.id.btnFooter04));
//		mButtons.add((Button) findViewById(R.id.btnFooter05));
//
//		for (int i = 0; i < mButtons.size(); i++) {
//			mButtons.get(i).setOnClickListener(this);
//		}
	}

	@Override
	public void onClick(View v) {
		try {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}