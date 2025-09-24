package com.example.interviewace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.interviewace.databinding.ActivityChatBinding
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var dbHelper: ChatDatabaseHelper
    private lateinit var userEmail: String
    private lateinit var subCategory: String
    private val messages = mutableListOf<Message>()
    private val TAG = "ChatActivity"
    private lateinit var queue: RequestQueue
    private var latestSummary: String? = null // Store the latest summary

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply subCategory from Intent
        subCategory = intent.getStringExtra("SUB_CATEGORY_NAME") ?: "unknown"

        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Volley
        queue = Volley.newRequestQueue(this)

        // Initialize SQLite
        dbHelper = ChatDatabaseHelper(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: run {
            Log.e(TAG, "No Email Entered or Logged in")
            finish()
            return
        }

        // Initialize RecyclerView
        chatAdapter = ChatAdapter(messages, this) // No onImageClick parameter
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true // Scroll to bottom
            }
            adapter = chatAdapter
        }

        // start new chat from SQLite
        startNewChat()

        // Handle send button click
        binding.sendButton.setOnClickListener {
            val userInput = binding.messageInput.text.toString().trim()
            if (userInput.isNotEmpty()) {
                // Add and save user message
                val userMessage = Message(content = userInput, isUser = true)
                chatAdapter.addMessage(userMessage)
                dbHelper.saveMessage(userEmail, subCategory, userInput, isUser = true)
                binding.messageInput.text.clear()
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)

                // Fetch summary and then bot response
                fetchSummaryAndBotResponse(userInput, subCategory)
            }
        }
    }

    private fun startNewChat() {
        // Load messages from SQLite for the current subCategory
        dbHelper.purgeChat(userEmail, subCategory)
        messages.clear()

        chatAdapter.notifyDataSetChanged()
        if (messages.isEmpty()) {
            // Add welcome message if no history exists
            val welcomeMessage = Message(content = "Hello! This is your interview practice for the $subCategory. Shall we begin?", isUser = false)
            chatAdapter.addMessage(welcomeMessage)
            dbHelper.saveMessage(userEmail, subCategory, welcomeMessage.content, isUser = false)
        }
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun fetchSummaryAndBotResponse(userInput: String, subCategory: String) {
        // Fetch summary first
        dbHelper.summarizeText(this, userEmail, subCategory, queue) { summary ->
            latestSummary = summary // Store the summary
            // Proceed to fetch bot response
            fetchBotResponse(userInput, subCategory)
        }
    }

    private fun fetchBotResponse(userInput: String, subCategory: String, retryCount: Int = 0) {
        val apiKey = ApiKeyProvider.getApiKey2(this) // Use Cerebras API key
        val maxRetries = 2

        // Define system prompt based on subCategory
        var systemPrompt = when (subCategory) {
            "IIM Ahmedabad" -> "You are Ajit, the interviewer for the selection of the user to IIM Ahmedabad."
            "IIM Bangalore" -> "You are Krishnan, the interviewer for the selection of the user to IIM Bangalore."
            "IIM Calcutta" -> "You are Aarti, the interviewer for the selection of the user to IIM Calcutta."
            "Indian School of Business" -> "You are Abhishek, the interviewer for the selection of the user to the Indian School of Business."
            "Harvard University" -> "You are Wallace, the interviewer for the selection of the user to the Harvard University."
            "Stanford University" -> "You are Elena, the interviewer for the selection of the user to the Stanford University."
            "London Business School" -> "You are Jeanne, the interviewer for the selection of the user to the London Business School."
            "Wharton University" -> "You are Douglas, the interviewer for the selection of the user to the Wharton University."
            else -> "You are a helpful assistant"
        }

        val commonPrompt = "Each of your responses is made up of two parts: 1. Telling the user if their previous response was good for the interview purpose and suggesting improvements where applicable, and 2. Asking the next question or follow up statement as the interviewer."
        systemPrompt += commonPrompt

        // Append stored summary to system prompt
        val moreAddedPrompt = if (latestSummary != null) {
            "\nConversation Summary: $latestSummary"
        } else {
            "\nNo conversation summary available."
        }
        systemPrompt += moreAddedPrompt

        // Prepare recent messages (trimmed to avoid token limit)
        val recentMessages = trimMessages(messages)
        val requestMessages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            // Add recent conversation history
            recentMessages.forEach { message ->
                put(JSONObject().apply {
                    put("role", if (message.isUser) "user" else "assistant")
                    put("content", message.content)
                })
            }
            // Add the latest user input
            put(JSONObject().apply {
                put("role", "user")
                put("content", userInput)
            })
        }

        // Check token estimate
        val estimatedTokens = calculatePromptMetrics(requestMessages)
        if (estimatedTokens > 8192 && retryCount < maxRetries) {
            // Retry with fewer messages
            fetchBotResponse(userInput, subCategory, retryCount + 1)
            return
        } else if (estimatedTokens > 8192) {
            handleError("Message history too long, please clear some messages.", queue)
            return
        }

        // Cerebras API request
        val url = "https://api.cerebras.ai/v1/chat/completions"
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
                    val botResponse = response.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    val botMessage = Message(content = botResponse, isUser = false)
                    chatAdapter.addMessage(botMessage)
                    dbHelper.saveMessage(userEmail, subCategory, botResponse, isUser = false)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Cerebras response: ${e.message}", e)
                    handleError("Error processing response from server.", queue)
                }
            },
            { error ->
                Log.e(TAG, "Error fetching Cerebras response: ${error.message}", error)
                val errorMsg = when (error) {
                    is com.android.volley.TimeoutError -> "Request timed out. Please try again."
                    else -> "Error connecting to server: ${error.message}"
                }
                handleError(errorMsg, queue)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Authorization" to "Bearer $apiKey")
            }
        }
        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            10000, // 10-second timeout
            maxRetries,
            1.0f // Backoff multiplier
        )
        queue.add(request)
    }

    private fun calculatePromptMetrics(requestMessages: JSONArray): Int {
        val promptBuilder = StringBuilder()
        for (i in 0 until requestMessages.length()) {
            val message = requestMessages.getJSONObject(i)
            val role = message.getString("role")
            val content = message.getString("content")
            promptBuilder.append("$role: $content\n")
        }
        val charCount = promptBuilder.length
        return (charCount / 4) // Rough estimate: ~4 chars per token
    }

    private fun handleError(errorMsg: String, queue: RequestQueue) {
        Log.d(TAG, "Handling error: $errorMsg")
        val errorMessage = Message(content = errorMsg, isUser = false)
        chatAdapter.addMessage(errorMessage)
        dbHelper.saveMessage(userEmail, subCategory, errorMsg, isUser = false)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun trimMessages(messages: List<Message>, maxTokens: Int = 8192): List<Message> {
        var totalChars = 0
        val trimmed = mutableListOf<Message>()
        for (message in messages.reversed()) {
            totalChars += message.content.length
            if (totalChars / 4 > maxTokens) break // Rough estimate: ~4 chars per token
            trimmed.add(0, message)
        }
        return trimmed
    }
}