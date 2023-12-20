package ru.aasmc.backend.service

import ru.aasmc.backend.dto.CreateUserRequest
import ru.aasmc.backend.dto.UserResponse

interface UserService {

    fun createUser(dto: CreateUserRequest): UserResponse

    fun getUserById(id: Long): UserResponse

    fun getAllUsers(from: Int, size: Int): List<UserResponse>

}


