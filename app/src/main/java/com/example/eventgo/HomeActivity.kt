package com.example.eventgo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eventgo.databinding.ActivityHomeBinding // Pastikan import ini ada
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi auth
        auth = FirebaseAuth.getInstance()

        // Listener untuk Tombol Lihat Event
        binding.btnViewEvents.setOnClickListener {
            startActivity(Intent(this, EventListActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Listener untuk Tombol Tambah Event
        binding.btnAddEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Listener untuk Tombol Logout
        binding.btnLogout.setOnClickListener {
            // Logout dari Firebase
            auth.signOut()

            // Hapus session peran (Admin/User)
            SessionManager.clearSession()

            // Pindah kembali ke halaman Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}