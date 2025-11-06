package com.example.eventgo

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eventgo.databinding.ActivityEditEventBinding
import com.squareup.picasso.Picasso
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Calendar
import android.location.Geocoder
import kotlinx.coroutines.*
import java.util.Locale
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore

class EditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditEventBinding
    private lateinit var mapView: MapView
    private lateinit var marker: Marker
    private var imageUri: Uri? = null
    private val calendar = Calendar.getInstance()
    private val geocoder by lazy { Geocoder(this, Locale.getDefault()) }
    private val firestore = FirebaseFirestore.getInstance()
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().userAgentValue = packageName
        mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(-6.914744, 107.609810)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        marker = Marker(mapView).apply {
            position = startPoint
            title = "Pilih lokasi event"
        }
        mapView.overlays.add(marker)

        // Ambil ID event dari Intent
        eventId = intent.getStringExtra("event_id")
        if (eventId == null) {
            Toast.makeText(this, "ID event tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ambil data lama dari Firestore
        loadEventData()

        // Map listener
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                e?.let {
                    val proj = mapView!!.projection
                    val point = proj.fromPixels(it.x.toInt(), it.y.toInt()) as GeoPoint
                    moveMarker(point)
                    fetchAddressFromCoords(point)
                }
                return true
            }
        })

        // Pilih gambar baru
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        // DatePicker
        binding.etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                binding.etDate.setText("$d-${m + 1}-$y")
            }, year, month, day).show()
        }

        // Update lokasi manual
        setLocationFieldWatcher(binding.etLocation)

        // Tombol Update
        binding.btnUpdate.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadEventData() {
        firestore.collection("events").document(eventId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    val date = doc.getString("date") ?: ""
                    val location = doc.getString("location") ?: ""
                    val price = doc.getDouble("price") ?: 0.0
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val lat = doc.getDouble("latitude") ?: -6.914744
                    val lng = doc.getDouble("longitude") ?: 107.609810

                    // Tampilkan di form
                    binding.etEventName.setText(title)
                    binding.etDescription.setText(description)
                    binding.etDate.setText(date)
                    binding.etLocation.setText(location)
                    binding.etPrice.setText(price.toString())

                    // Tampilkan gambar lama
                    if (imageUrl.isNotEmpty()) {
                        Picasso.get().load(imageUrl).into(binding.ivPreview)
                    }

                    // Posisikan marker
                    val point = GeoPoint(lat, lng)
                    moveMarker(point)
                } else {
                    Toast.makeText(this, "Data event tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data event", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        val title = binding.etEventName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val lat = marker.position.latitude
        val lng = marker.position.longitude

        val updatedData = hashMapOf(
            "title" to title,
            "description" to description,
            "date" to date,
            "location" to location,
            "price" to price,
            "latitude" to lat,
            "longitude" to lng
        )

        firestore.collection("events").document(eventId!!)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Event berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui event!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun moveMarker(point: GeoPoint) {
        marker.position = point
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    private fun fetchAddressFromCoords(point: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addressLine = addresses[0].getAddressLine(0)
                    withContext(Dispatchers.Main) {
                        binding.etLocation.setText(addressLine)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setLocationFieldWatcher(editText: EditText) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val locationText = editText.text.toString()
                if (locationText.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val addresses = geocoder.getFromLocationName(locationText, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val loc = addresses[0]
                                val newPoint = GeoPoint(loc.latitude, loc.longitude)
                                withContext(Dispatchers.Main) {
                                    moveMarker(newPoint)
                                    mapView.controller.animateTo(newPoint)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && data != null && data.data != null) {
            imageUri = data.data
            binding.ivPreview.setImageURI(imageUri)
        }
    }
}
