package com.example.go.data

data class Place(
    val id: String,
    val name: String,
    val description: String,
    val location: String,
    val visitors: Int,
    val imageUrl: String,
    val points: Int
)

// Для хранения истории посещений с баллами
data class VisitHistory(
    val placeId: String,
    val placeName: String,
    val visitTime: Long,
    val pointsEarned: Int
)