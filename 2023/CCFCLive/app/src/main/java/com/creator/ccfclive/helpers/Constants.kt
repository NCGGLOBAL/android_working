package com.creator.ccfclive.helpers

/**
 * Created by Darshan on 5/26/2015.
 */
object Constants {
    const val PERMISSION_REQUEST_CODE = 1000
    const val PERMISSION_GRANTED = 1001
    const val PERMISSION_DENIED = 1002
    const val REQUEST_CODE = 2000
    const val FETCH_STARTED = 2001
    const val FETCH_COMPLETED = 2002
    const val ERROR = 2005
    const val REQUEST_CAMERA = 3000
    const val REQUEST_WRITE_EXTERNAL_STORAGE = 3001
    const val REQUEST_SELECT_IMAGE_CAMERA = 3000
    const val REQUEST_SELECT_IMAGE_ALBUM = 3001
    const val FILECHOOSER_NORMAL_REQ_CODE = 1001
    const val FILECHOOSER_LOLLIPOP_REQ_CODE = 1002
    const val PERMISSIONS_MULTIPLE_REQUEST = 4001
    const val REQUEST_ADD_IMAGE = 5000
    const val REQUEST_EDIT_IMAGE = 5001
    const val REQUEST_CROP_IMAGE = 5002
    const val REQUEST_GET_FILE = 6000

    /**
     * Request code for permission has to be < (1 << 8)
     * Otherwise throws java.lang.IllegalArgumentException: Can only use lower 8 bits for requestCode
     */
    const val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 23
    const val INTENT_EXTRA_ALBUM = "album"
    const val INTENT_EXTRA_IMAGES = "images"
    const val INTENT_EXTRA_LIMIT = "limit"
    const val DEFAULT_LIMIT = 10

    //Maximum number of images that can be selected at a time
    var limit = 8
}