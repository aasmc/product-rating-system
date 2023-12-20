package ru.aasmc.aggregator.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import ru.aasmc.avro.AvroProductRating

private val log = LoggerFactory.getLogger(AggregatedProductsConsumer::class.java)

@Service
class AggregatedProductsConsumer {

    @KafkaListener(topics = ["\${topicprops.aggregatedReviewsTopic}"], concurrency = "3")
    fun consumeRecord(
        record: AvroProductRating,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int
    ) {
        log.error(
            "Consuming record from kafka. Key: {}, Record: {}, Partition: {}, Thread: {}",
            key,
            record,
            partition,
            Thread.currentThread().name
        )
    }

}