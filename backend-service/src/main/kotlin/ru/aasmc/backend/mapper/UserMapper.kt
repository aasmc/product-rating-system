package ru.aasmc.backend.mapper

import org.springframework.stereotype.Component
import ru.aasmc.backend.domain.AppUser
import ru.aasmc.backend.dto.CreateUserRequest
import ru.aasmc.backend.dto.UserResponse

@Component
class UserMapper: Mapper<CreateUserRequest, AppUser, UserResponse> {

    override fun mapToEntity(dto: CreateUserRequest): AppUser =
        AppUser(
            username = dto.username,
            firstName = dto.firstName,
            lastName = dto.lastName
        )

    override fun mapToDto(entity: AppUser): UserResponse =
        UserResponse(
            id = entity.id ?: throw RuntimeException("User entity without ID!"),
            username = entity.username,
            firstName = entity.firstName,
            lastName = entity.lastName
        )
}