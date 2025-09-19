package com.example.interviewace

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.interviewace.databinding.ActivityMainBinding
//import com.example.myapplication.premain.PreMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.scottyab.rootbeer.RootBeer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBeer = RootBeer(this)
        if (rootBeer.isRooted) {
            Toast.makeText(this, "This app can NOT run on rooted devices.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize Firebase Auth and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frameLayout = binding.frameLayout
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



        binding.menuButton.setOnClickListener{
            toggleDropdownMenu()
        }

        binding.planButton.setOnClickListener{
            navigateToPlanActivity()
            toggleDropdownMenu()
        }

        binding.aboutButton.setOnClickListener{
            navigateToAboutActivity()
            toggleDropdownMenu()
        }


        binding.accountButton.setOnClickListener{
            navigateToAccountActivity()
            toggleDropdownMenu()
        }
        // Set up logout button
        findViewById<Button>(R.id.account_button)?.setOnClickListener {
            navigateToAccountActivity()
        }
        findViewById<Button>(R.id.plan_button)?.setOnClickListener {
            navigateToPlanActivity()
        }

        findViewById<Button>(R.id.about_button)?.setOnClickListener {
            navigateToAboutActivity()
        }


        // Existing chatbot setup
        val categories = listOf(
            BroadCategory("GMAT", R.drawable.gmat),
            BroadCategory("CAT", R.drawable.cat),
            BroadCategory("UPSC", R.drawable.upsc),
            BroadCategory("SSC", R.drawable.ssc)

        )
        val mailVal = intent.getStringExtra("USER_EMAIL")?: "unknown"
        val tokenVal = intent.getStringExtra("USER_TOKEN")?: "unknown"
        val recyclerView = findViewById<RecyclerView>(R.id.chatbot_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns for the grid
        recyclerView.adapter = BroadCategoryAdapter(categories) { category ->
            val intent = Intent(this, PostMainActivity::class.java).apply {
                putExtra("CHATBOT_NAME", category.name)
                putExtra("USER_EMAIL",mailVal)
                putExtra("USER_TOKEN", tokenVal)
            }
            startActivity(intent)
        }
        recyclerView.addItemDecoration(GridSpacingDecoration(2, 20, true))
    }

    private fun toggleDropdownMenu(){
        val dropDownMenu = binding.dropdownMenu
        if (dropDownMenu.visibility == View.VISIBLE){
            dropDownMenu.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction({dropDownMenu.visibility = View.GONE})
                .start()
        }
        else {
            dropDownMenu.alpha = 0f
            dropDownMenu.visibility = View.VISIBLE
            dropDownMenu.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Clear Google Sign-In session (optional but recommended)
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
                    Toast.makeText(this@MainActivity, "Failed to clear Google session: ${e.message}", Toast.LENGTH_SHORT).show()
                    navigateToPreMainActivity()
                }
            }
        )
    }

    private fun navigateToPreMainActivity() {
        val intent = Intent(this, PreMainActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun navigateToAboutActivity() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPlanActivity() {
        val intent = Intent(this, PlanActivity::class.java)
        startActivity(intent)
    }



    private fun navigateToAccountActivity() {
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)
    }
}

class GridSpacingDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}