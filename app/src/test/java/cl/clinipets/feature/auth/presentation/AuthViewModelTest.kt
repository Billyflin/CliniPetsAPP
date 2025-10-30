package cl.clinipets.feature.auth.presentation

import android.content.Intent
import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.auth.data.GoogleAuthProvider
import cl.clinipets.feature.auth.domain.AuthRepositorio
import cl.clinipets.feature.auth.domain.CerrarSesionUseCase
import cl.clinipets.feature.auth.domain.IniciarSesionConGoogleUseCase
import cl.clinipets.feature.auth.domain.ObtenerPerfilUseCase
import cl.clinipets.feature.auth.domain.ObservarSesionUseCase
import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.openapi.models.LoginResponse
import cl.clinipets.openapi.models.MeResponse
import cl.clinipets.openapi.models.UsuarioInfo
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login exitoso publica sesion y perfil`() = runTest(mainDispatcherRule.testDispatcher) {
        val repo = FakeAuthRepositorio()
        val google = FakeGoogleAuthProvider()
        val loginResponse = crearLoginResponse()
        repo.loginResultado = Resultado.Exito(loginResponse)
        repo.perfilResultado = Resultado.Exito(crearMeResponse(loginResponse))
        google.resultado = Resultado.Exito("google-token")
        val viewModel = crearViewModel(repo, google)

        advanceUntilIdle()
        assertNull(viewModel.estado.value.sesion)

        viewModel.procesarResultadoGoogle(Intent("resultado"))
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertNotNull(estado.sesion)
        assertEquals(loginResponse.token, estado.sesion?.token)
        assertNotNull(estado.perfil)
        assertNull(estado.error)
        assertFalse(estado.cargando)
    }

    @Test
    fun `login con error expone Resultado_Error`() = runTest(mainDispatcherRule.testDispatcher) {
        val repo = FakeAuthRepositorio().apply {
            loginResultado = Resultado.Error(Resultado.Tipo.CLIENTE, mensaje = "Credenciales inválidas")
        }
        val google = FakeGoogleAuthProvider().apply {
            resultado = Resultado.Exito("token")
        }
        val viewModel = crearViewModel(repo, google)

        viewModel.procesarResultadoGoogle(Intent("resultado"))
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertNull(estado.sesion)
        assertNotNull(estado.error)
    }

    @Test
    fun `cuando google falla expone error sin llamar al repositorio`() = runTest(mainDispatcherRule.testDispatcher) {
        val repo = FakeAuthRepositorio()
        val google = FakeGoogleAuthProvider().apply {
            resultado = Resultado.Error(Resultado.Tipo.CLIENTE, mensaje = "Google cancelado")
        }
        val viewModel = crearViewModel(repo, google)

        viewModel.procesarResultadoGoogle(null)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertNull(estado.sesion)
        assertNotNull(estado.error)
    }

    private fun crearViewModel(
        repo: FakeAuthRepositorio,
        google: FakeGoogleAuthProvider,
    ): AuthViewModel {
        return AuthViewModel(
            observarSesion = ObservarSesionUseCase(repo),
            iniciarSesionConGoogle = IniciarSesionConGoogleUseCase(repo),
            obtenerPerfil = ObtenerPerfilUseCase(repo),
            cerrarSesion = CerrarSesionUseCase(repo),
            googleAuthProvider = google,
        )
    }

    private fun crearLoginResponse(): LoginResponse {
        val ahora = OffsetDateTime.parse("2024-01-01T08:00:00Z")
        return LoginResponse(
            token = "jwt-token",
            expiresAt = ahora.plusHours(1),
            usuario = UsuarioInfo(
                id = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"),
                email = "usuario@clinipets.cl",
                roles = listOf("TUTOR"),
            ),
            refreshExpiresAt = ahora.plusDays(3),
        )
    }

    private fun crearMeResponse(login: LoginResponse): MeResponse {
        return MeResponse(
            authenticated = true,
            id = login.usuario.id,
            email = login.usuario.email,
            roles = login.usuario.roles,
        )
    }

    private class FakeAuthRepositorio : AuthRepositorio {
        val sesionFlow = MutableStateFlow<Sesion?>(null)
        var loginResultado: Resultado<LoginResponse> = Resultado.Error(Resultado.Tipo.DESCONOCIDO)
        var perfilResultado: Resultado<MeResponse> = Resultado.Error(Resultado.Tipo.DESCONOCIDO)
        var cerrarResultado: Resultado<Unit> = Resultado.Exito(Unit)

        override val sesion = sesionFlow

        override suspend fun iniciarSesionConGoogle(idToken: String): Resultado<LoginResponse> {
            return when (val resultado = loginResultado) {
                is Resultado.Exito -> {
                    sesionFlow.value = Sesion(
                        token = resultado.dato.token,
                        expiraEn = resultado.dato.expiresAt,
                    )
                    resultado
                }
                is Resultado.Error -> resultado
            }
        }

        override suspend fun obtenerPerfil(): Resultado<MeResponse> = perfilResultado

        override suspend fun cerrarSesion(): Resultado<Unit> = cerrarResultado.also {
            if (it is Resultado.Exito) {
                sesionFlow.value = null
            }
        }
    }

    private class FakeGoogleAuthProvider : GoogleAuthProvider {
        var resultado: Resultado<String> = Resultado.Error(Resultado.Tipo.DESCONOCIDO)
        override fun getSignInIntent(): Intent = Intent("fake-sign-in")

        override fun extractIdTokenFromIntent(data: Intent?): Resultado<String> = resultado

        override fun signOut() { /* no-op */ }
    }
}
