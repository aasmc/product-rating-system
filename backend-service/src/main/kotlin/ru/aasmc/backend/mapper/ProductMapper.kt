package ru.aasmc.backend.mapper

import org.springframework.stereotype.Component
import ru.aasmc.backend.domain.Product
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse

@Component
class ProductMapper: Mapper<CreateProductRequest, Product, ProductResponse> {
    override fun mapToEntity(dto: CreateProductRequest): Product =
        Product(
            name = dto.name,
            price = dto.price,
            description = dto.description
        )

    override fun mapToDto(entity: Product): ProductResponse =
        ProductResponse(
            id = entity.id ?: throw RuntimeException("Product entity without ID!"),
            name = entity.name,
            price = entity.price,
            description = entity.description
        )
}