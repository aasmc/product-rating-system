package ru.aasmc.backend.service

import ru.aasmc.backend.domain.Product
import ru.aasmc.backend.dto.ProductResponse

interface OpenSearchService {

    fun getAllProductsSortedByWilsonScore(): List<ProductResponse>

    fun saveToOpenSearch(persistentProduct: Product)

}