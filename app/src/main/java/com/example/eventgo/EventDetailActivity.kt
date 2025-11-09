package com.example.eventgo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eventgo.databinding.ActivityEventDetailBinding
import com.example.eventgo.entity.Event
import com.google.firebase.database.FirebaseDatabase

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("eventId")
        loadEventDetail()

        // Tombol "Order" — bisa diubah nanti kalau ada fitur checkout
        binding.btnOrder.setOnClickListener {
            // sementara hanya tampilkan toast atau buat logika lanjutannya nanti
        }
    }

    private fun loadEventDetail() {
        val dbRef = FirebaseDatabase.getInstance().getReference("events").child(eventId ?: return)
        dbRef.get().addOnSuccessListener {
            val event = it.getValue(Event::class.java)
            event?.let {
                binding.tvTitle.text = it.title
                binding.tvDescription.text = it.description
                binding.tvDate.text = "Tanggal: ${it.date}"
                binding.tvLocation.text = "Lokasi: ${it.location}"
                binding.tvPrice.text = "Harga: Rp${it.price}"

                Glide.with(this)
                    .load(it.imageUrl)
                    .into(binding.ivEvent)
            }
        }
    }
}
