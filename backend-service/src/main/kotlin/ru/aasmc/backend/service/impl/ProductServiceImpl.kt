package ru.aasmc.backend.service.impl

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.aasmc.backend.dao.ProductDAO
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse
import ru.aasmc.backend.mapper.ProductMapper
import ru.aasmc.backend.service.ProductService

@Service
class ProductServiceImpl(
    private val productDao: ProductDAO,
    private val mapper: ProductMapper
) : ProductService {
    override fun createProduct(dto: CreateProductRequest): ProductResponse {
        val transientProduct = mapper.mapToEntity(dto)
        return mapper.mapToDto(productDao.save(transientProduct))
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