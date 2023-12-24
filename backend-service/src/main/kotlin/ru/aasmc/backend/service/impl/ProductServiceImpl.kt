package ru.aasmc.backend.service.impl

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.aasmc.backend.dao.ProductDAO
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse
import ru.aasmc.backend.mapper.ProductMapper
import ru.aasmc.backend.service.OpenSearchService
import ru.aasmc.backend.service.ProductService

private val log = LoggerFactory.getLogger(ProductServiceImpl::class.java)

@Service
class ProductServiceImpl(
    private val productDao: ProductDAO,
    private val mapper: ProductMapper,
    private val openSearchService: OpenSearchService
) : ProductService {
    override fun createProduct(dto: CreateProductRequest): ProductResponse {
        val transientProduct = mapper.mapToEntity(dto)
        val saved = productDao.save(transientProduct)
        // potential point of failure. Retryable.
        openSearchService.saveToOpenSearch(saved)
        return mapper.mapToDto(saved)
    }

    override fun getProductById(id: Long): ProductResponse {
        return productDao.findById(id)
            .map(mapper::mapToDto)
            .orElseThrow()
    }

    override fun getAllProducts(from: Int, size: Int): List<ProductResponse> {
        val pageable = PageRequest.of(from, size)
        return productDao.findAll(pageable)
            .map(mapper::mapToDto)
            .toList()
    }

}