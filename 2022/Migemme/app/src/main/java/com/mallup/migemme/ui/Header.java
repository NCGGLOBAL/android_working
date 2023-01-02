package com.mallup.migemme.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mallup.migemme.R;

public class Header extends RelativeLayout implements OnClickListener {

	public static final String LOGO = "logo";

	private static final String BUTTON_TEXT_BACK = "뒤로";

	private View vewLogoTitle;
	private LinearLayout llHeaderLeft;
	private TextView tvHeaderLeft;
	private Button btnHeaderRight;
	private TextView txtHeaderTitle;

	private ButtonType mLeftButtonType;
	private ButtonType mRightButtonType;

	public enum ButtonType {
		NONE, LOGO, BACK, SETTING, LOGIN, LOGOUT
	}

	public Header(Context context, AttributeSet attrs) {
		super(context, attrs);

		addView(((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.header, this, false));

		vewLogoTitle = findViewById(R.id.vewLogoTitle);
		llHeaderLeft = (LinearLayout)findViewById(R.id.llHeaderLeft);
		tvHeaderLeft = (TextView) findViewById(R.id.tvHeaderLeft);
		btnHeaderRight = (Button) findViewById(R.id.btnHeaderRight);
		txtHeaderTitle = (TextView) findViewById(R.id.txtHeaderTitle);
	}

	public void setTitle(String title) {
		vewLogoTitle.setVisibility(View.GONE);
		txtHeaderTitle.setVisibility(View.VISIBLE);
		txtHeaderTitle.setText(title);
	}

	public void setTitle(ButtonType buttonType) {
		vewLogoTitle.setVisibility(View.VISIBLE);
		txtHeaderTitle.setVisibility(View.GONE);
	}

	public void setLeftButton(ButtonType buttonType) {
		setLeftButton(buttonType, this);
	}


	/**
	 * 
	 * set header left button.
	 * 
	 * @param leftButtonText
	 *            text for left button. if null or empty, left button will not be displayed. if {@link #LOGO} logo will
	 *            be displayed.
	 * @param onClickListener
	 *            set OnclickListener to left button.
	 */
	public void setLeftButton(String leftButtonText, OnClickListener onClickListener) {
		llHeaderLeft.setOnClickListener(onClickListener);
		if (leftButtonText == null || leftButtonText.trim().equals("")) {
			llHeaderLeft.setVisibility(View.GONE);
		} else {
			llHeaderLeft.setVisibility(View.VISIBLE);
			tvHeaderLeft.setText(leftButtonText);
		}
		
		if(BUTTON_TEXT_BACK.equals(leftButtonText)) {
			tvHeaderLeft.setText("");
		}
	}
	
	public void setLeftButton(ButtonType leftButtonType, OnClickListener onClickListener) {
		mLeftButtonType = leftButtonType;
		switch (leftButtonType) {
		case BACK :
			llHeaderLeft.setVisibility(View.VISIBLE);
			llHeaderLeft.setOnClickListener(onClickListener);
			break;
		default:
			break;
		}
	}
	
	/**
	 * set header right button.
	 * 
	 * @param rightButtonText
	 *            text for right button. if null or empty, right button will not be displayed.
	 * 
	 * @param onClickListener
	 *            set OnclickListener to right button.
	 */
	public void setRightButton(String rightButtonText, OnClickListener onClickListener) {
		btnHeaderRight.setOnClickListener(onClickListener);

		if (rightButtonText == null || rightButtonText.trim().equals("")) {
			btnHeaderRight.setVisibility(View.GONE);
		} else {
			btnHeaderRight.setVisibility(View.VISIBLE);
			btnHeaderRight.setText(rightButtonText);
		}
	}
	
	public void setRightButton(ButtonType rightButtonType, OnClickListener onClickListener) {
		mRightButtonType = rightButtonType;
		switch (rightButtonType) {
		case SETTING:
			btnHeaderRight.setVisibility(View.VISIBLE);
			btnHeaderRight.setOnClickListener(onClickListener);
			break;
		default:
			break;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.llHeaderLeft:
			if (mLeftButtonType == ButtonType.BACK) {
			}
			break;
		case R.id.btnHeaderRight:
			if (mRightButtonType == ButtonType.SETTING) {
					
			}
			break;
		}
	}
}