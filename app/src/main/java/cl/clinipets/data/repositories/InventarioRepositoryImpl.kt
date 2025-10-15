package cl.clinipets.data.repositories

import cl.clinipets.data.api.InventarioApi
import cl.clinipets.data.dto.ProductoDto
import cl.clinipets.data.dto.StockItemDto
import cl.clinipets.domain.inventario.InventarioRepository

class InventarioRepositoryImpl(private val api: InventarioApi) : InventarioRepository {
    override suspend fun productos(): List<ProductoDto> = api.productos()
    override suspend fun stock(ubicacionId: String?, sku: String?): List<StockItemDto> =
        api.stock(ubicacionId, sku)
}

