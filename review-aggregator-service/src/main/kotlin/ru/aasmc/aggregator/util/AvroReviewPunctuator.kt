package ru.aasmc.aggregator.util

import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import org.slf4j.LoggerFactory
import ru.aasmc.aggregator.props.TopicProps
import ru.aasmc.avro.AvroProductRating

private val log = LoggerFactory.getLogger(AvroReviewPunctuator::class.java)

class AvroReviewPunctuator(
    private val topicProps: TopicProps
): Processor<String, AvroProductRating, String, AvroProductRating> {

    private lateinit var context: ProcessorContext<String, AvroProductRating>
    private lateinit var store: KeyValueStore<String, AvroProductRating>

    override fun init(context: ProcessorContext<String, AvroProductRating>) {
        this.context = context
        this.store = context.getStateStore(topicProps.aggregatedReviewsStore) as KeyValueStore<String, AvroProductRating>
        context.schedule(topicProps.punctuationInterval, PunctuationType.WALL_CLOCK_TIME, this::punctuate)
    }


    private fun punctuate(to: Long) {
        log.debug("Enter punctuate method.")
        store.all().forEachRemaining { entry ->
            log.info("Sending new aggregated value from punctuate: {}", entry.value)
            context.forward(Record(entry.key, entry.value, to))
            store.delete(entry.key)
        }
    }

    override fun process(record: Record<String, AvroProductRating>) {
        val key = record.key()
        val newRating = record.value()
        log.debug("Processing record with value: {}", newRating)
        val currentAggregatedRating = store.get(key)
        if (currentAggregatedRating != null) {
            val currentRatings = currentAggregatedRating.ratings
            newRating.ratings.forEach { (ratingKey, ratingValue) ->
                currentRatings.merge(ratingKey, ratingValue, Int::plus)
            }
            store.put(key, AvroProductRating(newRating.productId, currentRatings))
        } else {
            store.put(key, newRating)
        }
    }
}