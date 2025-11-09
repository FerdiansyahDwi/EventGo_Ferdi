package com.example.eventgo.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventgo.EditEventActivity
import com.example.eventgo.EventDetailActivity
import com.example.eventgo.R
import com.example.eventgo.entity.Event
import com.example.eventgo.usecase.EventUseCase
import com.example.eventgo.SessionManager
import java.util.Locale

class EventAdapter(
    private val context: Context,
    private var eventList: List<Event>
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val eventUseCase = EventUseCase()

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivEvent: ImageView = itemView.findViewById(R.id.ivEvent)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnDetail: Button = itemView.findViewById(R.id.btnDetail)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun getItemCount(): Int = eventList.size

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]

        holder.tvTitle.text = event.title
        holder.tvLocation.text = "Lokasi: ${event.location}"
        holder.tvDate.text = "Tanggal: ${event.date}"
        val priceAsLong = event.price.toLong()
        val formattedPrice = String.format(Locale("in", "ID"), "%,d", priceAsLong)
        holder.tvPrice.text = "Harga: Rp${formattedPrice}"

        val userRole = SessionManager.getUserRole()

        if (userRole == "admin") {
            // Jika Admin, tampilkan semua tombol
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDetail.visibility = View.VISIBLE
        } else {
            // Jika User, sembunyikan Edit dan Delete
            holder.btnEdit.visibility = View.GONE
            holder.btnDelete.visibility = View.GONE
            holder.btnDetail.visibility = View.VISIBLE
        }

        Glide.with(context)
            .load(event.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.ivEvent)

        // Tombol DETAIL
        holder.btnDetail.setOnClickListener {
            val intent = Intent(context, EventDetailActivity::class.java)
            intent.putExtra("event_id", event.id)
            context.startActivity(intent)
        }

        // Tombol EDIT
        holder.btnEdit.setOnClickListener {
            val intent = Intent(context, EditEventActivity::class.java)
            intent.putExtra("event_id", event.id)
            intent.putExtra("title", event.title)
            intent.putExtra("description", event.description)
            intent.putExtra("date", event.date)
            intent.putExtra("location", event.location)
            intent.putExtra("price", event.price)
            intent.putExtra("imageUrl", event.imageUrl)
            context.startActivity(intent)
        }

        // Tombol DELETE
        holder.btnDelete.setOnClickListener {
            event.id?.let { eventId ->
                eventUseCase.deleteEvent(eventId, {
                    Toast.makeText(context, "Event berhasil dihapus", Toast.LENGTH_SHORT).show()
                    val newList = eventList.toMutableList()
                    newList.removeAt(position)
                    updateData(newList)
                }, {
                    Toast.makeText(context, "Gagal menghapus event: ${it.message}", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    fun updateData(newList: List<Event>) {
        eventList = newList
        notifyDataSetChanged()
    }
}
