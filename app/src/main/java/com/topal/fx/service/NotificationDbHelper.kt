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
    val status: Int
) {
    val isForwarded: Boolean get() = status == NotificationDbHelper.STATUS_SENT
}

/**
 * Native lightweight SQLite helper to store and retrieve intercepted notifications,
 * supporting offline pending queues, automatic status transitions, and data pruning.
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

        // Status definitions
        const val STATUS_PENDING = 0 // Stored locally, waiting to be dispatched to Telegram
        const val STATUS_SENT = 1    // Successfully delivered and acknowledged by Telegram
        const val STATUS_IGNORED = 2 // Filtered/paused (stored for audit only, do not forward)
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
     * Inserts an intercepted notification into the database with specific delivery status.
     * Returns newly generated row ID.
     */
    fun insertNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String,
        timestamp: Long,
        dateStr: String,
        status: Int = STATUS_PENDING
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
                put(COL_FORWARDED, status)
            }
            val id = db.insert(TABLE_NAME, null, values)
            // Silently prune excess rows periodically if table grows very large
            if (id > 0 && id % 100 == 0L) {
                pruneOldRecords(5000)
            }
            id
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /**
     * Updates an existing notification to STATUS_SENT after successful Telegram transmission.
     */
    fun markAsSent(id: Long): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_FORWARDED, STATUS_SENT)
            }
            val rows = db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Retrieves all pending unsent notifications (STATUS_PENDING = 0) ordered chronologically.
     */
    fun getPendingNotifications(limit: Int = 100): List<NotificationRecord> {
        val list = mutableListOf<NotificationRecord>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME,
                null,
                "$COL_FORWARDED = ?",
                arrayOf(STATUS_PENDING.toString()),
                null,
                null,
                "$COL_ID ASC",
                limit.toString()
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(cursorToRecord(it))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Retrieves the most recent N notifications for `/history` command.
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
                    list.add(cursorToRecord(it))
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

    /**
     * Prunes old records, preserving the latest N entries to keep database size minimal forever.
     */
    fun pruneOldRecords(maxToKeep: Int = 5000) {
        try {
            val db = writableDatabase
            db.execSQL(
                "DELETE FROM $TABLE_NAME WHERE $COL_ID NOT IN (SELECT $COL_ID FROM $TABLE_NAME ORDER BY $COL_ID DESC LIMIT ?)",
                arrayOf(maxToKeep.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cursorToRecord(cursor: android.database.Cursor): NotificationRecord {
        return NotificationRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            packageName = cursor.getString(cursor.getColumnIndexOrThrow(COL_PACKAGE_NAME)),
            appName = cursor.getString(cursor.getColumnIndexOrThrow(COL_APP_NAME)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
            dateStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE_STR)),
            status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FORWARDED))
        )
    }
}
