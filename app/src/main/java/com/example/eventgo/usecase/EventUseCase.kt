package com.example.eventgo.usecase

import com.example.eventgo.entity.Event
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EventUseCase {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("events")

    // CREATE
    fun addEvent(event: Event, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val key = dbRef.push().key ?: return
        event.id = key
        dbRef.child(key).setValue(event)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // READ
    fun getAllEvents(onResult: (List<Event>) -> Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Event>()
                for (data in snapshot.children) {
                    data.getValue(Event::class.java)?.let { list.add(it) }
                }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun getEventById(eventId: String, onResult: (Event?) -> Unit, onError: (Exception) -> Unit) {
        dbRef.child(eventId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val event = snapshot.getValue(Event::class.java)
                onResult(event)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }
// ...

    // UPDATE
    fun updateEvent(event: Event, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        event.id?.let {
            dbRef.child(it).setValue(event)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it) }
        }
    }

    // DELETE
    fun deleteEvent(eventId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        dbRef.child(eventId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
