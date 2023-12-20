package ru.aasmc.backend.service

import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse

interface ProductService {

    fun createProduct(dto: CreateProductRequest): ProductResponse

    fun getProductById(id: Long): ProductResponse

    fun getAllProducts(from: Int, size: Int): List<ProductResponse>

}