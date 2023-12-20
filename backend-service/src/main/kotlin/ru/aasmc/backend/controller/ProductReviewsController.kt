package ru.aasmc.backend.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.aasmc.backend.dto.CreateReviewRequest
import ru.aasmc.backend.dto.ReviewResponse
import ru.aasmc.backend.service.ProductReviewService

private val log = LoggerFactory.getLogger(ProductReviewsController::class.java)

@RestController
@RequestMapping("/v1/reviews")
class ProductReviewsController(
    private val reviewService: ProductReviewService
) {

    @PostMapping
    fun createReview(@RequestBody dto: CreateReviewRequest): ReviewResponse {
        log.info("Received POST request to create review: {}", dto)
        return reviewService.createReview(dto)
    }

    @GetMapping("/user/{id}")
    fun getReviewsOfUser(
        @PathVariable("id") id: Long,
        @RequestParam("from", defaultValue = "0") from: Int,
        @RequestParam("size", defaultValue = "10") size: Int
    ): List<ReviewResponse> {
        log.info("Received request to GET all reviews of user with id={}", id)
        return reviewService.getAllReviewsOfUser(id, from, size)
    }

    @GetMapping("/product/{id}")
    fun getReviewsForProduct(
        @PathVariable("id") id: Long,
        @RequestParam("from", defaultValue = "0") from: Int,
        @RequestParam("size", defaultValue = "10") size: Int
    ): List<ReviewResponse> {
        log.info("Received request to GET all reviews for product with id={}", id)
        return reviewService.getAllReviewsOfProduct(id, from, size)
    }

    @GetMapping
    fun getAllReviews(
        @RequestParam("from", defaultValue = "0") from: Int,
        @RequestParam("size", defaultValue = "10") size: Int
    ): List<ReviewResponse> {
        log.info("Received request to GET all reviews")
        return reviewService.getAllReviews(from, size)
    }

}