// ui/viewmodels/InventoryViewModel.kt
package cl.clinipets.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.MedicationPresentation
import cl.clinipets.data.model.Service
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.data.model.Vaccine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
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
                    .whereEqualTo("active", true)
                    .orderBy("name")
                    .get()
                    .await()

                val medications = medicationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Medication>()?.copy(id = doc.id)
                }

                // Cargar vacunas
                val vaccinesSnapshot = firestore.collection("vaccines")
                    .whereEqualTo("active", true)
                    .orderBy("name")
                    .get()
                    .await()

                val vaccines = vaccinesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Vaccine>()?.copy(id = doc.id)
                }

                // Cargar servicios
                val servicesSnapshot = firestore.collection("services")
                    .whereEqualTo("active", true)
                    .orderBy("category")
                    .orderBy("name")
                    .get()
                    .await()

                val services = servicesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Service>()?.copy(id = doc.id)
                }

                // Actualizar el estado del inventario
                Log.d("InventoryViewModel", "Medications: $medications")
                Log.d("InventoryViewModel", "Vaccines: $vaccines")
                Log.d("InventoryViewModel", "Services: $services")
                _inventoryState.value = _inventoryState.value.copy(
                    medications = medications,
                    vaccines = vaccines,
                    services = services,
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
        presentation: MedicationPresentation,
        stock: Int,
        unitPrice: Double
    ) {
        viewModelScope.launch {
            try {
                val medication = Medication(
                    name = name,
                    presentation = presentation,
                    stock = stock,
                    unitPrice = unitPrice,
                    active = true
                )

                firestore.collection("medications")
                    .add(medication)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al agregar medicamento: ${e.message}"
                )
            }
        }
    }

    fun addVaccine(name: String, stock: Int, unitPrice: Double) {
        viewModelScope.launch {
            try {
                val vaccine = Vaccine(
                    name = name,
                    stock = stock,
                    unitPrice = unitPrice,
                    active = true
                )

                firestore.collection("vaccines")
                    .add(vaccine)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al agregar vacuna: ${e.message}"
                )
            }
        }
    }

    fun addService(name: String, category: ServiceCategory, basePrice: Double) {
        viewModelScope.launch {
            try {
                val service = Service(
                    name = name,
                    category = category,
                    basePrice = basePrice,
                    isActive = true
                )

                firestore.collection("services")
                    .add(service)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al agregar servicio: ${e.message}"
                )
            }
        }
    }

    fun updateMedicationStock(medicationId: String, newStock: Int) {
        viewModelScope.launch {
            try {
                firestore.collection("medications")
                    .document(medicationId)
                    .update("stock", newStock)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al actualizar stock: ${e.message}"
                )
            }
        }
    }

    fun updateVaccineStock(vaccineId: String, newStock: Int) {
        viewModelScope.launch {
            try {
                firestore.collection("vaccines")
                    .document(vaccineId)
                    .update("stock", newStock)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al actualizar stock: ${e.message}"
                )
            }
        }
    }

    fun updateServicePrice(serviceId: String, newPrice: Double) {
        viewModelScope.launch {
            try {
                firestore.collection("services")
                    .document(serviceId)
                    .update("basePrice", newPrice)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al actualizar precio: ${e.message}"
                )
            }
        }
    }

    fun deleteItem(collection: String, itemId: String) {
        viewModelScope.launch {
            try {
                firestore.collection(collection)
                    .document(itemId)
                    .update("isActive", false)
                    .await()

                loadInventory()
            } catch (e: Exception) {
                _inventoryState.value = _inventoryState.value.copy(
                    error = "Error al eliminar: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _inventoryState.value = _inventoryState.value.copy(error = null)
    }
}

data class InventoryState(
    val medications: List<Medication> = emptyList(),
    val vaccines: List<Vaccine> = emptyList(),
    val services: List<Service> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)