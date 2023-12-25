package ru.aasmc.aggregator.opensearch.service

import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.ScriptType
import org.springframework.data.elasticsearch.core.query.UpdateQuery
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import ru.aasmc.aggregator.opensearch.props.OpenSearchProps
import ru.aasmc.avro.AvroProductRating

private val log = LoggerFactory.getLogger(OpenSearchKafkaConsumer::class.java)

private const val SCRIPT_SOURCE = """
    if (ctx._source['rating_update_idempotency_key'] == null ||
     ctx._source['rating_update_idempotency_key'] < params.idempotency_key) {
        if (params.containsKey('one')) {
            ctx._source.ratings['1'] = (ctx._source.ratings['1'] ?: 0) + params.one;
        }
        if (params.containsKey('two')) {
            ctx._source.ratings['2'] = (ctx._source.ratings['2'] ?: 0) + params.two;
        }
        if (params.containsKey('three')) {
            ctx._source.ratings['3'] = (ctx._source.ratings['3'] ?: 0) + params.three;
        }
        if (params.containsKey('four')) {
            ctx._source.ratings['4'] = (ctx._source.ratings['4'] ?: 0) + params.four;
        }
        if (params.containsKey('five')) {
            ctx._source.ratings['5'] = (ctx._source.ratings['5'] ?: 0) + params.five;
        }
        long s1 = ctx._source.ratings.containsKey('1') ? ctx._source.ratings['1'] : 0;
        long s2 = ctx._source.ratings.containsKey('2') ? ctx._source.ratings['2'] : 0;
        long s3 = ctx._source.ratings.containsKey('3') ? ctx._source.ratings['3'] : 0;
        long s4 = ctx._source.ratings.containsKey('4') ? ctx._source.ratings['4'] : 0;
        long s5 = ctx._source.ratings.containsKey('5') ? ctx._source.ratings['5'] : 0;
        double p = (s1 * 0.0) + (s2 * 0.25) + (s3 * 0.5) + (s4 * 0.75) + (s5 * 1.0);
        double n = (s1 * 1.0) + (s2 * 0.75) + (s3 * 0.5) + (s4 * 0.25) + (s5 * 0.0);
        double wilsonScore = p + n > 0 ? ((p + 1.9208) / (p + n) - 1.96 * Math.sqrt((p * n) / (p + n) + 0.9604) / (p + n)) / (1 + 3.8416 / (p + n)) : 0;
        ctx._source['wilson-score'] = wilsonScore;
        ctx._source['updated_at'] = params.created;
    }
"""

private const val IDEMPOTENCY_KEY = "idempotency_key"

@Service
class OpenSearchKafkaConsumer(
    private val openSearchRestTemplate: OpenSearchRestTemplate,
    private val props: OpenSearchProps
) {

    @KafkaListener(topics = ["\${topicprops.aggregatedReviewsTopic}"], concurrency = "3")
    fun consumeAndSend(record: AvroProductRating) {
        updateProductInOpenSearch(record)
        log.info("Successfully updated rating of product with id = {}", record.productId)
    }

    private fun updateProductInOpenSearch(record: AvroProductRating) {
        val params = mutableMapOf<String, Any>()
        record.ratings.forEach { (key, value) ->
            params[key] = value
        }
        params[IDEMPOTENCY_KEY] = record.idempotencyKey
        val updateQuery = UpdateQuery.builder(record.productId.toString())
            .withScriptType(ScriptType.INLINE)
            .withLang("painless")
            .withScript(SCRIPT_SOURCE)
            .withParams(params)
            .build()
        openSearchRestTemplate.update(updateQuery, IndexCoordinates.of(props.productIndex))
    }
}