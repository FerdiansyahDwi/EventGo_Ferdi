package com.example.eventgo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eventgo.databinding.ActivityOrderTicketBinding
import com.example.eventgo.entity.Order
import com.example.eventgo.usecase.OrderUseCase
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class OrderTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderTicketBinding
    private val orderUseCase = OrderUseCase()
    private lateinit var auth: FirebaseAuth

    // Variabel untuk menyimpan info event
    private var eventId: String? = null
    private var eventName: String? = null
    private var eventDate: String? = null
    private var eventLocation: String? = null
    private var eventImageUrl: String? = null
    private var eventPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Ambil data event dari Intent
        eventId = intent.getStringExtra("EVENT_ID")
        eventName = intent.getStringExtra("EVENT_NAME")
        eventDate = intent.getStringExtra("EVENT_DATE")
        eventLocation = intent.getStringExtra("EVENT_LOCATION")
        eventPrice = intent.getDoubleExtra("EVENT_PRICE", 0.0)
        eventImageUrl = intent.getStringExtra("EVENT_IMAGE_URL")

        // Tampilkan data event di UI
        binding.tvEventName.text = eventName
        binding.tvEventDate.text = eventDate
        val formattedPrice = String.format(Locale("in", "ID"), "%,d", eventPrice.toLong())
        binding.tvEventPrice.text = "Rp$formattedPrice / tiket"

        // Isi nama pemesan pertama (statis)
        binding.etName.setText(auth.currentUser?.displayName ?: "")

        // Hitung total harga awal
        calculateTotalPrice()
        // Tambahkan listener untuk update total harga DAN field nama
        binding.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTotalPrice()

                // Update field nama berdasarkan jumlah
                val quantity = s.toString().toIntOrNull() ?: 0
                updateNameFields(quantity)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tombol Submit Order
        binding.btnSubmitOrder.setOnClickListener {
            submitOrder()
        }
    }

    private fun calculateTotalPrice() {
        val quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 0
        val total = quantity * eventPrice
        val formattedTotal = String.format(Locale("in", "ID"), "%,d", total.toLong())
        binding.tvTotalPrice.text = "Total: Rp$formattedTotal"
    }

    private fun updateNameFields(quantity: Int) {
        val container = binding.nameFieldsContainer

        val extraFieldsNeeded = if (quantity > 1) quantity - 1 else 0
        val finalExtraFields = if (extraFieldsNeeded > 9) 9 else extraFieldsNeeded

        val currentExtraFields = container.childCount

        if (finalExtraFields == currentExtraFields) return // Tidak ada perubahan

        if (finalExtraFields > currentExtraFields) {
            for (i in currentExtraFields until finalExtraFields) {

                val newField = EditText(this)

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.topMargin = (16 * resources.displayMetrics.density).toInt() // 16dp
                newField.layoutParams = layoutParams

                newField.setBackgroundResource(R.drawable.rounded_edittext_background)
                newField.setTextColor(getColor(R.color.text_dark)) // <-- Ini yang penting
                newField.setHintTextColor(getColor(R.color.soft_blue_accent)) // <-- Ini yang penting

                val paddingVertical = (12 * resources.displayMetrics.density).toInt()
                val paddingHorizontal = (16 * resources.displayMetrics.density).toInt()
                newField.setPadding(
                    paddingHorizontal,
                    paddingVertical,
                    paddingHorizontal,
                    paddingVertical
                )

                newField.hint = "Nama Pemesan Tiket ${i + 2}"

                container.addView(newField)
            }
        } else {
            for (i in (currentExtraFields - 1) downTo finalExtraFields) {
                container.removeViewAt(i)
            }
        }
    }

    private fun submitOrder() {
        val container = binding.nameFieldsContainer
        val quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 1
        val totalPrice = quantity * eventPrice

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Anda harus login untuk memesan", Toast.LENGTH_SHORT).show()
            return
        }

        var allNamesFilled = true
        var mainBuyerName = ""

        // Validasi field pertama
        val name1 = binding.etName.text.toString().trim()
        if (name1.isEmpty()) {
            binding.etName.error = "Nama tidak boleh kosong"
            allNamesFilled = false
        } else {
            mainBuyerName = name1
        }

        // Validasi field tambahan (jika ada)
        for (i in 0 until container.childCount) {
            val nameField = container.getChildAt(i) as EditText
            val name = nameField.text.toString().trim()

            if (name.isEmpty()) {
                nameField.error = "Nama tidak boleh kosong"
                allNamesFilled = false
            }
        }

        if (!allNamesFilled) {
            Toast.makeText(this, "Harap isi semua nama pemesan", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat objek Order baru
        val order = Order(
            eventId = eventId ?: "",
            userId = currentUser.uid,
            buyerName = mainBuyerName,
            buyerEmail = currentUser.email ?: "",
            eventName = eventName ?: "",
            eventDate = eventDate ?: "",
            eventLocation = eventLocation ?: "",
            eventImageUrl = eventImageUrl ?: "",
            quantity = quantity,
            totalPrice = totalPrice
        )

        // Simpan ke database
        orderUseCase.addOrder(currentUser.uid, order, {
            Toast.makeText(this, "Pemesanan berhasil!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MyTicketsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()

        }, {
            Toast.makeText(this, "Gagal memesan: ${it.message}", Toast.LENGTH_SHORT).show()
        })
    }
}