package ru.aasmc.backend.dto

import java.math.BigDecimal

data class CreateProductRequest(
    val name: String,
    val price: BigDecimal,
    val description: String? = null
)
