package com.example.interviewace


import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.interviewace.databinding.ActivityAboutBinding



class AboutActivity : AppCompatActivity(){

    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frameLayout = findViewById<FrameLayout>(R.id.root_frame_layout)





    }
}