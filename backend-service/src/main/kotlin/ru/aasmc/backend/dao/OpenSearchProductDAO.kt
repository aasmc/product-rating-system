package ru.aasmc.backend.dao

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import ru.aasmc.backend.domain.OpenSearchProduct

interface OpenSearchProductDAO: ElasticsearchRepository<OpenSearchProduct, String> {
}