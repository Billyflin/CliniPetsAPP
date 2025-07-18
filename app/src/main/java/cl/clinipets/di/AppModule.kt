// di/AppModule.kt
package cl.clinipets.di

import cl.clinipets.data.repository.ClinipetsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClinipetsRepository(): ClinipetsRepository {
        return ClinipetsRepository()
    }
}