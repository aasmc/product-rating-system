package ru.aasmc.backend.dto

data class CreateUserRequest(
    val username: String,
    val firstName: String,
    val lastName: String
)
