package ru.aasmc.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Table(name = "products")
@Entity
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,
    var price: BigDecimal,
    var description: String? = null
)