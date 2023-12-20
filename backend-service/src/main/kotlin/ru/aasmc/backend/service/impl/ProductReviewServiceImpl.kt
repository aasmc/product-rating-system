package ru.aasmc.backend.service.impl

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.aasmc.backend.dao.ProductReviewDAO
import ru.aasmc.backend.dto.CreateReviewRequest
import ru.aasmc.backend.dto.ReviewResponse
import ru.aasmc.backend.mapper.ReviewMapper
import ru.aasmc.backend.service.ProductReviewService

@Service
class ProductReviewServiceImpl(
    private val reviewDao: ProductReviewDAO,
    private val mapper: ReviewMapper
) : ProductReviewService {

    override fun createReview(dto: CreateReviewRequest): ReviewResponse {
        val transientReview = mapper.mapToEntity(dto)
        return mapper.mapToDto(reviewDao.save(transientReview))
    }

    override fun getAllReviewsOfUser(
        userId: Long,
        from: Int,
        size: Int
    ): List<ReviewResponse> {
        val pageable = PageRequest.of(from, size)
        return reviewDao.findAllByUserId(userId, pageable)
            .map(mapper::mapToDto)
    }

    override fun getAllReviewsOfProduct(
        productId: Long,
        from: Int,
        size: Int
    ): List<ReviewResponse> {
        val pageable = PageRequest.of(from, size)
        return reviewDao.findAllByProductId(productId, pageable)
            .map(mapper::mapToDto)
    }

    override fun getAllReviews(from: Int, size: Int): List<ReviewResponse> {
        val pageable = PageRequest.of(from, size)
        return reviewDao.findAll(pageable)
            .map(mapper::mapToDto)
            .toList()
    }
}