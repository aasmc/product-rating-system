package ru.aasmc.backend.service.impl

import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.aasmc.backend.dao.OpenSearchProductDAO
import ru.aasmc.backend.dao.ProductDAO
import ru.aasmc.backend.domain.OpenSearchProduct
import ru.aasmc.backend.domain.Product
import ru.aasmc.backend.dto.CreateProductRequest
import ru.aasmc.backend.dto.ProductResponse
import ru.aasmc.backend.mapper.ProductMapper
import ru.aasmc.backend.props.OpenSearchProps
import ru.aasmc.backend.service.ProductService

private val log = LoggerFactory.getLogger(ProductServiceImpl::class.java)

@Service
class ProductServiceImpl(
    private val productDao: ProductDAO,
    private val mapper: ProductMapper,
    private val openSearchProductDAO: OpenSearchProductDAO,
    private val openSearchRestTemplate: OpenSearchRestTemplate,
    private val openSearchProps: OpenSearchProps
) : ProductService {
    override fun createProduct(dto: CreateProductRequest): ProductResponse {
        val transientProduct = mapper.mapToEntity(dto)
        val saved = productDao.save(transientProduct)
        saveToOpenSearch(saved)
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

    override fun getAllProductsSortedByWilsonScore(): List<ProductResponse> {
        val searchQuery = NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .withSorts(SortBuilders.fieldSort(openSearchProps.wilsonScoreField).order(SortOrder.DESC))
            .build()
        return openSearchRestTemplate.search(searchQuery, OpenSearchProduct::class.java)
            .map { hit ->
                log.info("Converting product from OpenSearch to API response. {}", hit.content)
                mapOpenSearchHitToResponse(hit.content)
            }.toList()
    }

    private fun mapOpenSearchHitToResponse(hit: OpenSearchProduct): ProductResponse =
        ProductResponse(
            id = hit.id?.toLong()!!,
            name = hit.name!!,
            price = hit.price!!,
            description = hit.description,
            wilsonScore = hit.wilsonScore,
            ratings = hit.ratings
        )

    private fun saveToOpenSearch(persistentProduct: Product) {
        val openSearchProduct = OpenSearchProduct()
        openSearchProduct.name = persistentProduct.name
        openSearchProduct.price = persistentProduct.price
        openSearchProduct.description = persistentProduct.description
        openSearchProduct.id = persistentProduct.id.toString()
        val saved = openSearchProductDAO.save(openSearchProduct)
        log.info("Successfully saved product to OpenSearch. {}", saved)
    }
}