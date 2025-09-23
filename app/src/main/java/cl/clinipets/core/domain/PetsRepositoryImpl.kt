package cl.clinipets.core.domain

class PetsRepositoryImpl : PetsRepository {
    override suspend fun myPets(): List<Pair<String, String>> {
        TODO("Not yet implemented")
    }
}

class PetsRepositoryImplFake : PetsRepository {
    override suspend fun myPets(): List<Pair<String, String>> {

        return listOf(
            "pet1" to "Fido",
            "pet2" to "Whiskers"
        )

    }
}