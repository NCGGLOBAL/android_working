package com.nechingu.tagb.helpers;

/**
 * Created by Darshan on 5/26/2015.
 */
public class Constants {
    public static final int PERMISSION_REQUEST_CODE = 1000;
    public static final int PERMISSION_GRANTED = 1001;
    public static final int PERMISSION_DENIED = 1002;

    public static final int REQUEST_CODE = 2000;

    public static final int FETCH_STARTED = 2001;
    public static final int FETCH_COMPLETED = 2002;
    public static final int ERROR = 2005;

    public static final int REQUEST_CAMERA = 3000;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3001;

    public static final int REQUEST_SELECT_IMAGE_CAMERA = 3000;
    public static final int REQUEST_SELECT_IMAGE_ALBUM = 3001;

    public static final int FILECHOOSER_NORMAL_REQ_CODE = 1001;
    public static final int FILECHOOSER_LOLLIPOP_REQ_CODE = 1002;

    public static final int PERMISSIONS_MULTIPLE_REQUEST = 4001;

    public static final int REQUEST_ADD_IMAGE = 5000;
    public static final int REQUEST_EDIT_IMAGE = 5001;
    public static final int REQUEST_CROP_IMAGE = 5002;

    /**
     * Request code for permission has to be < (1 << 8)
     * Otherwise throws java.lang.IllegalArgumentException: Can only use lower 8 bits for requestCode
     */
    public static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 23;

    public static final String INTENT_EXTRA_ALBUM = "album";
    public static final String INTENT_EXTRA_IMAGES = "images";
    public static final String INTENT_EXTRA_LIMIT = "limit";
    public static final int DEFAULT_LIMIT = 10;

    //Maximum number of images that can be selected at a time
    public static int limit = 8;
}
