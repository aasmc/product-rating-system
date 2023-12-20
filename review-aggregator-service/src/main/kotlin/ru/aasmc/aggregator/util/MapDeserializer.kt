package ru.aasmc.aggregator.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer

class MapDeserializer(
    private val objectMapper: ObjectMapper = ObjectMapper()
): Deserializer<HashMap<String, Int>> {
    override fun deserialize(topic: String?, data: ByteArray?): HashMap<String, Int>? {
        if (data == null) return null
        try {
            val typeReference = object : TypeReference<HashMap<String, Int>>(){}
            return objectMapper.readValue(data, typeReference)
        } catch (e: Exception) {
            throw RuntimeException("Error deserializing value", e)
        }
    }

}