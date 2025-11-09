package com.example.eventgo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventgo.adapter.EventAdapter
import com.example.eventgo.databinding.ActivityEventListBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase

class EventListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventListBinding
    private lateinit var adapter: EventAdapter
    private val eventUseCase = EventUseCase()
    private val fullEventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Inisialisasi RecyclerView dan Adapter
        adapter = EventAdapter(this, mutableListOf())
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        // Panggil fungsi untuk load event
        loadEvents()
        // Panggil fungsi untuk setup search view
        setupSearch()
    }

    private fun loadEvents() {
        eventUseCase.getAllEvents { events ->
            if (events.isNotEmpty()) {
                // Simpan data asli ke fullEventList
                fullEventList.clear()
                fullEventList.addAll(events)

                // Tampilkan semua data saat pertama kali load
                adapter.updateData(events)
                binding.tvEmpty.visibility = View.GONE
                binding.rvEvents.visibility = View.VISIBLE
            } else {
                // Tampilkan teks jika data kosong dari database
                binding.tvEmpty.text = "Belum ada event."
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvEvents.visibility = View.GONE
            }
        }
    }

    // Fungsi baru untuk mengatur logika SearchView
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // Dipanggil saat user menekan "Enter" atau tombol search
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query)
                return true
            }

            // Dipanggil setiap kali user mengetik/menghapus huruf
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    // Fungsi baru untuk memfilter daftar berdasarkan query
    private fun filterList(query: String?) {
        val filteredList = mutableListOf<Event>()

        if (query.isNullOrEmpty()) {
            // Jika query kosong, tampilkan semua data
            filteredList.addAll(fullEventList)
        } else {
            // Jika ada query, filter data
            val searchQuery = query.lowercase().trim()
            for (event in fullEventList) {
                // Filter berdasarkan Judul ATAU Lokasi
                if (event.title.lowercase().contains(searchQuery) ||
                    event.location.lowercase().contains(searchQuery)) {
                    filteredList.add(event)
                }
            }
        }

        // Update adapter dengan data yang sudah difilter
        adapter.updateData(filteredList)

        // Atur tampilan "data kosong" jika hasil filter = 0
        if (filteredList.isEmpty()) {
            binding.tvEmpty.text = "Event tidak ditemukan."
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
        }
    }
}