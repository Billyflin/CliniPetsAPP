// ui/viewmodels/InventoryViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.InventoryItemType
import cl.clinipets.data.model.InventoryMovement
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.MedicationCategory
import cl.clinipets.data.model.MovementType
import cl.clinipets.data.model.Vaccine
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _inventoryState = MutableStateFlow(InventoryState())
    val inventoryState: StateFlow<InventoryState> = _inventoryState

    init {
        loadInventory()
    }

    fun loadInventory() {
        viewModelScope.launch {
            try {
                _inventoryState.value = _inventoryState.value.copy(isLoading = true)

                // Cargar medicamentos
                val medicationsSnapshot = firestore.collection("medications")
                    .whereEqualTo("isActive", true)
                    .orderBy("name")
                    .get()
                    .await()

                val medications = medicationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Medication>()?.copy(id = doc.id)
                }

                // Cargar vacunas
                val vaccinesSnapshot = firestore.collection("vaccines")
                    .whereEqualTo("isActive", true)
                    .orderBy("name")
                    .get()
                    .await()

                val vaccines = vaccinesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Vaccine>()?.copy(id = doc.id)
                }

                // Calcular estadísticas
                val totalItems = medications.size + vaccines.size
                val totalValue = medications.sumOf { it.stock * it.unitPrice } +
                        vaccines.sumOf { it.stock * it.unitPrice }
                val lowStockMedications = medications.filter { it.stock <= it.minStock }
                val lowStockVaccines = vaccines.filter { it.stock <= it.minStock }

                _inventoryState.value = _inventoryState.value.copy(
                    medications = medications,
                    vaccines = vaccines,
                    lowStockMedications = lowStockMedications,
                    lowStockVaccines = lowStockVaccines,
                    totalItems = totalItems,
                    totalValue = totalValue,
                    lowStockCount = lowStockMedications.size + lowStockVaccines.size,
                    isLoading = false
                )
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    isLoading = false,
                    error = "Error al cargar inventario: ${e.message}"
                )
            }
        }
    }

    fun addMedication(
        name: String,
        activeIngredient: String,
        presentation: String,
        laboratory: String,
        category: MedicationCategory,
        unitPrice: Double,
        purchasePrice: Double,
        initialStock: Int,
        minStock: Int,
        expirationDate: Long?,
        batch: String,
        isControlled: Boolean
    ) {
        viewModelScope.launch {
            try {
                _inventoryState.value = _inventoryState.value.copy(isLoading = true)

                val medication = Medication(
                    name = name,
                    activeIngredient = activeIngredient,
                    presentation = presentation,
                    laboratory = laboratory,
                    category = category,
                    unitPrice = unitPrice,
                    purchasePrice = purchasePrice,
                    stock = initialStock,
                    minStock = minStock,
                    expirationDate = expirationDate,
                    batch = batch,
                    isControlled = isControlled,
                    isActive = true,
                    lastUpdated = System.currentTimeMillis()
                )

                val docRef = firestore.collection("medications")
                    .add(medication)
                    .await()

                // Registrar movimiento inicial
                recordInventoryMovement(
                    itemId = docRef.id,
                    itemType = InventoryItemType.MEDICATION,
                    movementType = MovementType.IN,
                    quantity = initialStock,
                    unitPrice = purchasePrice,
                    reason = "Stock inicial"
                )

                _inventoryState.value = _inventoryState.value.copy(
                    isLoading = false,
                    isMedicationAdded = true
                )

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    isLoading = false,
                    error = "Error al agregar medicamento: ${e.message}"
                )
            }
        }
    }

    fun updateMedicationStock(
        medicationId: String,
        newStock: Int,
        reason: String,
        unitPrice: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val medicationRef = firestore.collection("medications").document(medicationId)

                firestore.runTransaction { transaction ->
                    val medicationDoc = transaction.get(medicationRef)
                    val currentStock = medicationDoc.getLong("stock")?.toInt() ?: 0
                    val difference = newStock - currentStock

                    transaction.update(
                        medicationRef, mapOf(
                            "stock" to newStock,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                    )

                    // Determinar tipo de movimiento
                    val movementType = when {
                        difference > 0 -> MovementType.IN
                        difference < 0 && reason.contains(
                            "consulta",
                            ignoreCase = true
                        ) -> MovementType.OUT

                        difference < 0 && reason.contains(
                            "vencido",
                            ignoreCase = true
                        ) -> MovementType.EXPIRED

                        difference < 0 && reason.contains(
                            "dañado",
                            ignoreCase = true
                        ) -> MovementType.DAMAGED

                        else -> MovementType.ADJUSTMENT
                    }

                    // Registrar movimiento
                    val movement = InventoryMovement(
                        itemId = medicationId,
                        itemType = InventoryItemType.MEDICATION,
                        movementType = movementType,
                        quantity = kotlin.math.abs(difference),
                        unitPrice = unitPrice ?: medicationDoc.getDouble("unitPrice") ?: 0.0,
                        totalPrice = kotlin.math.abs(difference) * (unitPrice
                            ?: medicationDoc.getDouble("unitPrice") ?: 0.0),
                        reason = reason,
                        performedBy = auth.currentUser?.uid ?: "",
                        timestamp = System.currentTimeMillis()
                    )

                    firestore.collection("inventory_movements")
                        .add(movement)
                }.await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al actualizar stock: ${e.message}"
                )
            }
        }
    }

    fun updateVaccineStock(
        vaccineId: String,
        newStock: Int,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                val vaccineRef = firestore.collection("vaccines").document(vaccineId)

                firestore.runTransaction { transaction ->
                    val vaccineDoc = transaction.get(vaccineRef)
                    val currentStock = vaccineDoc.getLong("stock")?.toInt() ?: 0
                    val difference = newStock - currentStock

                    transaction.update(
                        vaccineRef, mapOf(
                            "stock" to newStock,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                    )

                    val movementType = when {
                        difference > 0 -> MovementType.IN
                        difference < 0 && reason.contains(
                            "vacunación",
                            ignoreCase = true
                        ) -> MovementType.OUT

                        else -> MovementType.ADJUSTMENT
                    }

                    val movement = InventoryMovement(
                        itemId = vaccineId,
                        itemType = InventoryItemType.VACCINE,
                        movementType = movementType,
                        quantity = kotlin.math.abs(difference),
                        unitPrice = vaccineDoc.getDouble("unitPrice") ?: 0.0,
                        totalPrice = kotlin.math.abs(difference) * (vaccineDoc.getDouble("unitPrice")
                            ?: 0.0),
                        reason = reason,
                        performedBy = auth.currentUser?.uid ?: "",
                        timestamp = System.currentTimeMillis()
                    )

                    firestore.collection("inventory_movements")
                        .add(movement)
                }.await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al actualizar stock de vacuna: ${e.message}"
                )
            }
        }
    }

    fun processConsultationInventoryMovements(
        consultationId: String,
        movements: List<PendingInventoryMovement>
    ) {
        viewModelScope.launch {
            try {
                movements.forEach { movement ->
                    when (movement.itemType) {
                        InventoryItemType.MEDICATION -> {
                            updateMedicationStock(
                                medicationId = movement.itemId,
                                newStock = getCurrentStock(
                                    movement.itemId,
                                    movement.itemType
                                ) + movement.quantity,
                                reason = movement.reason
                            )
                        }

                        InventoryItemType.VACCINE -> {
                            updateVaccineStock(
                                vaccineId = movement.itemId,
                                newStock = getCurrentStock(
                                    movement.itemId,
                                    movement.itemType
                                ) + movement.quantity,
                                reason = movement.reason
                            )
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al procesar movimientos de inventario: ${e.message}"
                )
            }
        }
    }

    private suspend fun getCurrentStock(itemId: String, itemType: InventoryItemType): Int {
        return when (itemType) {
            InventoryItemType.MEDICATION -> {
                val doc = firestore.collection("medications").document(itemId).get().await()
                doc.getLong("stock")?.toInt() ?: 0
            }

            InventoryItemType.VACCINE -> {
                val doc = firestore.collection("vaccines").document(itemId).get().await()
                doc.getLong("stock")?.toInt() ?: 0
            }

            else -> 0
        }
    }

    fun loadInventoryMovements(itemId: String? = null, limit: Int = 50) {
        viewModelScope.launch {
            try {
                var query = firestore.collection("inventory_movements")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())

                if (itemId != null) {
                    query = query.whereEqualTo("itemId", itemId)
                }

                val snapshot = query.get().await()

                val movements = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<InventoryMovement>()?.copy(id = doc.id)
                }

                _inventoryState.value = _inventoryState.value.copy(
                    inventoryMovements = movements
                )
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al cargar movimientos: ${e.message}"
                )
            }
        }
    }

    fun searchMedications(query: String) {
        val filtered = if (query.isBlank()) {
            _inventoryState.value.medications
        } else {
            _inventoryState.value.medications.filter { medication ->
                medication.name.contains(query, ignoreCase = true) ||
                        medication.activeIngredient.contains(query, ignoreCase = true) ||
                        medication.laboratory.contains(query, ignoreCase = true) ||
                        medication.presentation.contains(query, ignoreCase = true)
            }
        }

        _inventoryState.value = _inventoryState.value.copy(
            filteredMedications = filtered,
            searchQuery = query
        )
    }

    fun checkExpirations() {
        viewModelScope.launch {
            val thirtyDaysFromNow = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)

            val expiringMedications = _inventoryState.value.medications.filter { medication ->
                medication.expirationDate?.let { it <= thirtyDaysFromNow } ?: false
            }

            _inventoryState.value = _inventoryState.value.copy(
                expiringMedications = expiringMedications
            )
        }
    }

    private suspend fun recordInventoryMovement(
        itemId: String,
        itemType: InventoryItemType,
        movementType: MovementType,
        quantity: Int,
        unitPrice: Double,
        reason: String,
        consultationId: String? = null,
        supplierId: String? = null,
        invoiceNumber: String? = null
    ) {
        val movement = InventoryMovement(
            itemId = itemId,
            itemType = itemType,
            movementType = movementType,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = quantity * unitPrice,
            reason = reason,
            consultationId = consultationId,
            supplierId = supplierId,
            invoiceNumber = invoiceNumber,
            performedBy = auth.currentUser?.uid ?: "",
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("inventory_movements")
            .add(movement)
            .await()
    }

    fun clearState() {
        _inventoryState.value = _inventoryState.value.copy(
            isMedicationAdded = false,
            isVaccineAdded = false,
            error = null
        )
    }
}

data class InventoryState(
    val medications: List<Medication> = emptyList(),
    val vaccines: List<Vaccine> = emptyList(),
    val filteredMedications: List<Medication> = emptyList(),
    val lowStockMedications: List<Medication> = emptyList(),
    val lowStockVaccines: List<Vaccine> = emptyList(),
    val expiringMedications: List<Medication> = emptyList(),
    val inventoryMovements: List<InventoryMovement> = emptyList(),
    val totalItems: Int = 0,
    val totalValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isMedicationAdded: Boolean = false,
    val isVaccineAdded: Boolean = false,
    val error: String? = null
)