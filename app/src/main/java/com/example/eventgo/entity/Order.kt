package com.example.eventgo.entity

data class Order(
    var id: String? = null,
    var eventId: String = "",
    var userId: String = "",
    var buyerName: String = "",
    var buyerEmail: String = "",

    var eventName: String = "",
    var eventDate: String = "",
    var eventLocation: String = "",
    var eventImageUrl: String = "",

    var quantity: Int = 1,
    var totalPrice: Double = 0.0
)