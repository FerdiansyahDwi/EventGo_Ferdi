package com.example.eventgo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eventgo.databinding.ActivityEventDetailBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase // ✅ Import EventUseCase
import java.util.Locale // ✅ Import Locale untuk format harga

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private var eventId: String? = null
    private var currentEvent: Event? = null
    private val eventUseCase = EventUseCase()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("event_id")

        // Panggil fungsi load data
        loadEventDetail()

        if (SessionManager.getUserRole() == "admin") {
            binding.btnOrder.visibility = View.GONE
        } else {
            binding.btnOrder.visibility = View.VISIBLE
        }

        // Tombol "Order"
        binding.btnOrder.setOnClickListener {
            currentEvent?.let { event ->
                val intent = Intent(this, OrderTicketActivity::class.java)

                intent.putExtra("EVENT_ID", event.id)
                intent.putExtra("EVENT_NAME", event.title)
                intent.putExtra("EVENT_DATE", event.date)
                intent.putExtra("EVENT_LOCATION", event.location)
                intent.putExtra("EVENT_PRICE", event.price)
                intent.putExtra("EVENT_IMAGE_URL", event.imageUrl)

                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

            } ?: Toast.makeText(this, "Data event belum selesai dimuat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadEventDetail() {
        eventId?.let { id ->
            eventUseCase.getEventById(id, { event ->
                event?.let {
                    binding.tvTitle.text = it.title
                    binding.tvDescription.text = it.description
                    binding.tvDate.text = "Tanggal: ${it.date}"
                    binding.tvLocation.text = "Lokasi: ${it.location}"

                    val priceAsLong = it.price.toLong()
                    val formattedPrice = String.format(Locale("in", "ID"), "%,d", priceAsLong)
                    binding.tvPrice.text = "Harga: Rp${formattedPrice}"

                    this.currentEvent = it

                    // Muat gambar
                    Glide.with(this)
                        .load(it.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.ivEvent)
                }
            }, { exception ->
                Toast.makeText(this, "Gagal memuat event: ${exception.message}", Toast.LENGTH_SHORT).show()
            })
        } ?: run {
            Toast.makeText(this, "ID Event tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}