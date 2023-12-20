package ru.aasmc.backend.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.aasmc.backend.dto.CreateUserRequest
import ru.aasmc.backend.dto.UserResponse
import ru.aasmc.backend.service.UserService

private val log = LoggerFactory.getLogger(UserController::class.java)

@RestController
@RequestMapping("/v1/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    fun createUser(@RequestBody dto: CreateUserRequest): UserResponse {
        log.info("Received POST request to create user: {}", dto)
        return userService.createUser(dto)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable("id") id: Long): UserResponse {
        log.info("Received request to GET user by id: {}", id)
        return userService.getUserById(id)
    }

    @GetMapping
    fun getAllUsers(
        @RequestParam("from", defaultValue = "0") from: Int,
        @RequestParam("size", defaultValue = "10") size: Int
    ): List<UserResponse> {
        log.info("Received request to get all users. From={}, size={}", from, size)
        return userService.getAllUsers(from, size)
    }

}