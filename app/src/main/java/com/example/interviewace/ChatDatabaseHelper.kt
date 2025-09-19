package com.example.interviewace

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(val message: String, val isUser: Boolean)
data class UserToken(val email: String, val token: String)

class ChatDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "interviewace.db"
        private const val DATABASE_VERSION = 9 // Incremented for message_count column
        private const val TABLE_USERS = "users"
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_SUMMARIES = "summaries"
        private const val COLUMN_ID = "id"
        private const val COLUMN_RECEIVER = "receiver"
        private const val COLUMN_BOT_TYPE = "bot_type"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_IS_USER = "is_user"
        private const val COLUMN_IMAGE_URL = "image_url"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_TOKEN = "token"

        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_MESSAGE_COUNT = "message_count"
        private const val TAG = "ChatDatabaseHelper"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_EMAIL TEXT PRIMARY KEY,
                $COLUMN_TOKEN TEXT
            )
        """)

        // Create messages table with all required columns
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECEIVER TEXT,
                $COLUMN_MESSAGE TEXT,
                $COLUMN_IS_USER INTEGER
            )
        """)



        Log.d(TAG, "Created tables: $TABLE_USERS, $TABLE_MESSAGES")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop and recreate tables
        dropAllTables(db)
        onCreate(db)
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val tableName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (tableName != "sqlite_sequence" && tableName != "android_metadata") {
                        db.execSQL("DROP TABLE IF EXISTS $tableName")
                        Log.d(TAG, "Dropped table: $tableName")
                    }
                    cursor.moveToNext()
                }
            }
            cursor.close()
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Error dropping tables: ${e.message}", e)
        } finally {
            db.endTransaction()
        }
    }

    fun flush() {
        val db = writableDatabase
        dropAllTables(db)
        db.close()
        // Reopen database to recreate tables
        val newDb = writableDatabase
        onCreate(newDb)
        newDb.close()
        Log.d(TAG, "Database flushed and tables recreated")
    }

    fun saveMessage(receiver: String, botType: String, message: String, isUser: Boolean) {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot save message: receiver is empty")
            return
        }
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_RECEIVER, receiver)
                put(COLUMN_BOT_TYPE, botType)
                put(COLUMN_MESSAGE, message)
                put(COLUMN_IS_USER, if (isUser) 1 else 0)
            }
            db.insert(TABLE_MESSAGES, null, values)
            Log.d(TAG, "Saved message for receiver: $receiver, botType: $botType")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message: ${e.message}", e)
        } finally {
            db.close()
        }
    }

    fun getMessages(receiver: String, botType: String): List<ChatMessage> {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot retrieve messages: receiver is empty")
            return emptyList()
        }
        val messages = mutableListOf<ChatMessage>()
        val db = readableDatabase
        try {
            val cursor = db.query(
                TABLE_MESSAGES,
                arrayOf(COLUMN_MESSAGE, COLUMN_IS_USER),
                "$COLUMN_RECEIVER = ? AND $COLUMN_BOT_TYPE = ?",
                arrayOf(receiver, botType),
                null,
                null,
                "$COLUMN_ID ASC"
            )
            while (cursor.moveToNext()) {
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_USER)) == 1
                messages.add(ChatMessage(message, isUser))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving messages: ${e.message}", e)
        } finally {
            db.close()
        }
        Log.d(TAG, "Retrieved ${messages.size} messages for receiver: $receiver")
        return messages
    }

    fun getUserToken(email: String): String? {
        val db = readableDatabase
        var token: String? = null
        try {
            val cursor = db.query(
                TABLE_USERS,
                arrayOf(COLUMN_TOKEN),
                "$COLUMN_EMAIL = ?",
                arrayOf(email),
                null,
                null,
                null
            )
            if (cursor.moveToFirst()) {
                token = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOKEN))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving token: ${e.message}", e)
        } finally {
            db.close()
        }
        return token
    }

    fun saveUserToken(email: String, token: String) {
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_EMAIL, email)
                put(COLUMN_TOKEN, token)
            }
            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving token: ${e.message}", e)
        } finally {
            db.close()
        }
    }

    fun getMessageCount(receiver: String, botType: String): Int {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot count messages: receiver is empty")
            return 0
        }
        val db = readableDatabase
        var count = 0
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_MESSAGES WHERE $COLUMN_RECEIVER = ? AND $COLUMN_BOT_TYPE = ?",
                arrayOf(receiver, botType)
            )
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error counting messages: ${e.message}", e)
        } finally {
            db.close()
        }
        Log.d(TAG, "Message count for receiver: $receiver, botType: $botType is $count")
        return count
    }








}