package cl.clinipets.domain.inventario

import cl.clinipets.data.dto.ProductoDto
import cl.clinipets.data.dto.StockItemDto

interface InventarioRepository {
    suspend fun productos(): List<ProductoDto>
    suspend fun stock(ubicacionId: String? = null, sku: String? = null): List<StockItemDto>
}

