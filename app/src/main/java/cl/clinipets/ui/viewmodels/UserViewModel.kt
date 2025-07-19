// ui/viewmodels/UserViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState

    init {
        loadUserData()
    }

    fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Cargar datos del usuario
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val userName = userDoc.getString("name") ?: "Usuario"
                val userEmail = auth.currentUser?.email ?: ""
                val memberSince = userDoc.getLong("createdAt") ?: System.currentTimeMillis()
                val isVet = userDoc.getBoolean("isVet") == true


                // Contar mascotas
                val petsCount = firestore.collection("users")
                    .document(userId)
                    .collection("pets")
                    .get()
                    .await()
                    .size()

                // Contar citas
                val appointmentsCount = firestore.collection("users")
                    .document(userId)
                    .collection("appointments")
                    .get()
                    .await()
                    .size()

                _userState.value = UserState(
                    userName = userName,
                    userEmail = userEmail,
                    memberSince = SimpleDateFormat("yyyy", Locale.getDefault()).format(
                        Date(
                            memberSince
                        )
                    ),
                    petsCount = petsCount,
                    appointmentsCount = appointmentsCount,
                    isVet = isVet

                )
            } catch (e: Exception) {
                _userState.value = _userState.value.copy(
                    error = "Error al cargar datos: ${e.message}"
                )
            }
        }
    }
}

data class UserState(
    val userName: String = "",
    val userEmail: String = "",
    val memberSince: String = "",
    val petsCount: Int = 0,
    val appointmentsCount: Int = 0,
    val error: String? = null,
    val isVet: Boolean = false
)