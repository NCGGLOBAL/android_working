package com.creator.devmalluplive.croper

import android.net.Uri
import java.io.File

/**
 * @author GT
 */
object Utils {
    fun getImageUri(path: String?): Uri {
        return Uri.fromFile(File(path))
    }
}