// cl/clinipets/navigation/navtypes/JsonNavTypes.kt
package cl.clinipets

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

object NavJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}

class JsonNavType<T : Any>(
    private val serializer: KSerializer<T>,
    private val json: Json = NavJson.instance,
    isNullableAllowed: Boolean = false
) : NavType<T>(isNullableAllowed) {

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun get(bundle: Bundle, key: String): T? {
        val encoded = bundle.getString(key) ?: return null
        val decoded = Uri.decode(encoded)
        return json.decodeFromString(serializer, decoded)
    }

    override fun parseValue(value: String): T {
        val decoded = Uri.decode(value)
        return json.decodeFromString(serializer, decoded)
    }

    override fun serializeAsValue(value: T): String {
        val str = json.encodeToString(serializer, value)
        return Uri.encode(str)
    }
}

object NavTypeRegistry {
    val cache = mutableMapOf<KType, NavType<*>>()

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> get(): NavType<T> = synchronized(cache) {
        val k = typeOf<T>()
        cache.getOrPut(k) {
            JsonNavType(serializer<T>())
        } as NavType<T>
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T : Any> of(json: Json = NavJson.instance): NavType<T> =
        JsonNavType(serializer<T>(), json)
}

inline fun <reified T : Any> typeEntry(): Pair<KType, NavType<*>> =
    typeOf<T>() to NavTypeRegistry.get<T>()
