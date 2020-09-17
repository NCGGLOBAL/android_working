package com.nechingu.tagbadmin.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.nechingu.tagbadmin.R
import com.nechingu.tagbadmin.models.ImageItem
import java.util.*

class CustomImageSelectAdapterKt (items: ArrayList<ImageItem>?) : RecyclerView.Adapter<CustomImageSelectAdapterKt.ViewHolder>() {
    val items = items
    var context: Context? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.findViewById<ImageView>(R.id.image_view_image_select)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.grid_view_item_image_select, parent, false)
        context = parent.context

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items!!.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items?.let {
            val item = items[position]
            Glide.with(context)
                    .load(item.imgUrl)
                    .placeholder(R.drawable.image_placeholder).into(holder.imageView)
        }
    }
}