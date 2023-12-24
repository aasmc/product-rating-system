package ru.aasmc.aggregator.service

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.aasmc.aggregator.props.KafkaProps
import ru.aasmc.aggregator.props.TopicProps
import ru.aasmc.aggregator.util.AvroReviewPunctuator
import ru.aasmc.avro.AvroProductRating
import ru.aasmc.avro.AvroReview
import java.util.UUID

private val log = LoggerFactory.getLogger(KafkaReviewAggregatorService::class.java)

@Service
class KafkaReviewAggregatorService(
    private val topicProps: TopicProps,
    private val kafkaProps: KafkaProps
) {

    @Autowired
    fun processStreams(builder: StreamsBuilder) {
        val reviewValueSerde = SpecificAvroSerde<AvroReview>()
        configureSerde(reviewValueSerde, false)
        val reviewKeySerde = Serdes.String()

        val productRatingValueSerde = SpecificAvroSerde<AvroProductRating>()
        configureSerde(productRatingValueSerde, false)
        val productRatingKeySerde = Serdes.String()

        val productReviews: KStream<String, AvroReview> = builder.stream(
            topicProps.reviewTopic,
            Consumed.with(reviewKeySerde, reviewValueSerde)
        )

        val aggregatedStore = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(topicProps.aggregatedReviewsStore),
            Serdes.String(),
            productRatingValueSerde
        )

        builder.addStateStore(aggregatedStore)

        productReviews
            .peek { key, value ->
                log.info("Processing AvroReview: {}. Message Key: {}", value, key)
            }
            .mapValues(::toAvroProductRating)
            .process(
                ProcessorSupplier {
                    AvroReviewPunctuator(topicProps)
                },
                topicProps.aggregatedReviewsStore
            )
            .peek { key, value ->
                log.info("Processing AvroProductRating: {}. Message Key: {}", value, key)
            }
            .to(
                topicProps.aggregatedReviewsTopic,
                Produced.with(productRatingKeySerde, productRatingValueSerde)
            )
    }


    private fun configureSerde(serde: Serde<*>, isKey: Boolean) {
        if (serde is SpecificAvroSerde) {
            val config = hashMapOf<String, Any>(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaProps.schemaRegistryUrl
            )
            serde.configure(config, isKey)
        }
    }


    private fun toAvroProductRating(record: AvroReview): AvroProductRating =
        AvroProductRating(
            record.productId,
            hashMapOf(
                convertDigitToString(record.rating) to 1
            ),
            UUID.randomUUID().toString()
        )

    private fun convertDigitToString(digit: Int): String {
        return when(digit) {
            1 -> "one"
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            else -> throw RuntimeException("Unsupported Rating: $digit")
        }
    }

}