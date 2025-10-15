package cl.clinipets.data.api

import cl.clinipets.data.dto.ProductoDto
import cl.clinipets.data.dto.StockItemDto
import retrofit2.http.GET
import retrofit2.http.Query

interface InventarioApi {
    @GET("/api/inventario/productos")
    suspend fun productos(): List<ProductoDto>

    @GET("/api/inventario/stock")
    suspend fun stock(
        @Query("ubicacionId") ubicacionId: String? = null,
        @Query("sku") sku: String? = null
    ): List<StockItemDto>
}

