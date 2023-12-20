package ru.aasmc.aggregator.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.time.Duration

@ConfigurationProperties(prefix = "topicprops")
class TopicProps @ConstructorBinding constructor(
    val reviewTopic: String,
    val aggregatedReviewsTopic: String,
    val aggregatedReviewsStore: String,
    val partitions: Int,
    val replication: Int,
    val punctuationInterval: Duration
)