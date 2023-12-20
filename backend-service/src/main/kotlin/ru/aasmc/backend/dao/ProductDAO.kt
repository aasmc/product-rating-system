package ru.aasmc.backend.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.aasmc.backend.domain.Product

interface ProductDAO: JpaRepository<Product, Long> {
}