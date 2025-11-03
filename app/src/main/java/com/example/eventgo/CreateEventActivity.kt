package com.example.eventgo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventgo.databinding.ActivityCreateEventBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private val eventUseCase = EventUseCase()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val desc = binding.etDescription.text.toString()
            val date = binding.etDate.text.toString()
            val location = binding.etLocation.text.toString()
            val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Harap isi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val event = Event(title = title, description = desc, date = date, location = location, price = price)
            eventUseCase.addEvent(event, {
                Toast.makeText(this, "Event berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            }, {
                Toast.makeText(this, "Gagal menambah event: ${it.message}", Toast.LENGTH_SHORT).show()
            })
        }
    }
}