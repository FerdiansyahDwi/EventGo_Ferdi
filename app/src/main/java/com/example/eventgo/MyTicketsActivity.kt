package com.example.eventgo

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventgo.adapter.OrderAdapter
import com.example.eventgo.databinding.ActivityMyTicketsBinding
import com.example.eventgo.usecase.OrderUseCase
import com.google.firebase.auth.FirebaseAuth

class MyTicketsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyTicketsBinding
    private lateinit var adapter: OrderAdapter
    private val orderUseCase = OrderUseCase()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTicketsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true) // Tampilkan judul

        auth = FirebaseAuth.getInstance()

        adapter = OrderAdapter(this, listOf())
        binding.rvMyTickets.layoutManager = LinearLayoutManager(this)
        binding.rvMyTickets.adapter = adapter

        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.recyclerview_animation_fade_in)
        binding.rvMyTickets.layoutAnimation = animation

        loadMyTickets()
    }

    private fun loadMyTickets() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Gagal memuat tiket, silakan login ulang", Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        }

        orderUseCase.getOrdersForUser(userId) { orders ->
            if (orders.isNotEmpty()) {
                adapter.updateData(orders)
                binding.rvMyTickets.visibility = View.VISIBLE
                binding.tvEmptyTickets.visibility = View.GONE
                binding.rvMyTickets.scheduleLayoutAnimation()
            } else {
                binding.rvMyTickets.visibility = View.GONE
                binding.tvEmptyTickets.visibility = View.VISIBLE
            }
        }
    }
}