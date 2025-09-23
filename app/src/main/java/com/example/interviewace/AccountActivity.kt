package com.example.interviewace

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.interviewace.databinding.ActivityAccountBinding
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.util.concurrent.Executors


class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityAccountBinding
    private lateinit var credentialManager: CredentialManager
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var dbHelper: ChatDatabaseHelper
    private lateinit var tvEmail: TextView
    private lateinit var tvMessagesRemaining: TextView
    private lateinit var etUsername: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        dbHelper = ChatDatabaseHelper(this)

        // Initialize View Binding
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frameLayout = findViewById<FrameLayout>(R.id.root_frame_layout)





        // Set up logout button
        binding.logoutButton.setOnClickListener {
            signOut()
        }

        binding.unameButton.setOnClickListener {
            changeUsername()
        }

        // Initialize TextViews
        tvEmail = findViewById(R.id.tvEmail)
        tvMessagesRemaining = findViewById(R.id.tvMessagesRemaining)
        etUsername = findViewById<EditText>(R.id.uname_text)

        populateCurrentUsername()

        populateAccountDetails()
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Clear Google Sign-In session
        val clearCredentialRequest = ClearCredentialStateRequest()
        credentialManager.clearCredentialStateAsync(
            request = clearCredentialRequest,
            cancellationSignal = null,
            executor = executor,
            callback = object : androidx.credentials.CredentialManagerCallback<Void?, androidx.credentials.exceptions.ClearCredentialException> {
                override fun onResult(result: Void?) {
                    navigateToPreMainActivity()
                }

                override fun onError(e: androidx.credentials.exceptions.ClearCredentialException) {
                    Toast.makeText(this@AccountActivity, "Failed to clear Google session: ${e.message}", Toast.LENGTH_SHORT).show()
                    navigateToPreMainActivity()
                }
            }
        )
    }

    private fun navigateToPreMainActivity() {
        val intent = Intent(this, PreMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun changeUsername() {
        val newUname = etUsername.text.toString()
        ApiKeyProvider.saveUname(this, newUname)
        etUsername.text.clear()
        populateCurrentUsername()
    }

    private fun populateCurrentUsername(){
        val unameNow = ApiKeyProvider.getUname(this)
        if (unameNow != ""){
            etUsername.hint = unameNow
        }
        else {
            etUsername.hint = "Enter a Username"
        }
    }

    private fun populateAccountDetails() {
        val user = auth.currentUser
        if (user != null){
            val email = user.email ?: "unknown"
            val token = dbHelper.getUserToken(email) ?: "unknown"
            if (email == "unknown" || token == "unknown"){
                tvMessagesRemaining.setText(R.string.not_available)
                tvEmail.setText(R.string.not_available)
                Toast.makeText(this, "The current user details are not available.", Toast.LENGTH_SHORT).show()
            }
            else {
                tvEmail.text = email
                fetchMessagesRemaining(token)
            }
        }
        else {
            tvMessagesRemaining.setText(R.string.not_available)
            tvEmail.setText(R.string.not_available)
            Toast.makeText(this, "The current user details are not available.",Toast.LENGTH_SHORT).show()
        }
        //tvMessagesRemaining.text = "No idea lol"
        //tvEmail.text = auth.currentUser?.email ?: "No email"
    }

    private fun fetchMessagesRemaining(token: String){
        val queue = Volley.newRequestQueue(this)
        val url = "https://quantumsure.onrender.com/api/intuser/get"
        val apiKey = ApiKeyProvider.getApiKey(this)

        val dataObject = JSONObject().apply {
            put("token", token)
        }

        val jsonBody = JSONObject().apply {
            put("data", dataObject)
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            { response ->
                Log.d("AccountActivity", "API Success: $response")
                try {
                    val data = response.getInt("messages_remaining")
                    //val messagesRemaining = data.getString("messages_remaining")
                    tvMessagesRemaining.text = data.toString()
                }
                catch (e: Exception){
                    tvMessagesRemaining.setText(R.string.not_available)
                    Toast.makeText(this, "${e} failed to fetch message balance", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("AccountActivity", "API Error: ${error.message}, Network Response: ${error.networkResponse?.statusCode}")
                tvMessagesRemaining.setText(R.string.not_available)
                Toast.makeText(this,"Failed to Register the User: ${error.message}", Toast.LENGTH_SHORT).show()

            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("api_key" to apiKey)
            }
        }
        queue.add(request)
    }
}