package ru.aasmc.backend.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse
import ru.aasmc.backend.service.ProductService

private val log = LoggerFactory.getLogger(ProductsController::class.java)

@RestController
@RequestMapping("/v1/products")
class ProductsController(
    private val productService: ProductService
) {

    @PostMapping
    fun createProduct(@RequestBody dto: CreateProductRequest): ProductResponse {
        log.info("Received POST request to create product: {}", dto)
        return productService.createProduct(dto)
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable("id") id: Long): ProductResponse {
        log.info("Received request to GET product by id={}", id)
        return productService.getProductById(id)
    }

    @GetMapping
    fun getAllProducts(
        @RequestParam("from", defaultValue = "0") from: Int,
        @RequestParam("size", defaultValue = "10") size: Int
    ): List<ProductResponse> {
        log.info("Received request to GET all products. From={}, size={}", from, size)
        return productService.getAllProducts(from, size)
    }
}