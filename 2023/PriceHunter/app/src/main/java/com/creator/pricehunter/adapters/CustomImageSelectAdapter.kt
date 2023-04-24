package com.creator.pricehunter.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.creator.pricehunter.R
import com.creator.pricehunter.models.Image

/**
 * Created by Darshan on 4/18/2015.
 */
class CustomImageSelectAdapter : CustomGenericAdapter<Image> {
    private var mIsSelectedCheck = true

    constructor(context: Context?, images: ArrayList<Image>?) : super(context, images) {}
    constructor(context: Context?, images: ArrayList<Image>?, isSelectedCheck: Boolean) : super(
        context,
        images
    ) {
        mIsSelectedCheck = isSelectedCheck
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_view_item_image_select, null)
            viewHolder = ViewHolder()
            viewHolder.imageView =
                convertView.findViewById<View>(R.id.image_view_image_select) as ImageView
            viewHolder.textView =
                convertView.findViewById<View>(R.id.image_view_image_sequence) as TextView
            viewHolder.view = convertView.findViewById(R.id.view_alpha)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        viewHolder.imageView!!.layoutParams.width = size
        viewHolder.imageView!!.layoutParams.height = size
        viewHolder.view!!.layoutParams.width = size
        viewHolder.view!!.layoutParams.height = size
        if (mIsSelectedCheck && arrayList!![position]!!.isSelected) {
            viewHolder.textView!!.visibility = View.VISIBLE
            viewHolder.textView!!.text = arrayList!![position]!!.sequence.toString() + ""
            viewHolder.view!!.alpha = 0.5f
            (convertView as FrameLayout).foreground =
                context!!.resources.getDrawable(R.drawable.ic_done_white)
        } else {
            viewHolder.textView!!.visibility = View.INVISIBLE
            viewHolder.view!!.alpha = 0.0f
            (convertView as FrameLayout).foreground = null
        }
        Glide.with(context)
            .load(arrayList!![position]!!.path)
            .placeholder(R.drawable.image_placeholder).into(viewHolder.imageView)
        return convertView
    }

    private class ViewHolder {
        var imageView: ImageView? = null
        var textView: TextView? = null
        var view: View? = null
    }
}