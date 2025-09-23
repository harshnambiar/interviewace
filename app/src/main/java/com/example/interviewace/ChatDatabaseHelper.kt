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
        private const val DATABASE_NAME = "chatzone.db"
        private const val DATABASE_VERSION = 10 // Incremented for schema changes
        private const val TABLE_USERS = "users"
        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_ID = "id"
        private const val COLUMN_RECEIVER = "receiver"
        private const val COLUMN_SUBCATEGORY = "subcategory"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_IS_USER = "is_user"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_TOKEN = "token"
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

        // Create messages table without image_url
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECEIVER TEXT,
                $COLUMN_SUBCATEGORY TEXT,
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

    fun saveMessage(receiver: String, subCategory: String, message: String, isUser: Boolean) {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot save message: receiver is empty")
            return
        }
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_RECEIVER, receiver)
                put(COLUMN_SUBCATEGORY, subCategory)
                put(COLUMN_MESSAGE, message)
                put(COLUMN_IS_USER, if (isUser) 1 else 0)
            }
            db.insert(TABLE_MESSAGES, null, values)
            Log.d(TAG, "Saved message for receiver: $receiver, subCategory: $subCategory")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message: ${e.message}", e)
        } finally {
            db.close()
        }
    }

    fun getMessages(receiver: String, subCategory: String): List<ChatMessage> {
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
                "$COLUMN_RECEIVER = ? AND $COLUMN_SUBCATEGORY = ?",
                arrayOf(receiver, subCategory),
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
        Log.d(TAG, "Retrieved ${messages.size} messages for receiver: $receiver, subCategory: $subCategory")
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

    fun getMessageCount(receiver: String, subCategory: String): Int {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot count messages: receiver is empty")
            return 0
        }
        val db = readableDatabase
        var count = 0
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_MESSAGES WHERE $COLUMN_RECEIVER = ? AND $COLUMN_SUBCATEGORY = ?",
                arrayOf(receiver, subCategory)
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
        Log.d(TAG, "Message count for receiver: $receiver, subCategory: $subCategory is $count")
        return count
    }

    fun purgeChat(receiver: String, subCategory: String) {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot purge chat: receiver is empty")
            return
        }
        val db = writableDatabase
        try {
            db.delete(
                TABLE_MESSAGES,
                "$COLUMN_RECEIVER = ? AND $COLUMN_SUBCATEGORY = ?",
                arrayOf(receiver, subCategory)
            )
            Log.d(TAG, "Purged messages for receiver: $receiver, subCategory: $subCategory")
        } catch (e: Exception) {
            Log.e(TAG, "Error purging chat: ${e.message}", e)
        } finally {
            db.close()
        }
    }

    fun summarizeText(context: Context, receiver: String, subCategory: String, queue: RequestQueue, callback: (String?) -> Unit) {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot summarize: receiver is empty")
            callback(null)
            return
        }
        // Fetch all messages
        val allMessages = getMessages(receiver, subCategory)
            .joinToString("\n") { "${if (it.isUser) "User" else subCategory}: ${it.message}" }

        val summaryPrompt = """
            Summarize the following conversation into a concise summary (100-150 words) that captures the main topics, user preferences, and key points discussed. Focus on user-specific information (e.g., name, likes, dislikes, occupation, location, hobbies, relationship status) and the general context of the conversation. Do not include any sensitive information like passwords or API keys. Here is the conversation:

            $allMessages
        """.trimIndent()

        val url = "https://api.cerebras.ai/v1/chat/completions"
        val requestMessages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", summaryPrompt)
            })
        }
        val requestBody = JSONObject().apply {
            put("model", "llama3.1-8b")
            put("messages", requestMessages)
            put("max_tokens", 200)
            put("temperature", 0.5)
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, requestBody,
            { response ->
                try {
                    val summary = response.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    Log.d(TAG, "Generated summary: $summary")
                    callback(summary)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing summary response: ${e.message}", e)
                    callback(null)
                }
            },
            { error ->
                Log.e(TAG, "Error generating summary: ${error.message}", error)
                callback(null)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Authorization" to "Bearer ${ApiKeyProvider.getApiKey2(context)}")
            }
        }
        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            10000, // 10-second timeout
            2,     // Increased retries
            1.0f   // Backoff multiplier
        )
        queue.add(request)
    }
}