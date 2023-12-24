package ru.aasmc.backend.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "opensearch")
class OpenSearchProps @ConstructorBinding constructor(
    val uris: String,
    val username: String,
    val password: String,
    val wilsonScoreField: String
)