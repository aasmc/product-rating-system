package ru.aasmc.backend.mapper

import org.springframework.stereotype.Component
import ru.aasmc.backend.dao.ProductDAO
import ru.aasmc.backend.dao.UserDAO
import ru.aasmc.backend.domain.ProductReview
import ru.aasmc.backend.dto.CreateReviewRequest
import ru.aasmc.backend.dto.ReviewResponse

@Component
class ReviewMapper(
    private val userDao: UserDAO,
    private val productDao: ProductDAO
): Mapper<CreateReviewRequest, ProductReview, ReviewResponse> {

    override fun mapToEntity(dto: CreateReviewRequest): ProductReview {
        val user = userDao.findById(dto.userId)
            .orElseThrow {
                RuntimeException("User with ID=${dto.userId} not found")
            }
        val product = productDao.findById(dto.productId)
            .orElseThrow {
                RuntimeException("Product with ID=${dto.productId} not found!")
            }
        return ProductReview(
            user = user,
            product = product,
            rating = dto.rating,
            comment = dto.comment
        )
    }

    override fun mapToDto(entity: ProductReview): ReviewResponse =
        ReviewResponse(
            id = entity.id ?: throw RuntimeException("ProductReview entity without ID!"),
            userId = entity.user.id!!,
            productId = entity.product.id!!,
            rating = entity.rating,
            createdAt = entity.createdAt,
            comment = entity.comment
        )
}