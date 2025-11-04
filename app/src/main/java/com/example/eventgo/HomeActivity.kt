package com.example.eventgo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnViewEvents = findViewById<Button>(R.id.btnViewEvents)
        val btnAddEvent = findViewById<Button>(R.id.btnAddEvent)

        btnViewEvents.setOnClickListener {
            val intent = Intent(this, EventListActivity::class.java)
            startActivity(intent)
        }

        btnAddEvent.setOnClickListener {
            val intent = Intent(this, CreateEventActivity::class.java)
            startActivity(intent)
        }
    }
}
