package ru.aasmc.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication(exclude = [ElasticsearchDataAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableRetry
class BackendServiceApplication

fun main(args: Array<String>) {
    runApplication<BackendServiceApplication>(*args)
}
