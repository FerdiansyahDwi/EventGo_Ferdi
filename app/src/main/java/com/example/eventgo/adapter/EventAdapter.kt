package com.example.eventgo.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventgo.R
import com.example.eventgo.entity.Event
import com.example.eventgo.EditEventActivity
import com.example.eventgo.EventDetailActivity
import com.example.eventgo.usecase.EventUseCase
import android.widget.Toast
import com.bumptech.glide.Glide


class EventAdapter (
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
        holder.tvPrice.text = "Harga: Rp${event.price}"

        Glide.with(context)
            .load(event.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.ivEvent)
    }

    fun updateData(newList: List<Event>) {
        eventList = newList
        notifyDataSetChanged()
    }
}