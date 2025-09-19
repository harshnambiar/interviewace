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
                alpha = 0.2f
                layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                    setMargins(startMarginPx, yPosition, 0, 0)
                }
            }
            val middleHeart = ImageView(this).apply {
                setImageResource(R.drawable.ic_heart)
                alpha = 0.2f
                layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                    setMargins((screenWidthPx - heartSizePx)/2, yDelta, 0, 0)
                }
            }
            val endHeart = ImageView(this).apply {
                setImageResource(R.drawable.ic_heart)
                alpha = 0.2f
                layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                    setMargins(0, yPosition, endMarginPx, 0)
                    gravity = Gravity.END
                }
            }

            frameLayout.addView(startHeart)
            frameLayout.addView(middleHeart)
            frameLayout.addView((endHeart))
        }

    }
}