package com.ledinhthi.ontaptld.core.data.mapper

/** Data (Room @Entity / DTO) -> Domain model. */
interface EntityMapper<E, D> {
    fun toDomain(entity: E): D
    fun toDomainOrNull(entity: E?): D? = entity?.let(::toDomain)
    fun toDomainList(entities: List<E>?): List<D> = entities?.map(::toDomain).orEmpty()
}

/** Domain model -> Data. */
interface ModelMapper<D, E> {
    fun toEntity(model: D): E
    fun toEntityList(models: List<D>?): List<E> = models?.map(::toEntity).orEmpty()
}
