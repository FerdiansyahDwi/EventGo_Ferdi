package com.example.eventgo.usecase

import com.example.eventgo.entity.Order
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OrderUseCase {
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    //Menyimpan order ke node spesifik pengguna: /users/{userId}/my_tickets/{orderId}
    fun addOrder(userId: String, order: Order, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        // Buat key di bawah /users/{userId}/my_tickets/
        val key = dbRef.child(userId).child("my_tickets").push().key ?: return
        order.id = key
        order.userId = userId

        dbRef.child(userId).child("my_tickets").child(key).setValue(order)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    //Mengambil SEMUA order HANYA untuk pengguna yang sedang login
    fun getOrdersForUser(userId: String, onResult: (List<Order>) -> Unit) {
        val query = dbRef.child(userId).child("my_tickets")

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Order>()
                for (data in snapshot.children) {
                    data.getValue(Order::class.java)?.let { list.add(it) }
                }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}