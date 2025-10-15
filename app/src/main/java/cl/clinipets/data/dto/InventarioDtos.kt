package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductoDto(
    val sku: String,
    val nombre: String,
    val categoria: String,
)

@Serializable
data class StockItemDto(
    val ubicacionId: String,
    val sku: String,
    val qty: Int,
)

