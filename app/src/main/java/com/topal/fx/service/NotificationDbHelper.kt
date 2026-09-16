package com.topal.fx.service

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

data class NotificationRecord(
    val id: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val dateStr: String,
    val isForwarded: Boolean
)

/**
 * Native lightweight SQLite helper to store and retrieve intercepted notifications.
 */
class NotificationDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "topalfx_notifications.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "notifications_log"
        const val COL_ID = "id"
        const val COL_PACKAGE_NAME = "package_name"
        const val COL_APP_NAME = "app_name"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_DATE_STR = "date_str"
        const val COL_FORWARDED = "is_forwarded"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PACKAGE_NAME TEXT,
                $COL_APP_NAME TEXT,
                $COL_TITLE TEXT,
                $COL_CONTENT TEXT,
                $COL_TIMESTAMP INTEGER,
                $COL_DATE_STR TEXT,
                $COL_FORWARDED INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Inserts an intercepted notification into the database.
     */
    fun insertNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String,
        timestamp: Long,
        dateStr: String,
        isForwarded: Boolean
    ): Long {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_PACKAGE_NAME, packageName)
                put(COL_APP_NAME, appName)
                put(COL_TITLE, title)
                put(COL_CONTENT, content)
                put(COL_TIMESTAMP, timestamp)
                put(COL_DATE_STR, dateStr)
                put(COL_FORWARDED, if (isForwarded) 1 else 0)
            }
            db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /**
     * Retrieves the most recent N notifications.
     */
    fun getRecentNotifications(limit: Int = 5): List<NotificationRecord> {
        val list = mutableListOf<NotificationRecord>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                "$COL_ID DESC",
                limit.coerceIn(1, 25).toString()
            )
            cursor.use {
                while (it.moveToNext()) {
                    val record = NotificationRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        packageName = it.getString(it.getColumnIndexOrThrow(COL_PACKAGE_NAME)),
                        appName = it.getString(it.getColumnIndexOrThrow(COL_APP_NAME)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        dateStr = it.getString(it.getColumnIndexOrThrow(COL_DATE_STR)),
                        isForwarded = it.getInt(it.getColumnIndexOrThrow(COL_FORWARDED)) == 1
                    )
                    list.add(record)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Returns count of notifications received today.
     */
    fun getTodayNotificationCount(): Int {
        var count = 0
        try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis

            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COL_TIMESTAMP >= ?",
                arrayOf(startOfDay.toString())
            )
            cursor.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}
