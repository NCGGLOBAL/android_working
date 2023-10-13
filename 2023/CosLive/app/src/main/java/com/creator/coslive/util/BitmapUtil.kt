package com.creator.coslive.util

import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.creator.coslive.delegator.HNSharedPreference
import java.io.*

/**
 * Created by skcrackers on 10/16/17.
 */
class BitmapUtil(context: Context?, rotateRotationAngle: Float) : BitmapTransformation(context) {
    private var rotateRotationAngle = 0f

    init {
        this.rotateRotationAngle = rotateRotationAngle
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotateRotationAngle)
        return Bitmap.createBitmap(
            toTransform,
            0,
            0,
            toTransform.width,
            toTransform.height,
            matrix,
            true
        )
    }

    override fun getId(): String {
        return "rotate$rotateRotationAngle"
    }

    companion object {
        fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        fun recycleBitmap(iv: ImageView) {
            val d = iv.drawable
            if (d is BitmapDrawable) {
                val b = d.bitmap
                b.recycle()
            }
        }

        @Synchronized
        fun GetExifOrientation(filepath: String?): Int {
            var degree = 0
            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(filepath!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (exif != null) {
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                if (orientation != -1) {
                    // We only recognize a subset of orientation tag values.
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                    }
                }
            }
            return degree
        }

        @Synchronized
        fun GetRotatedBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
            var bitmap = bitmap
            if (degrees != 0 && bitmap != null) {
                val m = Matrix()
                m.setRotate(
                    degrees.toFloat(),
                    bitmap.width.toFloat() / 2,
                    bitmap.height.toFloat() / 2
                )
                try {
                    val b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                    if (bitmap != b2) {
                        bitmap.recycle()
                        bitmap = b2
                    }
                } catch (ex: OutOfMemoryError) {
                    // We have no memory to rotate. Return the original bitmap.
                }
            }
            return bitmap
        }

        fun getRealPathFromURI(context: Context, contentUri: Uri?): String {
            var cursor: Cursor? = null
            return try {
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                cursor.getString(column_index)
            } finally {
                cursor?.close()
            }
        }

        fun saveImage(
            context: Context,
            preFilePath: String?,
            fileName: String?,
            sequence: String
        ): Boolean {
            try {
                Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ $preFilePath")
                Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ $fileName")
                Log.e(
                    "SeongKwon",
                    "BitmapUtil saveImage@@@@@@@@@@@@ " + GetExifOrientation(preFilePath)
                )
                val orignFile = File(preFilePath)
                if (GetExifOrientation(preFilePath) == 0) {
                    val destFile = File(context.filesDir.toString() + "/" + fileName)
                    Log.e(
                        "SeongKwon",
                        "BitmapUtil saveImage@@@@@@@@@@@@ destFile1 = " + destFile.absolutePath
                    )
                    copy(orignFile, destFile)
                } else {
                    val bmOptions = BitmapFactory.Options()
                    val bitmap = BitmapFactory.decodeFile(orignFile.absolutePath, bmOptions)
                    var out: FileOutputStream? = null
                    try {
                        Log.e(
                            "SeongKwon",
                            "BitmapUtil saveImage@@@@@@@@@@@@ destFile2 = " + context.filesDir + "/" + fileName
                        )
                        val bm = GetRotatedBitmap(bitmap, GetExifOrientation(preFilePath))
                        out = FileOutputStream(context.filesDir.toString() + "/" + fileName)
                        bm!!.compress(CompressFormat.JPEG, 100, out)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            out?.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }

                // 저장된 이미지 리스트를 preference에 저장
                var savedImage = HNSharedPreference.getSharedPreference(context, "savedImage")
                savedImage += "$fileName&$sequence,"
                HNSharedPreference.putSharedPreference(context, "savedImage", savedImage)
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }

        fun saveRotate(context: Context, preFilePath: String?, fileName: String): Boolean {
            val orignFile = File(preFilePath)
            if (GetExifOrientation(preFilePath) != 0) {
                val destFile = File(context.filesDir.toString() + "/" + fileName)
                val bmOptions = BitmapFactory.Options()
                val bitmap = BitmapFactory.decodeFile(orignFile.absolutePath, bmOptions)
                var out: FileOutputStream? = null
                try {
                    val bm = GetRotatedBitmap(bitmap, GetExifOrientation(preFilePath))
                    out = FileOutputStream(context.filesDir.toString() + "/" + fileName)
                    bm!!.compress(CompressFormat.JPEG, 100, out)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        out?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return true
        }

        fun deleteImage(context: Context, fileName: String?): Boolean {
            try {
                val file = File(context.filesDir.toString() + "/")
                val flist = file.listFiles()
                Log.d("SeongKwon", "imgcnt = " + flist.size)
                Log.d("SeongKwon", fileName!!)
                Log.d("SeongKwon", context.filesDir.toString() + "/")
                for (i in flist.indices) {
                    val fname = flist[i].name
                    Log.d("SeongKwon", fname)
                    if (fname == fileName) {
                        flist[i].delete()
                    }
                }

                // 저장된 이미지 리스트를 preference에 저장
                val savedImage = HNSharedPreference.getSharedPreference(context, "savedImage")
                val savedImageArray = savedImage!!.split(",").toTypedArray()
                var tmp = ""
                for (i in savedImageArray.indices) {
                    val imageItem = savedImageArray[i]
                    Log.d("SeongKwon", "imageItem = $imageItem")
                    val delemeter = imageItem.indexOf("&")
                    Log.d("SeongKwon", "imageArray[0] = " + imageItem.substring(0, delemeter))
                    Log.d(
                        "SeongKwon",
                        "imageArray[1] = " + imageItem.substring(delemeter + 1, imageItem.length)
                    )
                    val name = imageItem.substring(0, delemeter)
                    Log.d("SeongKwon", "$name//$fileName")
                    if (name != fileName) {
                        tmp += "$imageItem,"
                    }
                }
                HNSharedPreference.putSharedPreference(context, "savedImage", tmp)
            } catch (e: Exception) {
                Toast.makeText(context, "파일 삭제 실패 ", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        fun deleteImages(context: Context?, filePath: String): Boolean {
            HNSharedPreference.putSharedPreference(context, "savedImage", "")
            val dir = File(filePath)
            val children = dir.list()
            if (children != null) {
                for (i in children.indices) {
                    val filename = children[i]
                    val f = File(filePath + filename)
                    if (f.exists()) {
                        f.delete()
                    }
                }
            }
            return true
        }

        fun getSavedImage(context: Context, fileName: String): Bitmap? {
            var bm: Bitmap? = null
            try {
                val imgpath = context.filesDir.toString() + "/" + fileName + ".jpg"
                bm = BitmapFactory.decodeFile(imgpath)
                Toast.makeText(context, "load ok", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "load error", Toast.LENGTH_SHORT).show()
            }
            return bm
        }

        @Throws(IOException::class)
        fun copy(src: File?, dst: File?) {
            val `in`: InputStream = FileInputStream(src)
            try {
                val out: OutputStream = FileOutputStream(dst)
                try {
                    // Transfer bytes from in to out
                    val buf = ByteArray(1024)
                    var len: Int
                    while (`in`.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                } finally {
                    out.close()
                }
            } finally {
                `in`.close()
            }
        }

        fun uriToBitmap(context: Context, uri: Uri?): Bitmap? {
            val bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri)
            return bitmap
        }
    }
}