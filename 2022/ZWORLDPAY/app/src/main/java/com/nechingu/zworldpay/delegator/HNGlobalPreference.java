package com.nechingu.zworldpay.delegator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

/**
 * Delegator for javaScript. Global preference. manage values to share with other apps.
 */
public class HNGlobalPreference extends HNPlugun {
	private static HNGlobalPreference sInstance;
//	private DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());

	public static HNGlobalPreference getInstance() {
		if (sInstance == null) {
			sInstance = new HNGlobalPreference();
		}
		return sInstance;
	}

	class DatabaseHelper extends SQLiteOpenHelper {
		public static final String DB_NAME = "HNDB";

		private static final String COLUMN_NAME_KEY = "key";
		private static final String COLUMN_NAME_VALUE = "value";

		private SQLiteDatabase sqliteDb;

		private DatabaseHelper(Context context) {
			super(context, DB_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + DB_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, key TEXT, value TEXT)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}

		public void put(String key, String value) {
			sqliteDb = getWritableDatabase();

			String[] columns = { COLUMN_NAME_VALUE };
			String[] selectionArgs = { key };
			Cursor cursor = sqliteDb.query(DatabaseHelper.DB_NAME, columns, COLUMN_NAME_KEY + "=?", selectionArgs,
					null, null, null);

			if (cursor.getCount() != 0) {
				sqliteDb.delete(DB_NAME, COLUMN_NAME_KEY + "=?", selectionArgs);
			}

			ContentValues values = new ContentValues();
			values.put(COLUMN_NAME_KEY, key);
			values.put(COLUMN_NAME_VALUE, value);
			sqliteDb.insert(DatabaseHelper.DB_NAME, null, values);
			sqliteDb.close();
		}

		public String get(String key) {
			sqliteDb = getWritableDatabase();
			String[] columns = { COLUMN_NAME_VALUE };
			String[] selectionArgs = { key };
			Cursor cursor = sqliteDb.query(DatabaseHelper.DB_NAME, columns, COLUMN_NAME_KEY + "=?", selectionArgs,
					null, null, null);

			String value = "undefined";
			if (cursor.getCount() == 1 && cursor.moveToFirst()) {
				value = cursor.getString(0);
			}
			sqliteDb.close();
			return value;
		}

		public HashMap<String, String> getAll() {
			sqliteDb = getWritableDatabase();
			HashMap<String, String> data = new HashMap<String, String>();

			String[] columns = { COLUMN_NAME_KEY, COLUMN_NAME_VALUE };
			Cursor cursor = sqliteDb.query(DatabaseHelper.DB_NAME, columns, null, null, null, null, null);

			while (cursor.moveToNext()) {
				data.put(cursor.getString(0), cursor.getString(1));
			}

			sqliteDb.close();
			return data;
		}

		public void remove(String key) {
			sqliteDb = getWritableDatabase();
			String[] selectionArgs = { key };
			sqliteDb.delete(DB_NAME, COLUMN_NAME_KEY + "=?", selectionArgs);
			sqliteDb.close();
		}
	}
}
