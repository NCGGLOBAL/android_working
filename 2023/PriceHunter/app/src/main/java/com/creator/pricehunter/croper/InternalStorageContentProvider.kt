package com.creator.pricehunter.croper

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.creator.pricehunter.ImageCropActivity
import com.creator.pricehunter.ImageViewActivity
import java.io.File
import java.io.FileNotFoundException

/*
 * The solution is taken from here: http://stackoverflow.com/questions/10042695/how-to-get-camera-result-as-a-uri-in-data-folder
 */
class InternalStorageContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return try {
            val mFile = File(context!!.filesDir, ImageCropActivity.Companion.TEMP_PHOTO_FILE_NAME)
            if (!mFile.exists()) {
                mFile.createNewFile()
                context!!.contentResolver.notifyChange(CONTENT_URI, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getType(uri: Uri): String? {
        val path = uri.toString()
        for (extension in MIME_TYPES.keys) {
            if (path.endsWith(extension)) {
                return MIME_TYPES[extension]
            }
        }
        return null
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val f = File(context!!.filesDir, ImageViewActivity.Companion.TEMP_PHOTO_FILE_NAME)
        if (f.exists()) {
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE)
        }
        throw FileNotFoundException(uri.path)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return 0
    }

    companion object {
        val CONTENT_URI = Uri.parse("content://com.myntra.profilepic.crop/")
        private val MIME_TYPES = HashMap<String, String>()

        init {
            MIME_TYPES[".jpg"] = "image/jpeg"
            MIME_TYPES[".jpeg"] = "image/jpeg"
        }
    }
}