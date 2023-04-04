package com.creator.wanpantv.models

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Darshan on 4/18/2015.
 */
class Image : Parcelable {
    var id: Long
    var name: String?
    var path: String?
    var sequence: Int
    var isSelected = false

    constructor(id: Long, name: String?, path: String?, isSelected: Boolean, sequence: Int) {
        this.id = id
        this.name = name
        this.path = path
        this.isSelected = isSelected
        this.sequence = sequence
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeString(path)
        dest.writeInt(sequence)
    }

    private constructor(`in`: Parcel) {
        id = `in`.readLong()
        name = `in`.readString()
        path = `in`.readString()
        sequence = `in`.readInt()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Image?> = object : Parcelable.Creator<Image?> {
            override fun createFromParcel(source: Parcel): Image? {
                return Image(source)
            }

            override fun newArray(size: Int): Array<Image?> {
                return arrayOfNulls(size)
            }
        }
    }
}