package com.creator.changgolive.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.creator.changgolive.models.Image
import com.creator.changgolive.util.BitmapUtil

class PageAdapter(var context: Context, private val mSelectedImgages: ArrayList<Image>?) :
    PagerAdapter() {
    override fun getCount(): Int {
        return mSelectedImgages!!.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as ImageView
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageView = ImageView(context)
        //		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageBitmap(getImageBitmap(mSelectedImgages!![position].path))
        Log.d(
            "SeongKwon",
            (mSelectedImgages[position].sequence - 1).toString() + "//" + count + "//"
        )
        (container as ViewPager).addView(imageView, 0)
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        (container as ViewPager).removeView(`object` as ImageView)
    }

    fun getImageBitmap(path: String?): Bitmap? {
        var resized: Bitmap? = null
        try {
            // 회전
            val matrix = Matrix()
            matrix.postRotate(BitmapUtil.Companion.GetExifOrientation(path).toFloat())
            val options = BitmapFactory.Options()
            options.inSampleSize = 2
            val src = BitmapFactory.decodeFile(path, options)
            val width = src.width
            val height = src.height
            resized = Bitmap.createBitmap(src, 0, 0, width, height, matrix, true)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return resized
    }

    fun addItem(image: Image, index: Int) {
        mSelectedImgages!!.add(index, image)
        notifyDataSetChanged()
    }

    fun removeItem(index: Int) {
        mSelectedImgages!!.removeAt(index)
        notifyDataSetChanged()
    }

    override fun getItemPosition(`object`: Any): Int {
//		return super.getItemPosition(object);
        return POSITION_NONE
    }
}