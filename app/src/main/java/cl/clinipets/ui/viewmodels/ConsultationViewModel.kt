// ui/viewmodels/ConsultationViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.InventoryItemType
import cl.clinipets.data.model.MedicalConsultation
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.MedicationUsed
import cl.clinipets.data.model.PaymentMethod
import cl.clinipets.data.model.PaymentStatus
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.ServiceProvided
import cl.clinipets.data.model.VaccinationRecord
import cl.clinipets.data.model.Vaccine
import cl.clinipets.data.model.VaccineApplied
import cl.clinipets.data.model.VeterinaryService
import com.google.firebase.auth.ktx.auth
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
class ConsultationViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _consultationState = MutableStateFlow(ConsultationState())
    val consultationState: StateFlow<ConsultationState> = _consultationState

    fun startConsultation(appointmentId: String) {
        val veterinarianId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _consultationState.value = _consultationState.value.copy(isLoading = true)

                // Obtener datos de la cita
                val appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .await()

                val appointment =
                    appointmentDoc.toObject<Appointment>()?.copy(id = appointmentDoc.id)

                if (appointment != null) {
                    // Crear consulta médica
                    val consultation = MedicalConsultation(
                        appointmentId = appointmentId,
                        petId = appointment.petId,
                        veterinarianId = veterinarianId,
                        startTime = System.currentTimeMillis()
                    )

                    val consultationRef = firestore.collection("consultations")
                        .add(consultation)
                        .await()

                    // Actualizar estado de la cita
                    firestore.collection("appointments")
                        .document(appointmentId)
                        .update(
                            mapOf(
                                "status" to AppointmentStatus.IN_PROGRESS.name,
                                "consultationId" to consultationRef.id
                            )
                        )
                        .await()

                    // Cargar información de la mascota
                    val petDoc = firestore.collection("pets")
                        .document(appointment.petId)
                        .get()
                        .await()

                    val pet = petDoc.toObject<Pet>()?.copy(id = petDoc.id)

                    _consultationState.value = _consultationState.value.copy(
                        isLoading = false,
                        activeConsultation = consultation.copy(id = consultationRef.id),
                        currentPet = pet,
                        currentAppointment = appointment
                    )
                }
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    isLoading = false,
                    error = "Error al iniciar consulta: ${e.message}"
                )
            }
        }
    }

    fun updateClinicalData(
        weight: Float? = null,
        temperature: Float? = null,
        heartRate: Int? = null,
        respiratoryRate: Int? = null,
        symptoms: String = "",
        clinicalExam: String = "",
        diagnosis: String = "",
        treatment: String = "",
        observations: String = "",
        recommendations: String = ""
    ) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "weight" to weight,
                    "temperature" to temperature,
                    "heartRate" to heartRate,
                    "respiratoryRate" to respiratoryRate,
                    "symptoms" to symptoms,
                    "clinicalExam" to clinicalExam,
                    "diagnosis" to diagnosis,
                    "treatment" to treatment,
                    "observations" to observations,
                    "recommendations" to recommendations,
                    "lastUpdated" to System.currentTimeMillis()
                ).filterValues { it != null }

                firestore.collection("consultations")
                    .document(consultationId)
                    .update(updates)
                    .await()

                // Actualizar estado local
                val updatedConsultation = _consultationState.value.activeConsultation?.copy(
                    weight = weight ?: _consultationState.value.activeConsultation!!.weight,
                    temperature = temperature
                        ?: _consultationState.value.activeConsultation!!.temperature,
                    heartRate = heartRate
                        ?: _consultationState.value.activeConsultation!!.heartRate,
                    respiratoryRate = respiratoryRate
                        ?: _consultationState.value.activeConsultation!!.respiratoryRate,
                    symptoms = symptoms,
                    clinicalExam = clinicalExam,
                    diagnosis = diagnosis,
                    treatment = treatment,
                    observations = observations,
                    recommendations = recommendations
                )

                _consultationState.value = _consultationState.value.copy(
                    activeConsultation = updatedConsultation,
                    isClinicalDataSaved = true
                )
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al guardar datos clínicos: ${e.message}"
                )
            }
        }
    }

    fun addService(serviceId: String, customPrice: Double? = null) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                val serviceDoc = firestore.collection("services")
                    .document(serviceId)
                    .get()
                    .await()

                val service = serviceDoc.toObject<VeterinaryService>()

                if (service != null) {
                    val serviceProvided = ServiceProvided(
                        serviceId = serviceId,
                        name = service.name,
                        category = service.category,
                        price = customPrice ?: service.basePrice
                    )

                    val consultationRef = firestore.collection("consultations")
                        .document(consultationId)

                    firestore.runTransaction { transaction ->
                        val consultationDoc = transaction.get(consultationRef)
                        val consultation = consultationDoc.toObject<MedicalConsultation>()

                        if (consultation != null) {
                            val updatedServices = consultation.services + serviceProvided
                            val subtotal = updatedServices.sumOf { it.price }
                            val total = subtotal - consultation.discount

                            transaction.update(
                                consultationRef, mapOf(
                                    "services" to updatedServices,
                                    "subtotal" to subtotal,
                                    "total" to total
                                )
                            )
                        }
                    }.await()

                    loadConsultation(consultationId)
                }
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al agregar servicio: ${e.message}"
                )
            }
        }
    }

    fun addMedication(
        medicationId: String,
        dose: String,
        frequency: String,
        duration: String,
        route: String,
        quantity: Int
    ) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                val medicationDoc = firestore.collection("medications")
                    .document(medicationId)
                    .get()
                    .await()

                val medication = medicationDoc.toObject<Medication>()

                if (medication != null) {
                    val medicationUsed = MedicationUsed(
                        medicationId = medicationId,
                        name = medication.name,
                        dose = dose,
                        frequency = frequency,
                        duration = duration,
                        route = route,
                        quantity = quantity,
                        unitPrice = medication.unitPrice,
                        totalPrice = medication.unitPrice * quantity
                    )

                    val consultationRef = firestore.collection("consultations")
                        .document(consultationId)

                    firestore.runTransaction { transaction ->
                        val consultationDoc = transaction.get(consultationRef)
                        val consultation = consultationDoc.toObject<MedicalConsultation>()

                        if (consultation != null) {
                            val updatedMedications = consultation.medications + medicationUsed
                            val medicationsTotal = updatedMedications.sumOf { it.totalPrice }
                            val servicesTotal = consultation.services.sumOf { it.price }
                            val vaccinesTotal = consultation.vaccines.sumOf { it.price }
                            val subtotal = servicesTotal + medicationsTotal + vaccinesTotal
                            val total = subtotal - consultation.discount

                            transaction.update(
                                consultationRef, mapOf(
                                    "medications" to updatedMedications,
                                    "subtotal" to subtotal,
                                    "total" to total
                                )
                            )
                        }
                    }.await()

                    // Notificar al InventoryViewModel para actualizar stock
                    _consultationState.value = _consultationState.value.copy(
                        pendingInventoryMovements = _consultationState.value.pendingInventoryMovements +
                                PendingInventoryMovement(
                                    itemId = medicationId,
                                    itemType = InventoryItemType.MEDICATION,
                                    quantity = -quantity,
                                    reason = "Uso en consulta $consultationId"
                                )
                    )

                    loadConsultation(consultationId)
                }
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al agregar medicamento: ${e.message}"
                )
            }
        }
    }

    fun addVaccine(
        vaccineId: String,
        batch: String,
        expirationDate: Long,
        nextDoseDate: Long? = null
    ) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return
        val petId = _consultationState.value.activeConsultation?.petId ?: return

        viewModelScope.launch {
            try {
                val vaccineDoc = firestore.collection("vaccines")
                    .document(vaccineId)
                    .get()
                    .await()

                val vaccine = vaccineDoc.toObject<Vaccine>()

                if (vaccine != null) {
                    val vaccineApplied = VaccineApplied(
                        vaccineId = vaccineId,
                        name = vaccine.name,
                        batch = batch,
                        expirationDate = expirationDate,
                        nextDoseDate = nextDoseDate,
                        price = vaccine.unitPrice
                    )

                    val consultationRef = firestore.collection("consultations")
                        .document(consultationId)

                    firestore.runTransaction { transaction ->
                        val consultationDoc = transaction.get(consultationRef)
                        val consultation = consultationDoc.toObject<MedicalConsultation>()

                        if (consultation != null) {
                            val updatedVaccines = consultation.vaccines + vaccineApplied
                            val vaccinesTotal = updatedVaccines.sumOf { it.price }
                            val servicesTotal = consultation.services.sumOf { it.price }
                            val medicationsTotal = consultation.medications.sumOf { it.totalPrice }
                            val subtotal = servicesTotal + medicationsTotal + vaccinesTotal
                            val total = subtotal - consultation.discount

                            transaction.update(
                                consultationRef, mapOf(
                                    "vaccines" to updatedVaccines,
                                    "subtotal" to subtotal,
                                    "total" to total
                                )
                            )

                            // Crear registro de vacunación
                            val vaccinationRecord = VaccinationRecord(
                                petId = petId,
                                vaccineId = vaccineId,
                                vaccineName = vaccine.name,
                                consultationId = consultationId,
                                veterinarianId = consultation.veterinarianId,
                                applicationDate = System.currentTimeMillis(),
                                batch = batch,
                                expirationDate = expirationDate,
                                nextDoseDate = nextDoseDate,
                                certificateNumber = generateCertificateNumber()
                            )

                            firestore.collection("vaccinations")
                                .add(vaccinationRecord)
                        }
                    }.await()

                    // Notificar al InventoryViewModel
                    _consultationState.value = _consultationState.value.copy(
                        pendingInventoryMovements = _consultationState.value.pendingInventoryMovements +
                                PendingInventoryMovement(
                                    itemId = vaccineId,
                                    itemType = InventoryItemType.VACCINE,
                                    quantity = -1,
                                    reason = "Vacunación en consulta $consultationId"
                                )
                    )

                    loadConsultation(consultationId)
                }
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al aplicar vacuna: ${e.message}"
                )
            }
        }
    }

    fun finishConsultation(
        discount: Double = 0.0,
        discountReason: String? = null,
        paymentMethod: PaymentMethod,
        amountPaid: Double
    ) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return
        val appointmentId = _consultationState.value.activeConsultation?.appointmentId ?: return

        viewModelScope.launch {
            try {
                _consultationState.value = _consultationState.value.copy(isLoading = true)

                val consultationRef = firestore.collection("consultations")
                    .document(consultationId)

                firestore.runTransaction { transaction ->
                    val consultationDoc = transaction.get(consultationRef)
                    val consultation = consultationDoc.toObject<MedicalConsultation>()

                    if (consultation != null) {
                        val total = consultation.subtotal - discount
                        val paymentStatus = when {
                            amountPaid >= total -> PaymentStatus.PAID
                            amountPaid > 0 -> PaymentStatus.PARTIAL
                            else -> PaymentStatus.PENDING
                        }

                        transaction.update(
                            consultationRef, mapOf(
                                "discount" to discount,
                                "discountReason" to discountReason,
                                "total" to total,
                                "amountPaid" to amountPaid,
                                "paymentStatus" to paymentStatus.name,
                                "paymentMethod" to paymentMethod.name,
                                "endTime" to System.currentTimeMillis()
                            )
                        )

                        // Actualizar estado de la cita
                        val appointmentRef = firestore.collection("appointments")
                            .document(appointmentId)
                        transaction.update(
                            appointmentRef,
                            "status", AppointmentStatus.COMPLETED.name
                        )
                    }
                }.await()

                _consultationState.value = _consultationState.value.copy(
                    isLoading = false,
                    isConsultationFinished = true,
                    activeConsultation = null
                )
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    isLoading = false,
                    error = "Error al finalizar consulta: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadConsultation(consultationId: String) {
        try {
            val doc = firestore.collection("consultations")
                .document(consultationId)
                .get()
                .await()

            val consultation = doc.toObject<MedicalConsultation>()?.copy(id = doc.id)

            _consultationState.value = _consultationState.value.copy(
                activeConsultation = consultation
            )
        } catch (e: Exception) {
            // Error handling silencioso
        }
    }

    private fun generateCertificateNumber(): String {
        return "CERT-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    fun clearState() {
        _consultationState.value = ConsultationState()
    }
}

data class ConsultationState(
    val activeConsultation: MedicalConsultation? = null,
    val currentPet: Pet? = null,
    val currentAppointment: Appointment? = null,
    val pendingInventoryMovements: List<PendingInventoryMovement> = emptyList(),
    val isLoading: Boolean = false,
    val isClinicalDataSaved: Boolean = false,
    val isConsultationFinished: Boolean = false,
    val error: String? = null
)

data class PendingInventoryMovement(
    val itemId: String,
    val itemType: InventoryItemType,
    val quantity: Int,
    val reason: String
)