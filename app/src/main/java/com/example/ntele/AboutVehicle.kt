package com.example.ntele

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ntele.fragments.HomeFragment

class AboutVehicle : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_vehicle)

        val continue_button = findViewById<Button>(R.id.continue_button)
        val name = intent.getStringExtra("name")
        continue_button.setOnClickListener{
            val intent = Intent(this, BottomNavigationDrawer::class.java)
            intent.putExtra("name",name)
            Log.d(" name Vehicle","" + name)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

    }
}