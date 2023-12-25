package ru.aasmc.backend.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Document(indexName = "products")
class OpenSearchProduct(
    @Id
    var id: String? = null,
    @Field(type = FieldType.Text, analyzer = "russian")
    var name: String? = null,
    @Field(type = FieldType.Text, analyzer = "russian")
    var description: String? = null,
    @Field(type = FieldType.Double)
    var price: BigDecimal? = null,
    @Field(name = "wilson-score", type = FieldType.Double)
    var wilsonScore: Double = 0.0,
    @Field(type = FieldType.Object)
    var ratings: Map<String, Int> = hashMapOf(
        "1" to 0,
        "2" to 0,
        "3" to 0,
        "4" to 0,
        "5" to 0,
    ),
    @Field(type = FieldType.Long, name = "rating_update_idempotency_key")
    var ratingIdempotencyKey: Long? = null
) {
    override fun toString(): String {
        return "OpenSearchProduct [id=$id, name=$name, description=$description, price=$price, wilsonScore=$wilsonScore, ratings=$ratings]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenSearchProduct

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (price != other.price) return false
        if (wilsonScore != other.wilsonScore) return false
        if (ratings != other.ratings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + wilsonScore.hashCode()
        result = 31 * result + ratings.hashCode()
        return result
    }


}