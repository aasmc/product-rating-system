package ru.aasmc.backend.dto

data class CreateReviewRequest(
    val userId: Long,
    val productId: Long,
    val rating: Int,
    val comment: String? = null
)
