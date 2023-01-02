package com.creator.ssakdeal.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.creator.ssakdeal.delegator.HNSharedPreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by skcrackers on 10/16/17.
 */

public class BitmapUtil extends BitmapTransformation {
    private float rotateRotationAngle = 0f;

    public BitmapUtil(Context context, float rotateRotationAngle) {
        super( context );

        this.rotateRotationAngle = rotateRotationAngle;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        Matrix matrix = new Matrix();

        matrix.postRotate(rotateRotationAngle);

        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.getWidth(), toTransform.getHeight(), matrix, true);
    }

    @Override
    public String getId() {
        return "rotate" + rotateRotationAngle;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static void recycleBitmap(ImageView iv) {
        Drawable d = iv.getDrawable();
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            b.recycle();
        }
    }

    public synchronized static int GetExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }

            }
        }

        return degree;
    }

    public synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap.recycle();
                    bitmap = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }

        return bitmap;
    }


    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean saveImage(Context context, String preFilePath, String fileName, String sequence) {
        try {
            Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ " + preFilePath);
            Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ " + fileName);
            Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ " + GetExifOrientation(preFilePath));

            File orignFile = new File(preFilePath);
            if(GetExifOrientation(preFilePath) == 0) {
                File destFile = new File(context.getFilesDir() + "/" + fileName);
                Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ destFile1 = " + destFile.getAbsolutePath());
                copy(orignFile, destFile);
            } else {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(orignFile.getAbsolutePath(),bmOptions);

                FileOutputStream out = null;
                try {
                    Log.e("SeongKwon", "BitmapUtil saveImage@@@@@@@@@@@@ destFile2 = " + context.getFilesDir() + "/" + fileName);
                    Bitmap bm = GetRotatedBitmap(bitmap, GetExifOrientation(preFilePath));
                    out = new FileOutputStream(context.getFilesDir() + "/" + fileName);
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 저장된 이미지 리스트를 preference에 저장
            String savedImage = HNSharedPreference.getSharedPreference(context, "savedImage");
            savedImage += fileName + "&" + sequence + ",";

            HNSharedPreference.putSharedPreference(context, "savedImage", savedImage);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean saveRotate(Context context, String preFilePath, String fileName) {
        File orignFile = new File(preFilePath);
        if(GetExifOrientation(preFilePath) != 0) {
            File destFile = new File(context.getFilesDir() + "/" + fileName);

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(orignFile.getAbsolutePath(), bmOptions);

            FileOutputStream out = null;
            try {
                Bitmap bm = GetRotatedBitmap(bitmap, GetExifOrientation(preFilePath));
                out = new FileOutputStream(context.getFilesDir() + "/" + fileName);
                bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static boolean deleteImage(Context context, String fileName) {
        try {
            File file = new File(context.getFilesDir() + "/");
            File[] flist = file.listFiles();
            Log.d("SeongKwon", "imgcnt = " + flist.length);
            Log.d("SeongKwon", fileName);
            Log.d("SeongKwon", context.getFilesDir() + "/");

            for (int i = 0; i < flist.length; i++) {
                String fname = flist[i].getName();
                Log.d("SeongKwon", fname);
                if (fname.equals(fileName)) {
                    flist[i].delete();
                }
            }

            // 저장된 이미지 리스트를 preference에 저장
            String savedImage = HNSharedPreference.getSharedPreference(context, "savedImage");
            String[] savedImageArray = savedImage.split(",");

            String tmp = "";
            for (int i = 0; i < savedImageArray.length; i++) {
                String imageItem = savedImageArray[i];
                Log.d("SeongKwon", "imageItem = " + imageItem);

                int delemeter = imageItem.indexOf("&");
                Log.d("SeongKwon", "imageArray[0] = " + imageItem.substring(0, delemeter));
                Log.d("SeongKwon", "imageArray[1] = " + imageItem.substring(delemeter + 1, imageItem.length()));

                String name = imageItem.substring(0, delemeter);
                Log.d("SeongKwon", name + "//" + fileName);
                if(!name.equals(fileName)) {
                    tmp += imageItem + ",";
                }
            }
            HNSharedPreference.putSharedPreference(context, "savedImage", tmp);

        } catch (Exception e) {
            Toast.makeText(context, "파일 삭제 실패 ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean deleteImages(Context context, String filePath)
    {
        HNSharedPreference.putSharedPreference(context, "savedImage", "");

        String mPath = filePath;
        File dir = new File(mPath);

        String[] children = dir.list();
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                File f = new File(mPath + filename);

                if (f.exists()) {
                    f.delete();
                }
            }
        }
        return true;
    }

    public static Bitmap getSavedImage(Context context, String fileName) {
        Bitmap bm = null;
        try {
            String imgpath = context.getFilesDir() + "/" + fileName + ".jpg";
            bm = BitmapFactory.decodeFile(imgpath);
            Toast.makeText(context, "load ok", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "load error", Toast.LENGTH_SHORT).show();
        }
        return bm;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}