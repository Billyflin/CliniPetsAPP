// core/domain/PetsRepository.kt
package cl.clinipets.core.domain


interface PetsRepository {
    suspend fun myPets(): List<Pair<String,String>> // (id, nombre)
}

