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

        //val frameLayout = findViewById<FrameLayout>(R.id.root_frame_layout)


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