package ru.aasmc.backend.initializer

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.CreateReviewRequest
import ru.aasmc.backend.dto.CreateUserRequest
import ru.aasmc.backend.service.ProductReviewService
import ru.aasmc.backend.service.ProductService
import ru.aasmc.backend.service.UserService
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

//@Component
class AppInitializer(
    private val productService: ProductService,
    private val userService: UserService,
    private val reviewService: ProductReviewService
): ApplicationRunner {

    private val productIdx = AtomicInteger(1)
    private val userIdx = AtomicInteger(1)
    private val random = Random(System.currentTimeMillis())
    override fun run(args: ApplicationArguments?) {
        var userId = 0L
        var prodId = 0L
        repeat(10) {
            val prod = productService.createProduct(provideRandomCreateProductRequest())
            val user = userService.createUser(provideRandomCreateUserRequest())
            userId = user.id
            prodId = prod.id
        }
        reviewService.createReview(CreateReviewRequest(
            userId = userId,
            productId = prodId,
            rating = 1,
            comment = "Comment"
        ))
    }

    private fun provideRandomCreateUserRequest(): CreateUserRequest {
        val idx = userIdx.getAndIncrement()
        val username = "username $idx"
        val firstName = "FirstName$idx"
        val lastName = "LastName$idx"
        return CreateUserRequest(username, firstName, lastName)
    }

    private fun provideRandomCreateProductRequest(): CreateProductRequest {
        val idx = productIdx.getAndIncrement()
        val name = "Product Name $idx"
        val description = "Product Description: $idx"
        val price = BigDecimal.valueOf(random.nextDouble(10.0, 10000.0))
        return CreateProductRequest(name, price, description)
    }
}