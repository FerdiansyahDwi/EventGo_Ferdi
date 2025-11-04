package com.example.eventgo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventgo.databinding.ActivityEditEventBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase
import com.google.firebase.database.FirebaseDatabase

class EditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditEventBinding
    private val eventUseCase = EventUseCase()
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("EVENT_ID")

        loadEventData()

        binding.btnUpdate.setOnClickListener {
            val updatedEvent = Event(
                id = eventId,
                title = binding.etTitle.text.toString(),
                description = binding.etDescription.text.toString(),
                date = binding.etDate.text.toString(),
                location = binding.etLocation.text.toString(),
                price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
            )

            eventUseCase.updateEvent(updatedEvent, {
                Toast.makeText(this, "Event berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }, {
                Toast.makeText(this, "Gagal memperbarui: ${it.message}", Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun loadEventData() {
        val dbRef = FirebaseDatabase.getInstance().getReference("events").child(eventId ?: return)
        dbRef.get().addOnSuccessListener {
            val event = it.getValue(Event::class.java)
            event?.let {
                binding.etTitle.setText(it.title)
                binding.etDescription.setText(it.description)
                binding.etDate.setText(it.date)
                binding.etLocation.setText(it.location)
                binding.etPrice.setText(it.price.toString())
            }
        }
    }
}