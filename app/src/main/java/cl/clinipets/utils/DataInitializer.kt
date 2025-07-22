// utils/DataInitializer.kt
package cl.clinipets.utils

import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.MedicationPresentation
import cl.clinipets.data.model.Service
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.data.model.Vaccine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object DataInitializer {
    private val firestore: FirebaseFirestore
        get() = Firebase.firestore

    suspend fun initializeBasicServices() {
        val services = listOf(
            Service(
                name = "Consulta General",
                category = ServiceCategory.CONSULTATION,
                basePrice = 25000.0
            ),
            Service(
                name = "Vacuna Séxtuple",
                category = ServiceCategory.VACCINATION,
                basePrice = 35000.0
            ),
            Service(
                name = "Vacuna Antirrábica",
                category = ServiceCategory.VACCINATION,
                basePrice = 20000.0
            ),
            Service(
                name = "Cirugía Esterilización",
                category = ServiceCategory.SURGERY,
                basePrice = 120000.0
            ),
            Service(
                name = "Baño y Corte",
                category = ServiceCategory.GROOMING,
                basePrice = 20000.0
            ),
            Service(name = "Desparasitación", category = ServiceCategory.OTHER, basePrice = 15000.0)
        )

        services.forEach { service ->
            try {
                // Verificar si ya existe
                val existing = firestore.collection("services")
                    .whereEqualTo("name", service.name)
                    .get()
                    .await()

                if (existing.isEmpty) {
                    firestore.collection("services")
                        .add(service)
                        .await()
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    suspend fun initializeCommonMedications() {
        val medications = listOf(
            Medication(
                name = "Dexametasona",
                presentation = MedicationPresentation.INJECTION,
                stock = 10,
                unitPrice = 5000.0
            ),
            Medication(
                name = "Amoxicilina",
                presentation = MedicationPresentation.TABLET,
                stock = 50,
                unitPrice = 1000.0
            ),
            Medication(
                name = "Meloxicam",
                presentation = MedicationPresentation.SYRUP,
                stock = 5,
                unitPrice = 15000.0
            ),
            Medication(
                name = "Tramadol",
                presentation = MedicationPresentation.DROPS,
                stock = 8,
                unitPrice = 12000.0
            ),
            Medication(
                name = "Cefalexina",
                presentation = MedicationPresentation.TABLET,
                stock = 30,
                unitPrice = 1500.0
            )
        )

        medications.forEach { medication ->
            try {
                val existing = firestore.collection("medications")
                    .whereEqualTo("name", medication.name)
                    .get()
                    .await()

                if (existing.isEmpty) {
                    firestore.collection("medications")
                        .add(medication)
                        .await()
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    suspend fun initializeCommonVaccines() {
        val vaccines = listOf(
            Vaccine(name = "Séxtuple Canina", stock = 20, unitPrice = 35000.0),
            Vaccine(name = "Triple Felina", stock = 15, unitPrice = 30000.0),
            Vaccine(name = "Antirrábica", stock = 30, unitPrice = 20000.0),
            Vaccine(name = "Leucemia Felina", stock = 10, unitPrice = 40000.0)
        )

        vaccines.forEach { vaccine ->
            try {
                val existing = firestore.collection("vaccines")
                    .whereEqualTo("name", vaccine.name)
                    .get()
                    .await()

                if (existing.isEmpty) {
                    firestore.collection("vaccines")
                        .add(vaccine)
                        .await()
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}