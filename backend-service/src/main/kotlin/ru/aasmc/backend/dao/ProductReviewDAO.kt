package ru.aasmc.backend.dao

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.aasmc.backend.domain.ProductReview

interface ProductReviewDAO: JpaRepository<ProductReview, Long> {

    fun findAllByUserId(userId: Long, pageable: Pageable): List<ProductReview>

    fun findAllByProductId(productId: Long, pageable: Pageable): List<ProductReview>

}