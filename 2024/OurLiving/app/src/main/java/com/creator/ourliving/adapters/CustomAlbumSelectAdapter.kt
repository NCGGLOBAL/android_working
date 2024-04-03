package com.creator.ourliving.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.creator.ourliving.R
import com.creator.ourliving.models.Album

/**
 * Created by Darshan on 4/14/2015.
 */
class CustomAlbumSelectAdapter(
    context: Context?,
    albums: ArrayList<Album>?,
    albumCount: HashMap<String?, Long>
) : CustomGenericAdapter<Album>(context, albums) {
    private var count = HashMap<String?, Long>()

    init {
        count = albumCount
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_view_item_album_select, null)
            viewHolder = ViewHolder()
            viewHolder.imageView =
                convertView.findViewById<View>(R.id.image_view_album_image) as ImageView
            viewHolder.textViewName =
                convertView.findViewById<View>(R.id.text_view_album_name) as TextView
            viewHolder.textViewCount =
                convertView.findViewById<View>(R.id.text_view_album_count) as TextView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

//        viewHolder.imageView.getLayoutParams().width = size;
//        viewHolder.imageView.getLayoutParams().height = size;
        viewHolder.textViewName!!.text = arrayList!![position]!!.name
        viewHolder.textViewCount!!.text = count[arrayList!![position]!!.name].toString() + ""
        Glide.with(context)
            .load(arrayList!![position]!!.cover)
            .placeholder(R.drawable.image_placeholder).centerCrop().into(viewHolder.imageView)
        return convertView
    }

    private class ViewHolder {
        var imageView: ImageView? = null
        var textViewName: TextView? = null
        var textViewCount: TextView? = null
    }
}