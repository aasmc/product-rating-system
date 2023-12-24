package ru.aasmc.backend.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse
import ru.aasmc.backend.service.OpenSearchService
import ru.aasmc.backend.service.ProductService

private val log = LoggerFactory.getLogger(ProductsController::class.java)

@RestController
@RequestMapping("/v1/products")
class ProductsController(
    private val productService: ProductService,
    private val openSearchService: OpenSearchService
) {

    @PostMapping
    fun createProduct(@RequestBody dto: CreateProductRequest): ProductResponse {
        log.info("Received POST request to create product: {}", dto)
        return productService.createProduct(dto)
    }

    @GetMapping("/sorted")
    fun getSortedProducts(): List<ProductResponse> {
        log.info("Received request to GET all product sorted by wilson score")
        return openSearchService.getAllProductsSortedByWilsonScore()
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