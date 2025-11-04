package com.example.eventgo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventgo.databinding.ActivityCreateEventBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private val eventUseCase = EventUseCase()
    private var imageUri: Uri? = null
    private val PICK_IMAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        binding.btnSave.setOnClickListener {
            if (imageUri == null) {
                Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadImageAndSaveEvent()
        }
    }

    private fun uploadImageAndSaveEvent() {
        val storageRef = FirebaseStorage.getInstance().getReference("event_images/${UUID.randomUUID()}")
        val uploadTask = storageRef.putFile(imageUri!!)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                saveEvent(uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Upload gagal: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEvent(imageUrl: String) {
        val event = Event(
            title = binding.etTitle.text.toString(),
            description = binding.etDescription.text.toString(),
            date = binding.etDate.text.toString(),
            location = binding.etLocation.text.toString(),
            price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0,
            imageUrl = imageUrl
        )

        eventUseCase.addEvent(event, {
            Toast.makeText(this, "Event berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            finish()
        }, {
            Toast.makeText(this, "Gagal menambah event: ${it.message}", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            binding.ivPreview.setImageURI(imageUri)
        }
    }
}