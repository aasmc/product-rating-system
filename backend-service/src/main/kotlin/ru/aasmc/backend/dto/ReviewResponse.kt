package ru.aasmc.backend.dto

import java.time.LocalDateTime

data class ReviewResponse(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val rating: Int,
    val createdAt: LocalDateTime,
    val comment: String? = null
)
