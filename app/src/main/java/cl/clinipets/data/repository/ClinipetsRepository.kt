// data/repository/ClinipetsRepository.kt
package cl.clinipets.data.repository

import cl.clinipets.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClinipetsRepository @Inject constructor() {

    // Mock data para demostración
    private val _pets = MutableStateFlow(
        listOf(
            Pet(
                id = "1",
                name = "Max",
                species = PetSpecies.DOG,
                breed = "Golden Retriever",
                age = 3,
                weight = 30.5f,
                ownerId = "user1"
            ),
            Pet(
                id = "2",
                name = "Luna",
                species = PetSpecies.CAT,
                breed = "Persa",
                age = 2,
                weight = 4.2f,
                ownerId = "user1"
            )
        )
    )


    val pets: Flow<List<Pet>> = _pets.asStateFlow()

    private val _appointments = MutableStateFlow(
        listOf(
            Appointment(
                id = "1",
                petId = "1",
                petName = "Max",
                serviceType = ServiceType("1", "Vacunación", "vaccine", 30, 15000),
                dateTime = System.currentTimeMillis() + 86400000, // Mañana
                veterinarianId = "vet1",
                veterinarianName = "Dra. Andrea Bravo",
                status = AppointmentStatus.CONFIRMED
            )
        )
    )
    val appointments: Flow<List<Appointment>> = _appointments.asStateFlow()

    fun addPet(pet: Pet) {
        _pets.value = _pets.value + pet
    }

    fun addAppointment(appointment: Appointment) {
        _appointments.value = _appointments.value + appointment
    }

    fun updateAppointmentStatus(appointmentId: String, status: AppointmentStatus) {
        _appointments.value = _appointments.value.map { appointment ->
            if (appointment.id == appointmentId) {
                appointment.copy(status = status)
            } else {
                appointment
            }
        }
    }
}