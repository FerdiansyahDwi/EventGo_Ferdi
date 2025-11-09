package com.example.eventgo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eventgo.databinding.ActivityUserHomeBinding
import com.google.firebase.auth.FirebaseAuth

class UserHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnViewEvents.setOnClickListener {
            startActivity(Intent(this, EventListActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()

            SessionManager.clearSession()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}