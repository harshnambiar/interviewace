package com.example.interviewace

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.interviewace.databinding.ActivityPostMainBinding
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.io.Resources


class PostMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostMainBinding

    private lateinit var userEmail: String
    private lateinit var broadCategoryType: String
    private lateinit var categories: List<SubCategory>
    private val TAG = "PostMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityPostMainBinding.inflate(layoutInflater)
        //setContentView(binding.root)
        setContentView(R.layout.activity_post_main)
        // Initialize SQLite
        //dbHelper = ChatDatabaseHelper(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: run {
            Log.e(TAG, "No Email Entered or Logged in")
            finish()
            return
        }

        broadCategoryType = intent.getStringExtra("BROAD_CATEGORY") ?: "unknown"

        if (broadCategoryType == "CAT"){
            // Existing chatbot setup
            categories = listOf(
                SubCategory("IIM Calcutta", R.drawable.iimc),
                SubCategory("IIM Bangalore", R.drawable.iimb),
                SubCategory("IIM Ahmedabad", R.drawable.iima),
                SubCategory("Indian School of Business", R.drawable.isb)

            )
        }
        else if (broadCategoryType == "GMAT"){
            categories = listOf(
                SubCategory("Wharton University", R.drawable.wu),
                SubCategory("Harvard University", R.drawable.hu),
                SubCategory("Stanford University", R.drawable.su),
                SubCategory("London Business School", R.drawable.lbs)

            )
        }
        else {
            categories = listOf(
            )
        }

        val mailVal = intent.getStringExtra("USER_EMAIL")?: "unknown"
        val tokenVal = intent.getStringExtra("USER_TOKEN")?: "unknown"
        val titleTextView = findViewById<TextView>(R.id.title_text_1)
        titleTextView.text = resources.getString(R.string.broad_category, broadCategoryType)
        val recyclerView = findViewById<RecyclerView>(R.id.category_recycler_view_1)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns for the grid
        recyclerView.adapter = SubCategoryAdapter(categories) { category ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CATEGORY_NAME", broadCategoryType)
                putExtra("SUB_CATEGORY_NAME", category.name)
                putExtra("USER_EMAIL",mailVal)
                putExtra("USER_TOKEN", tokenVal)
            }
            startActivity(intent)
        }

        recyclerView.addItemDecoration(GridSpacingDecoration(2, 20, true))


    }
}
