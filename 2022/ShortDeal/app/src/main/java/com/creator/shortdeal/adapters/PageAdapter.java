package com.creator.shortdeal.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.creator.shortdeal.models.Image;
import com.creator.shortdeal.util.BitmapUtil;

import java.util.ArrayList;

public class PageAdapter extends PagerAdapter {
	Context context;
	private ArrayList<Image> mSelectedImgages;

	public PageAdapter(Context context, ArrayList<Image> selectedImages){
		this.context=context;

		this.mSelectedImgages = selectedImages;
	}

	@Override
	public int getCount() {
		return mSelectedImgages.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((ImageView) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		ImageView imageView = new ImageView(context);
//		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		imageView.setImageBitmap(getImageBitmap(mSelectedImgages.get(position).path));

		Log.d("SeongKwon", (mSelectedImgages.get(position).sequence - 1) + "//" + getCount() + "//");
		((ViewPager) container).addView(imageView, 0);
		return imageView;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((ImageView) object);
	}

	public Bitmap getImageBitmap(String path) {
		Bitmap resized = null;
		try {
			// 회전
			Matrix matrix = new Matrix();
			matrix.postRotate(BitmapUtil.GetExifOrientation(path));


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap src = BitmapFactory.decodeFile(path, options);

            int width = src.getWidth();
            int height = src.getHeight();
            resized = Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return resized;
	}

	public void addItem(Image image, int index) {
		mSelectedImgages.add(index, image);
		notifyDataSetChanged();
	}

	public void removeItem(int index) {
		mSelectedImgages.remove(index);
		notifyDataSetChanged();
	}

	@Override
	public int getItemPosition(@NonNull Object object) {
//		return super.getItemPosition(object);
		return POSITION_NONE;
	}
}