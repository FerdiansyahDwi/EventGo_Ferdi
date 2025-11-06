package com.example.eventgo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventgo.adapter.EventAdapter
import com.example.eventgo.databinding.ActivityEventListBinding
import com.example.eventgo.usecase.EventUseCase
import com.example.eventgo.entity.Event

class EventListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventListBinding
    private lateinit var adapter: EventAdapter
    private val eventUseCase = EventUseCase()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi RecyclerView dan Adapter
        adapter = EventAdapter(this, mutableListOf())
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        // Panggil fungsi untuk load event
        loadEvents()
    }

    private fun loadEvents() {
        eventUseCase.getAllEvents { events ->
            if (events.isNotEmpty()) {
                adapter.updateData(events)
            } else {
                // Bisa kasih teks kalau data kosong
                binding.tvEmpty.text = "Belum ada event."
                binding.tvEmpty.visibility = android.view.View.VISIBLE
            }
        }
    }
}
