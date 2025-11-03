package com.example.eventgo.entity

data class Event(
    var id: String? = null,
    var title: String? = null,
    var date: String? = null,
    var location: String? = null,
    var price: Double? = null,
    var description: String? = null,
    var category: String? = null
)
