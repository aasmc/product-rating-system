package ru.aasmc.backend.service.impl

import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import ru.aasmc.backend.dao.OpenSearchProductDAO
import ru.aasmc.backend.domain.OpenSearchProduct
import ru.aasmc.backend.domain.Product
import ru.aasmc.backend.dto.ProductResponse
import ru.aasmc.backend.props.OpenSearchProps
import ru.aasmc.backend.service.OpenSearchService
import java.io.IOException

private val log = LoggerFactory.getLogger(OpenSearchServiceImpl::class.java)

@Service
class OpenSearchServiceImpl(
    private val openSearchProps: OpenSearchProps,
    private val openSearchRestTemplate: OpenSearchRestTemplate,
    private val openSearchProductDAO: OpenSearchProductDAO
) : OpenSearchService {

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

    @Retryable(
        value = [IOException::class],
        maxAttempts = 5,
        backoff = Backoff(delay = 1000, multiplier = 1.2, random = true)
    )
    override fun saveToOpenSearch(persistentProduct: Product) {
        val openSearchProduct = OpenSearchProduct()
        openSearchProduct.name = persistentProduct.name
        openSearchProduct.price = persistentProduct.price
        openSearchProduct.description = persistentProduct.description
        openSearchProduct.id = persistentProduct.id.toString()
        openSearchProductDAO.save(openSearchProduct).also {
            log.info("Successfully saved product to OpenSearch. {}", it)
        }
    }
}