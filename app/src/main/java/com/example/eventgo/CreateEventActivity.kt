package com.example.eventgo

import android.app.Activity
import android.app.DatePickerDialog
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
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay


class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private val eventUseCase = EventUseCase()
    private var imageUri: Uri? = null
    private val PICK_IMAGE = 1001
    private lateinit var mapView: MapView
    private var marker: Marker? = null
    private val client = OkHttpClient()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Konfigurasi Cloudinary
        val config = mapOf(
            "cloud_name" to "dqs99dmch",
            "api_key" to "871657475853684",
            "api_secret" to "nr_SAdat_KM83Xpcj13dlM7OFU4",
            "secure" to true
        )
        try {
            MediaManager.init(this, config)
        } catch (_: Exception) {}

        // ✅ Konfigurasi OSM Map
        Configuration.getInstance().userAgentValue = packageName
        mapView = binding.mapView
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        val startPoint = GeoPoint(-6.200000, 106.816666) // Default Jakarta
        mapView.controller.setCenter(startPoint)

        // Buat marker awal
        marker = Marker(mapView)
        marker?.position = startPoint
        marker?.title = "Pilih lokasi event"
        mapView.overlays.add(marker)

// Tambahkan listener klik pada map
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    moveMarker(p)
                    fetchAddressFromCoordinates(p.latitude, p.longitude)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))


        // Klik tombol pilih gambar
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        // ✅ Pilih tanggal pakai DatePicker
        binding.etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val selectedDate = String.format("%02d-%02d-%04d", d, m + 1, y)
                    binding.etDate.setText(selectedDate)
                },
                year, month, day
            )
            datePicker.show()
        }

        // ✅ Kalau user isi manual alamat → otomatis pindah marker
        binding.etLocation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val inputAddress = binding.etLocation.text.toString().trim()
                if (inputAddress.isNotEmpty()) {
                    fetchCoordinatesFromAddress(inputAddress)
                }
            }
        }

        // ✅ Tombol Simpan Event
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val date = binding.etDate.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val priceText = binding.etPrice.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || date.isEmpty() || location.isEmpty() || priceText.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageUri == null) {
                Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadImageAndSaveEvent()
        }
    }

    // ✅ Fungsi pindahkan marker ke lokasi baru
    private fun moveMarker(point: GeoPoint) {
        marker?.position = point
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    // ✅ Ambil alamat berdasarkan koordinat (Reverse Geocoding)
    private fun fetchAddressFromCoordinates(lat: Double, lon: Double) {
        Thread {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon"
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

    // ✅ Ambil koordinat dari input alamat (Forward Geocoding)
    private fun fetchCoordinatesFromAddress(address: String) {
        Thread {
            try {
                val url = "https://nominatim.openstreetmap.org/search?format=json&q=${address}"
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
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Alamat tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ✅ Upload Gambar Cloudinary
    private fun uploadImageAndSaveEvent() {
        MediaManager.get().upload(imageUri)
            .option("upload_preset", "unsigned_eventgo")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Toast.makeText(this@CreateEventActivity, "Mengupload gambar...", Toast.LENGTH_SHORT).show()
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url").toString()
                    saveEvent(imageUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(this@CreateEventActivity, "Upload gagal: ${error?.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    // ✅ Simpan event ke Firestore
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

    // ✅ Ambil hasil pilih gambar
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            binding.ivPreview.setImageURI(imageUri)
        }
    }
}
