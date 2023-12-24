package ru.aasmc.aggregator.opensearch.client

import org.opensearch.client.RestHighLevelClient
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration
import org.opensearch.data.client.orhlc.ClientConfiguration
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.data.client.orhlc.RestClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.aasmc.aggregator.opensearch.props.OpenSearchProps

@Configuration
class RestClientConfig(
    private val openSearchProps: OpenSearchProps
): AbstractOpenSearchConfiguration() {

    override fun opensearchClient(): RestHighLevelClient {
        val clientConfiguration = ClientConfiguration.builder()
            .connectedTo(openSearchProps.uris)
            .withBasicAuth(openSearchProps.username, openSearchProps.password)
            .build()
        return RestClients.create(clientConfiguration).rest()
    }

    @Bean
    fun openSearchRestTemplate(client: RestHighLevelClient): OpenSearchRestTemplate {
        return OpenSearchRestTemplate(client)
    }
}