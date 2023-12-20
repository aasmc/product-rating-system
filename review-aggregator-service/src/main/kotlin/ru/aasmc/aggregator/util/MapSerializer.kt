package ru.aasmc.aggregator.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Serializer

class MapSerializer(
    private val objectMapper: ObjectMapper = ObjectMapper()
) : Serializer<HashMap<String, Int>> {
    override fun serialize(topic: String?, data: HashMap<String, Int>?): ByteArray? {
        if (data == null) return null
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw RuntimeException("Error serializing value", e)
        }
    }

    override fun close() {
        // nothing to close
    }
}