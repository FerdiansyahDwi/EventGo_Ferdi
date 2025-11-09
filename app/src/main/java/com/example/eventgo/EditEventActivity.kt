package com.example.eventgo

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.eventgo.databinding.ActivityEditEventBinding
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Calendar

class EditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditEventBinding
    private lateinit var mapView: MapView
    private lateinit var marker: Marker
    private var imageUri: Uri? = null
    private val calendar = Calendar.getInstance()
    private val eventUseCase = EventUseCase()
    private val client = OkHttpClient()
    private var eventId: String? = null
    private var imageUrlLama: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val config = mapOf(
            "cloud_name" to "dqs99dmch",
            "api_key" to "871657475853684",
            "api_secret" to "nr_SAdat_KM83Xpcj13dlM7OFU4",
            "secure" to true
        )
        try {
            MediaManager.init(this, config)
        } catch (_: Exception) {}

        // Konfigurasi OSM Map
        Configuration.getInstance().userAgentValue = packageName
        mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(-6.914744, 107.609810) // Default Bandung
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        marker = Marker(mapView).apply {
            position = startPoint
            title = "Pilih lokasi event"
        }
        mapView.overlays.add(marker)

        // Ambil ID event dari Intent
        eventId = intent.getStringExtra("event_id")
        if (eventId.isNullOrEmpty()) {
            Toast.makeText(this, "ID event tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ambil data lama dari Realtime Database
        loadEventData()

        // Map listener (Ganti ke MapEventsReceiver agar konsisten)
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    moveMarker(p)
                    fetchAddressFromCoords(p) // Gunakan fungsi Nominatim
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

        // Pilih gambar baru
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 100) // Request code 100
        }

        // DatePicker
        binding.etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                binding.etDate.setText(String.format("%02d-%02d-%04d", d, m + 1, y))
            }, year, month, day).show()
        }

        // Update lokasi manual
        setLocationFieldWatcher(binding.etLocation)

        // Tombol Update
        binding.btnUpdate.setOnClickListener {
            saveChanges()
        }
    }

    // Load data dari EventUseCase (Realtime Database)
    private fun loadEventData() {
        eventId?.let { id ->
            eventUseCase.getEventById(id, { event ->
                event?.let {
                    // Simpan URL gambar lama
                    imageUrlLama = it.imageUrl

                    // Tampilkan di form
                    binding.etEventName.setText(it.title)
                    binding.etDescription.setText(it.description)
                    binding.etDate.setText(it.date)
                    binding.etLocation.setText(it.location)
                    val priceAsLong = it.price.toLong()
                    binding.etPrice.setText(priceAsLong.toString())

                    // Tampilkan gambar lama
                    if (it.imageUrl.isNotEmpty()) {
                        Picasso.get().load(it.imageUrl).into(binding.ivPreview)
                    }

                    // Posisikan marker berdasarkan lokasi yang disimpan
                    searchLocationAndUpdateMarker(it.location) // Panggil fungsi yang sebelumnya "merah"
                } ?: Toast.makeText(this, "Data event tidak ditemukan", Toast.LENGTH_SHORT).show()
            }, {
                Toast.makeText(this, "Gagal memuat data event: ${it.message}", Toast.LENGTH_SHORT).show()
            })
        }
    }

    // Fungsi ini adalah yang "merah" (hilang) sebelumnya
    private fun searchLocationAndUpdateMarker(address: String) {
        if (address.isNotEmpty()) {
            // Panggil fungsi geocoding, set 'isInitialLoad' ke true
            // agar tidak menampilkan Toast "Alamat tidak ditemukan" jika gagal saat load
            fetchCoordinatesFromAddress(address, true)
        }
    }

    // Logika utama untuk menyimpan perubahan
    private fun saveChanges() {
        // Ambil data dari views
        val title = binding.etEventName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val priceText = binding.etPrice.text.toString().filter { it.isDigit() }
        val price = priceText.toDoubleOrNull() ?: 0.0

        // Validasi input
        if (title.isEmpty() || description.isEmpty() || date.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Cek apakah user memilih gambar baru
        if (imageUri != null) {
            // Ada gambar baru -> Upload dulu
            uploadImageAndUpdateEvent(title, description, date, location, price)
        } else {
            // Tidak ada gambar baru -> Langsung update data dengan imageUrl lama
            val updatedEvent = Event(
                id = eventId,
                title = title,
                description = description,
                date = date,
                location = location,
                price = price,
                imageUrl = imageUrlLama ?: "" // Pakai URL gambar yang lama
            )
            updateEventInDatabase(updatedEvent)
        }
    }

    // Fungsi untuk upload gambar baru (jika ada)
    private fun uploadImageAndUpdateEvent(title: String, description: String, date: String, location: String, price: Double) {
        MediaManager.get().upload(imageUri)
            .option("upload_preset", "unsigned_eventgo")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Toast.makeText(this@EditEventActivity, "Mengupload gambar baru...", Toast.LENGTH_SHORT).show()
                }
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val newImageUrl = resultData?.get("secure_url").toString()
                    val updatedEvent = Event(
                        id = eventId,
                        title = title,
                        description = description,
                        date = date,
                        location = location,
                        price = price,
                        imageUrl = newImageUrl
                    )
                    updateEventInDatabase(updatedEvent)
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(this@EditEventActivity, "Upload gambar gagal: ${error?.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    // Fungsi final untuk update data ke EventUseCase
    private fun updateEventInDatabase(event: Event) {
        eventUseCase.updateEvent(event, {
            Toast.makeText(this, "Event berhasil diperbarui", Toast.LENGTH_SHORT).show()
            finish()
        }, {
            Toast.makeText(this, "Gagal memperbarui event: ${it.message}", Toast.LENGTH_SHORT).show()
        })
    }

    // Fungsi pindah marker
    private fun moveMarker(point: GeoPoint) {
        marker.position = point
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    // Ambil alamat dari koordinat (Reverse Geocoding - Nominatim)
    private fun fetchAddressFromCoords(point: GeoPoint) {
        Thread {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${point.latitude}&lon=${point.longitude}"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "EventGoApp")
                    .build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body!!.string())
                val address = json.optString("display_name", "Lokasi tidak diketahui")
                runOnUiThread {
                    binding.etLocation.setText(address)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Gagal mengambil alamat", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // Ambil koordinat dari input alamat (Forward Geocoding - Nominatim)
    private fun fetchCoordinatesFromAddress(address: String, isInitialLoad: Boolean = false) {
        Thread {
            try {
                val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "EventGoApp")
                    .build()
                val response = client.newCall(request).execute()
                val jsonArray = JSONObject("{\"data\":${response.body!!.string()}}").optJSONArray("data")

                if (jsonArray != null && jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    runOnUiThread {
                        val point = GeoPoint(lat, lon)
                        moveMarker(point)
                        mapView.controller.setCenter(point)
                    }
                } else if (!isInitialLoad) { // Jangan tampilkan toast jika ini adalah saat load data
                    runOnUiThread {
                        Toast.makeText(this, "Alamat tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!isInitialLoad) { // Jangan tampilkan toast jika ini adalah saat load data
                    runOnUiThread {
                        Toast.makeText(this, "Gagal mencari koordinat", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    // Update listener untuk EditText lokasi
    private fun setLocationFieldWatcher(editText: EditText) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val locationText = editText.text.toString()
                if (locationText.isNotEmpty()) {
                    fetchCoordinatesFromAddress(locationText) // Panggil versi Nominatim
                }
            }
        }
    }

    // Ambil hasil pilih gambar
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            binding.ivPreview.setImageURI(imageUri) // Tampilkan gambar baru di preview
        }
    }
}