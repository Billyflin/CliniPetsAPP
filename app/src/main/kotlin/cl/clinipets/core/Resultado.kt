package cl.clinipets.core

/**
 * Contenedor sellado que expresa el resultado de operaciones asíncronas sin exponer
 * detalles HTTP a las capas superiores.
 */
sealed class Resultado<out T> {

    data class Exito<out T>(val dato: T) : Resultado<T>()

    data class Error(
        val tipo: Tipo,
        val mensaje: String? = null,
        val causa: Throwable? = null,
        val codigoHttp: Int? = null,
    ) : Resultado<Nothing>()

    enum class Tipo {
        RED,
        CLIENTE,
        SERVIDOR,
        DESCONOCIDO,
    }
}

inline fun <T> Resultado<T>.siEsExito(block: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Exito) {
        block(dato)
    }
    return this
}

inline fun <T> Resultado<T>.siEsError(block: (Resultado.Error) -> Unit): Resultado<T> {
    if (this is Resultado.Error) {
        block(this)
    }
    return this
}
