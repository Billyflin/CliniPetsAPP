
// domain/auth/AuthRepository.kt
package cl.clinipets.domain.auth

import com.google.firebase.auth.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val isAuthenticated: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val userFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // Email/Password Authentication
    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Google Sign In
    suspend fun signInWithGoogle(idToken: String): Result<AuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Phone Authentication
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<AuthResult> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Link Providers
    suspend fun linkWithGoogle(idToken: String): Result<AuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = currentUser?.linkWithCredential(credential)?.await()
            result?.let { Result.success(it) } ?: Result.failure(Exception("No user logged in"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkWithPhone(credential: PhoneAuthCredential): Result<AuthResult> {
        return try {
            val result = currentUser?.linkWithCredential(credential)?.await()
            result?.let { Result.success(it) } ?: Result.failure(Exception("No user logged in"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            val result = currentUser?.linkWithCredential(credential)?.await()
            result?.let { Result.success(it) } ?: Result.failure(Exception("No user logged in"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Unlink Provider
    suspend fun unlinkProvider(providerId: String): Result<FirebaseUser> {
        return try {
            val result = currentUser?.unlink(providerId)?.await()
            result?.let { Result.success(it) } ?: Result.failure(Exception("No user logged in"))
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<FirebaseUser>
    }

    // Password reset
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Get linked providers
    fun getLinkedProviders(): List<String> {
        return currentUser?.providerData?.map { it.providerId } ?: emptyList()
    }
}