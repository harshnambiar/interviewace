package com.example.interviewace

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class PlanActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_plan)

        val frameLayout = findViewById<FrameLayout>(R.id.root_frame_layout)
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeightPx = displayMetrics.heightPixels
        val screenWidthPx = displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val intervalDp = 150
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

        // Existing chatbot setup
        val plans = listOf(
            Plan("Basic Plan", R.drawable.basic),
            Plan("Bronze Plan", R.drawable.bronze1),
            Plan("Silver Plan", R.drawable.silver1),
            Plan("Gold Plan", R.drawable.gold1),
            Plan("Diamond Plan", R.drawable.diamond),
            Plan("Ruby Plan", R.drawable.ruby),
        )
        val recyclerView = findViewById<RecyclerView>(R.id.chatbot_recycler_view_1)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns for the grid
        recyclerView.adapter = PlanAdapter(plans) { plan ->
            val intent = Intent(this, PlanActivity::class.java).apply {
                putExtra("PLAN_NAME", plan.name)
            }
            startActivity(intent)
        }
        recyclerView.addItemDecoration(GridSpacingDecoration(2, 20, true))
    }
}