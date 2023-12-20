package ru.aasmc.backend.service

import ru.aasmc.backend.dto.CreateReviewRequest
import ru.aasmc.backend.dto.ReviewResponse

interface ProductReviewService {

    fun createReview(dto: CreateReviewRequest): ReviewResponse

    fun getAllReviewsOfUser(userId: Long, from: Int, size: Int): List<ReviewResponse>

    fun getAllReviewsOfProduct(productId: Long, from: Int, size: Int): List<ReviewResponse>

    fun getAllReviews(from: Int, size: Int): List<ReviewResponse>

}