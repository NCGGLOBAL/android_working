package com.creator.wellbeinglive.delegator

import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Delegator for javaScript. Global preference. manage values to share with other apps.
 */
object HNGlobalPreference : HNPlugun() {
    private var sInstance: HNGlobalPreference? = null

    //	private DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
    val instance: HNGlobalPreference?
        get() {
            if (sInstance == null) {
                sInstance = HNGlobalPreference
            }
            return sInstance
        }

    internal class DatabaseHelper private constructor(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, 1) {
        private var sqliteDb: SQLiteDatabase? = null
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE " + DB_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, key TEXT, value TEXT)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        fun put(key: String, value: String?) {
            sqliteDb = writableDatabase
            val columns = arrayOf(COLUMN_NAME_VALUE)
            val selectionArgs = arrayOf(key)
            val cursor = sqliteDb?.query(
                DB_NAME, columns, COLUMN_NAME_KEY + "=?", selectionArgs,
                null, null, null
            )
            if (cursor?.count != 0) {
                sqliteDb?.delete(DB_NAME, COLUMN_NAME_KEY + "=?", selectionArgs)
            }
            val values = ContentValues()
            values.put(COLUMN_NAME_KEY, key)
            values.put(COLUMN_NAME_VALUE, value)
            sqliteDb?.insert(DB_NAME, null, values)
            sqliteDb?.close()
        }

        operator fun get(key: String): String {
            sqliteDb = writableDatabase
            val columns = arrayOf(COLUMN_NAME_VALUE)
            val selectionArgs = arrayOf(key)
            val cursor = sqliteDb?.query(
                DB_NAME, columns, COLUMN_NAME_KEY + "=?", selectionArgs,
                null, null, null
            )
            var value = "undefined"
            if (cursor?.count == 1 && cursor.moveToFirst()) {
                value = cursor.getString(0)
            }
            sqliteDb?.close()
            return value
        }

        val all: HashMap<String, String>
            get() {
                sqliteDb = writableDatabase
                val data = HashMap<String, String>()
                val columns = arrayOf(COLUMN_NAME_KEY, COLUMN_NAME_VALUE)
                val cursor = sqliteDb?.query(DB_NAME, columns, null, null, null, null, null)
                while (cursor?.moveToNext() == true) {
                    data[cursor.getString(0)] = cursor.getString(1)
                }
                sqliteDb?.close()
                return data
            }

        fun remove(key: String) {
            sqliteDb = writableDatabase
            val selectionArgs = arrayOf(key)
            sqliteDb?.delete(DB_NAME, COLUMN_NAME_KEY + "=?", selectionArgs)
            sqliteDb?.close()
        }

        companion object {
            const val DB_NAME = "HNDB"
            private const val COLUMN_NAME_KEY = "key"
            private const val COLUMN_NAME_VALUE = "value"
        }
    }
}