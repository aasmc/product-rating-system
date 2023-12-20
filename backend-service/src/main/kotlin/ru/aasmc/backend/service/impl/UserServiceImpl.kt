package ru.aasmc.backend.service.impl

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.aasmc.backend.dao.UserDAO
import ru.aasmc.backend.dto.CreateUserRequest
import ru.aasmc.backend.dto.UserResponse
import ru.aasmc.backend.mapper.UserMapper
import ru.aasmc.backend.service.UserService

@Service
class UserServiceImpl(
    private val userDao: UserDAO,
    private val mapper: UserMapper
): UserService {

    override fun createUser(dto: CreateUserRequest): UserResponse {
        val transientEntity = mapper.mapToEntity(dto)
        return mapper.mapToDto(userDao.save(transientEntity))
    }

    override fun getUserById(id: Long): UserResponse {
        return userDao.findById(id)
            .map(mapper::mapToDto)
            .orElseThrow()
    }

    override fun getAllUsers(from: Int, size: Int): List<UserResponse> {
        val pageable = PageRequest.of(from, size)
         return userDao.findAll(pageable).map(mapper::mapToDto).toList()
    }

}