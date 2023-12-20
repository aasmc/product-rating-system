package ru.aasmc.aggregator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@ComponentScan("ru.aasmc")
@SpringBootApplication
@ConfigurationPropertiesScan
class ReviewAggregatorServiceApplication

fun main(args: Array<String>) {
    runApplication<ReviewAggregatorServiceApplication>(*args)
}
