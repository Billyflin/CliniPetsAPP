package cl.clinipets.feature.auth.domain

import java.time.OffsetDateTime

data class Sesion(
    val token: String,
    val expiraEn: OffsetDateTime?,
)
