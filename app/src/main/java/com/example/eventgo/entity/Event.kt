package com.example.eventgo.entity

data class Event(
    var id: String? = null,
    var title: String = "",
    var date: String = "",
    var location: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var category: String = "",
    var imageUrl: String = ""
)
