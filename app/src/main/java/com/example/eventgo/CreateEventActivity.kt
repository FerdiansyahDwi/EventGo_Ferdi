package com.example.eventgo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eventgo.databinding.ActivityCreateEventBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private val eventUseCase = EventUseCase()
    private var imageUri: Uri? = null
    private val PICK_IMAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Inisialisasi manual Cloudinary
        val config = mapOf(
            "cloud_name" to "dqs99dmch", // ganti dengan cloud name kamu
            "api_key" to "871657475853684",
            "api_secret" to "nr_SAdat_KM83Xpcj13dlM7OFU4",
            "secure" to true
        )

        try {
            MediaManager.init(this, config)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Pilih gambar
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        // Tombol simpan
        binding.btnSave.setOnClickListener {
            if (imageUri == null) {
                Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadImageAndSaveEvent()
        }
    }

    // ✅ Upload pakai unsigned preset
    private fun uploadImageAndSaveEvent() {
        MediaManager.get().upload(imageUri)
            .option("upload_preset", "unsigned_eventgo") // ganti sesuai preset kamu
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Toast.makeText(this@CreateEventActivity, "Mengupload gambar...", Toast.LENGTH_SHORT).show()
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url").toString()
                    Toast.makeText(this@CreateEventActivity, "Upload berhasil! URL: $imageUrl", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("Cloudinary", "Image URL: $imageUrl")

                    saveEvent(imageUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    android.util.Log.e("Cloudinary", "Upload error: ${error?.description}")
                    Toast.makeText(
                        this@CreateEventActivity,
                        "Upload gagal: ${error?.description}",
                        Toast.LENGTH_LONG
                    ).show()
                }


                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
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
            android.util.Log.d("EventGo", "Event berhasil disimpan ke backend")
            finish()
        }, {
            Toast.makeText(this, "Gagal menambah event: ${it.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("EventGo", "Gagal simpan event: ${it.message}")
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
