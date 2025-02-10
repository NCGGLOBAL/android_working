package com.creator.sayrobin.adapters

import android.content.Context
import android.view.LayoutInflater
import android.widget.BaseAdapter

/**
 * Created by Darshan on 4/24/2015.
 */
abstract class CustomGenericAdapter<T>(
    protected var context: Context?,
    protected var arrayList: ArrayList<T>?
) : BaseAdapter() {
    protected var layoutInflater: LayoutInflater
    protected var size = 0

    init {
        layoutInflater = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return arrayList!!.size
    }

    override fun getItem(position: Int): T {
        return arrayList!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setLayoutParams(size: Int) {
        this.size = size
    }

    fun releaseResources() {
        arrayList = null
        context = null
    }
}