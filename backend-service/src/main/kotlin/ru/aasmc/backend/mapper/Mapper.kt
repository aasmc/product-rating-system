package ru.aasmc.backend.mapper

interface Mapper<IN, ENTITY, OUT> {

    fun mapToEntity(dto: IN): ENTITY

    fun mapToDto(entity: ENTITY): OUT

}