package com.example.interviewace

import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.interviewace.databinding.ActivityMainBinding

class PreMainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var dbHelper: ChatDatabaseHelper
    private lateinit var request: GetCredentialRequest
    private val executor = Executors.newSingleThreadExecutor()
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* No-op */ }
    private var isRegistering = false // Flag to prevent multiple registrations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_main)

        val frameLayout = findViewById<FrameLayout>(R.id.root_frame_layout)

        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeightPx = displayMetrics.heightPixels
        val screenWidthPx = displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val intervalDp = 125
        val intervalDeltaDp = 10
        val startMarginDp = 25
        val endMarginDp = 25
        val heartSizeDp = 32
        val intervalPx = (intervalDp * density).toInt()
        val intervalDeltaPx = (intervalDeltaDp * density).toInt()
        val startMarginPx = (startMarginDp * density).toInt()
        val endMarginPx = (endMarginDp * density).toInt()
        val heartSizePx = (heartSizeDp * density).toInt()

        val numIntervals = (screenHeightPx / intervalPx) + 1

        for (i in 0 until numIntervals){
            val yPosition = i * intervalPx
            val yDelta = yPosition + intervalDeltaPx
            val startHeart = ImageView(this).apply {
                setImageResource(R.drawable.ic_heart)
                alpha = 0.3f
                layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                    setMargins(startMarginPx, yPosition, 0, 0)
                }
            }
            val middleHeart = ImageView(this).apply {
                setImageResource(R.drawable.ic_heart)
                alpha = 0.3f
                layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                    setMargins((screenWidthPx - heartSizePx)/2, yDelta, 0, 0)
                }
            }
            val endHeart = ImageView(this).apply {
                setImageResource(R.drawable.ic_heart)
                alpha = 0.3f
                layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                    setMargins(0, yPosition, endMarginPx, 0)
                    gravity = Gravity.END
                }
            }

            frameLayout.addView(startHeart)
            frameLayout.addView(middleHeart)
            frameLayout.addView((endHeart))
        }

        // Initialize Firebase Auth and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        dbHelper = ChatDatabaseHelper(this)

        // Configure Google Sign-In
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Handle Google Sign-In button click
        findViewById<Button>(R.id.google_sign_in_button).setOnClickListener {
            if (!isRegistering) {
                val cancellationSignal = CancellationSignal()
                credentialManager.getCredentialAsync(
                    context = this,
                    request = request,
                    cancellationSignal = cancellationSignal,
                    executor = executor,
                    callback = object : androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                        override fun onResult(result: GetCredentialResponse) {
                            handleSignIn(result)
                        }

                        override fun onError(e: GetCredentialException) {
                            showToast("Sign-in Failed: ${e.message}")
                        }
                    }
                )
            }
        }

        findViewById<Button>(R.id.flush).setOnClickListener {
            dbHelper.flush()
            showToast("Flushed All")
        }
    }

    override fun onStart() {
        super.onStart()
        // Use AuthStateListener to check authentication state
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null && !isRegistering) {
                val email = firebaseAuth.currentUser?.email ?: "unknown"
                val tokenValue = dbHelper.getUserToken(email)
                if (email == "unknown" || tokenValue == null) {
                    showToast("Failed to login user, please flush and retry again.")
                    // Stay in PreMainActivity
                } else {
                    startMainActivity(tokenValue)
                    finish()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Remove listener to prevent memory leaks
        auth.removeAuthStateListener { /* No-op */ }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is GoogleIdTokenCredential -> {
                val idToken = credential.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    showToast("No ID token received")
                }
            }
            else -> {
                showToast("Unexpected credential type")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        if (isRegistering) return // Prevent re-entry
        isRegistering = true

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val email = auth.currentUser?.email ?: "unknown"
                    val existingToken = dbHelper.getUserToken(email)
                    if (existingToken != null) {
                        registerUserWithApi(email, existingToken) { success ->
                            if (success) {
                                startMainActivity(existingToken)
                                finish()
                            }
                            isRegistering = false
                        }
                    } else {
                        val tokenNew = generateUniqueToken(email)
                        dbHelper.saveUserToken(email, tokenNew)
                        registerUserWithApi(email, tokenNew) { success ->
                            if (success) {
                                startMainActivity(tokenNew)
                                finish()
                            }
                            isRegistering = false
                        }
                    }
                } else {
                    showToast("Authentication failed: ${task.exception?.message}")
                    isRegistering = false
                }
            }
    }

    private fun startMainActivity(token: String) {
        val email = auth.currentUser?.email ?: "unknown"
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_EMAIL", email)
            putExtra("USER_TOKEN", token)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun generateUniqueToken(email: String): String {
        val epochTime = System.currentTimeMillis().toString()
        val uuid = UUID.randomUUID().toString()
        return "$email-$epochTime-$uuid"
    }

    private fun registerUserWithApi(email: String, token: String, callback: (Boolean) -> Unit) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://quantumsure.onrender.com/api/chatuser/create"
        val apiKey = ApiKeyProvider.getApiKey(this)

        val dataObject = JSONObject().apply {
            put("email", email)
            put("token", token)
            put("plan", 100)
            put("userIntro", "")
        }

        val jsonBody = JSONObject().apply {
            put("data", dataObject)
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            { response ->
                Log.d("PreMainActivity", "API Success: $response")
                callback(true)
            },
            { error ->
                Log.e("PreMainActivity", "API Error: ${error.message}, Network Response: ${error.networkResponse?.statusCode}")
                showToast("Failed to Register the User: ${error.message ?: "Unknown error"}")
                callback(false)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("api_key" to apiKey)
            }
        }
        queue.add(request)
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}