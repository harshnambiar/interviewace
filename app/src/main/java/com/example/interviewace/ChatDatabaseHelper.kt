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

data class ChatMessage(val message: String, val isUser: Boolean, val imageUrl: String)
data class UserToken(val email: String, val token: String)

class ChatDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "chatzone.db"
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
        private const val COLUMN_SUMMARY_ID = "summary_id"
        private const val COLUMN_SUMMARY_TEXT = "summary_text"
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
                $COLUMN_BOT_TYPE TEXT,
                $COLUMN_MESSAGE TEXT,
                $COLUMN_IS_USER INTEGER,
                $COLUMN_IMAGE_URL TEXT
            )
        """)

        // Create summaries table with message_count
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SUMMARIES (
                $COLUMN_SUMMARY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECEIVER TEXT,
                $COLUMN_BOT_TYPE TEXT,
                $COLUMN_SUMMARY_TEXT TEXT,
                $COLUMN_TIMESTAMP INTEGER,
                $COLUMN_MESSAGE_COUNT INTEGER
            )
        """)

        Log.d(TAG, "Created tables: $TABLE_USERS, $TABLE_MESSAGES, $TABLE_SUMMARIES")
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

    fun saveMessage(receiver: String, botType: String, message: String, isUser: Boolean, imageUrl: String) {
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
                put(COLUMN_IMAGE_URL, imageUrl)
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
                arrayOf(COLUMN_MESSAGE, COLUMN_IS_USER, COLUMN_IMAGE_URL),
                "$COLUMN_RECEIVER = ? AND $COLUMN_BOT_TYPE = ?",
                arrayOf(receiver, botType),
                null,
                null,
                "$COLUMN_ID ASC"
            )
            while (cursor.moveToNext()) {
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_USER)) == 1
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)) ?: ""
                messages.add(ChatMessage(message, isUser, imageUrl))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving messages: ${e.message}", e)
        } finally {
            db.close()
        }
        Log.d(TAG, "Retrieved ${messages.size} messages for receiver: $receiver, botType: $botType")
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



    fun saveSummary(receiver: String, botType: String, summary: String, messageCount: Int) {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot save summary: receiver is empty")
            return
        }
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_RECEIVER, receiver)
                put(COLUMN_BOT_TYPE, botType)
                put(COLUMN_SUMMARY_TEXT, summary)
                put(COLUMN_TIMESTAMP, System.currentTimeMillis())
                put(COLUMN_MESSAGE_COUNT, messageCount)
            }
            db.insert(TABLE_SUMMARIES, null, values)
            Log.d(TAG, "Saved summary for receiver: $receiver, botType: $botType, messageCount: $messageCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving summary: ${e.message}", e)
        } finally {
            db.close()
        }
    }

    fun getLatestSummary(receiver: String, botType: String): Pair<String?, Int> {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot retrieve summary: receiver is empty")
            return Pair(null, 0)
        }
        val db = readableDatabase
        var summary: String? = null
        var messageCount = 0
        try {
            val cursor = db.query(
                TABLE_SUMMARIES,
                arrayOf(COLUMN_SUMMARY_TEXT, COLUMN_MESSAGE_COUNT),
                "$COLUMN_RECEIVER = ? AND $COLUMN_BOT_TYPE = ?",
                arrayOf(receiver, botType),
                null,
                null,
                "$COLUMN_TIMESTAMP DESC",
                "1"
            )
            if (cursor.moveToFirst()) {
                summary = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY_TEXT))
                messageCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_COUNT))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving summary: ${e.message}", e)
        } finally {
            db.close()
        }
        Log.d(TAG, "Retrieved latest summary for receiver: $receiver, botType: $botType: $summary, messageCount: $messageCount")
        return Pair(summary, messageCount)
    }

    fun summarizeText(context: Context, receiver: String, botType: String, messageCount: Int, queue: RequestQueue, callback: (Boolean) -> Unit) {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot summarize: receiver is empty")
            callback(false)
            return
        }
        // Fetch the last 10 messages
        val recentMessages = getMessages(receiver, botType)
            .takeLast(10)
            .joinToString("\n") { "${if (it.isUser) "User" else botType}: ${it.message}" }

        val summaryPrompt = """
            Summarize the following conversation into a concise summary (100-150 words) that captures the main topics, user preferences, and key points discussed. Focus on user-specific information (e.g., name, likes, dislikes, occupation, location, hobbies, relationship status) and the general context of the conversation. Do not include any sensitive information like passwords or API keys. Here is the conversation:

            $recentMessages
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
                    saveSummary(receiver, botType, summary, messageCount)
                    callback(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing summary response: ${e.message}", e)
                    callback(false)
                }
            },
            { error ->
                Log.e(TAG, "Error generating summary: ${error.message}", error)
                callback(false)
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

    fun getLastTenSummaries(receiver: String, botType: String, maxTokens: Int = 2000): String {
        if (receiver.isEmpty()) {
            Log.e(TAG, "Cannot retrieve summaries: receiver is empty")
            return "No summaries available."
        }
        val summaries = mutableListOf<String>()
        var totalChars = 0
        val db = readableDatabase
        try {
            val cursor = db.query(
                TABLE_SUMMARIES,
                arrayOf(COLUMN_SUMMARY_TEXT),
                "$COLUMN_RECEIVER = ? AND $COLUMN_BOT_TYPE = ?",
                arrayOf(receiver, botType),
                null,
                null,
                "$COLUMN_TIMESTAMP DESC",
                "10"
            )
            while (cursor.moveToNext()) {
                val summary = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY_TEXT)) ?: ""
                totalChars += summary.length
                if (totalChars / 4 > maxTokens) break // ~4 chars per token
                summaries.add(summary)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving summaries: ${e.message}", e)
        } finally {
            db.close()
        }
        return if (summaries.isNotEmpty()) {
            summaries.joinToString("\n") { "Summary: $it" }
        } else {
            "No summaries available."
        }
    }



}