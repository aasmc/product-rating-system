package ru.aasmc.backend.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "product_reviews")
class ProductReview(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: AppUser,
    @ManyToOne( optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,
    var rating: Int,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var comment: String? = null
)