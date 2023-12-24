package ru.aasmc.backend.dto

import java.math.BigDecimal

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val description: String? = null,
    val wilsonScore: Double? = null,
    val ratings: Map<String, Int> = hashMapOf()
)
