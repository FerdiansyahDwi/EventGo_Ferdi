package com.example.eventgo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventgo.R
import com.example.eventgo.entity.Order
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.Locale

class OrderAdapter (
    private val context: Context,
    private var orderList: List<Order>
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventName: TextView = view.findViewById(R.id.tvEventName)
        val tvEventDate: TextView = view.findViewById(R.id.tvEventDate)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvTotalPrice)
        val ivQrCode: ImageView = view.findViewById(R.id.ivQrCode)
        val cardView: View = view.findViewById(R.id.cardOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = orderList.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]

        // Bind data biasa (untuk card kecil)
        holder.tvEventName.text = order.eventName
        holder.tvEventDate.text = order.eventDate
        holder.tvQuantity.text = "Jumlah: ${order.quantity} Tiket"
        val formattedTotal = String.format(Locale("in", "ID"), "%,d", order.totalPrice.toLong())
        holder.tvTotalPrice.text = "Total: Rp$formattedTotal"

        // Buat QR Code kecil
        generateQrCode(holder.ivQrCode, order.id ?: "INVALID_ID", 150) // 150px

        holder.cardView.setOnClickListener {
            showTicketDialog(order)
        }
    }

    private fun showTicketDialog(order: Order) {
        val builder = AlertDialog.Builder(context, R.style.Theme_EventGo_Dialog)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_ticket_detail, null)
        builder.setView(dialogView)

        // Inisialisasi semua view di dalam dialog
        val ivEventImage = dialogView.findViewById<ImageView>(R.id.ivEventImage)
        val tvEventName = dialogView.findViewById<TextView>(R.id.tvEventName)
        val tvEventDate = dialogView.findViewById<TextView>(R.id.tvEventDate)
        val tvBuyerName = dialogView.findViewById<TextView>(R.id.tvBuyerName)
        val tvQuantity = dialogView.findViewById<TextView>(R.id.tvQuantity)
        val tvTotalPrice = dialogView.findViewById<TextView>(R.id.tvTotalPrice)
        val ivQrCodeLarge = dialogView.findViewById<ImageView>(R.id.ivQrCodeLarge)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        // Isi data ke dalam view
        Glide.with(context)
            .load(order.eventImageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(ivEventImage)

        tvEventName.text = order.eventName
        tvEventDate.text = order.eventDate
        tvBuyerName.text = "Pemesan: ${order.buyerName}"
        tvQuantity.text = "Jumlah: ${order.quantity} Tiket"
        val formattedTotal = String.format(Locale("in", "ID"), "%,d", order.totalPrice.toLong())
        tvTotalPrice.text = "Total: Rp$formattedTotal"

        // Buat QR Code besar
        generateQrCode(ivQrCodeLarge, order.id ?: "INVALID_ID", 600) // 600px

        // Tampilkan dialog
        val dialog = builder.create()
        dialog.show()

        // Atur tombol tutup
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun generateQrCode(imageView: ImageView, text: String, size: Int) {
        try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateData(newList: List<Order>) {
        orderList = newList
        notifyDataSetChanged()
    }
}