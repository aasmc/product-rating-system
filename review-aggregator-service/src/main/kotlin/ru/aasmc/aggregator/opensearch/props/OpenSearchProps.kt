package ru.aasmc.aggregator.opensearch.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "opensearch")
class OpenSearchProps @ConstructorBinding constructor(
    val uris: String,
    val username: String,
    val password: String,
    val scriptName: String,
    val productIndex: String,
    val clockDeltaMs: Long
)