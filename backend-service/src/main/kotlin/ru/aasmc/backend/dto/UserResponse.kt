package ru.aasmc.backend.dto

data class UserResponse(
    val id: Long,
    val username: String,
    val firstName: String,
    val lastName: String
)
