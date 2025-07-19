package cl.clinipets.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.R
import cl.clinipets.ui.theme.LocalExtendedColors
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val extColors = LocalExtendedColors.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Credential Manager
    val credentialManager = remember { CredentialManager.create(context) }

    // State for phone login
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var showPhoneLogin by remember { mutableStateOf(false) }

    // Handle sign in result
    fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        scope.launch {
                            snackbarHostState.showSnackbar("Error al procesar la respuesta de Google")
                        }
                    }
                } else {
                    Log.e(TAG, "Unexpected type of credential")
                    scope.launch {
                        snackbarHostState.showSnackbar("Tipo de credencial inesperado")
                    }
                }
            }
            else -> {
                Log.e(TAG, "Unexpected type of credential")
                scope.launch {
                    snackbarHostState.showSnackbar("Tipo de credencial inesperado")
                }
            }
        }
    }

    // Google Sign In
    fun signInWithGoogle() {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Error getting credential", e)
                snackbarHostState.showSnackbar("Error al iniciar sesión con Google")
            }
        }
    }

    // Facebook Sign In (placeholder - necesitarás configurar el SDK de Facebook)
    fun signInWithFacebook() {
        scope.launch {
            snackbarHostState.showSnackbar("Facebook login próximamente")
            // TODO: Implementar Facebook Login SDK
        }
    }

    // Effects
    LaunchedEffect(uiState.isSignInSuccessful) {
        if (uiState.isSignInSuccessful) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Logo section
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = extColors.pink.colorContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = extColors.pink.onColorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Bienvenido a Clinipets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Inicia sesión para continuar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Social Login Buttons
                if (!showPhoneLogin) {
                    // Google Sign In Button
                    Button(
                        onClick = { signInWithGoogle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continuar con Google",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Facebook Sign In Button
                    Button(
                        onClick = { signInWithFacebook() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Facebook,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continuar con Facebook",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Sign In Button
                    Button(
                        onClick = { showPhoneLogin = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extColors.mint.color,
                            contentColor = extColors.mint.onColor
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continuar con Teléfono",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    // Phone Login Form
                    PhoneLoginContent(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        verificationCode = verificationCode,
                        onVerificationCodeChange = { verificationCode = it },
                        phoneVerificationStep = uiState.phoneVerificationStep,
                        onSendCode = {
                            focusManager.clearFocus()
                            viewModel.sendPhoneVerification(
                                "+56$phoneNumber",
                                context as Activity
                            )
                        },
                        onVerifyCode = {
                            focusManager.clearFocus()
                            viewModel.verifyPhoneCode(verificationCode)
                        },
                        onBack = {
                            showPhoneLogin = false
                            phoneNumber = ""
                            verificationCode = ""
                        },
                        isLoading = uiState.isLoading
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Register link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "¿Primera vez en Clinipets? ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            "Crear cuenta",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun PhoneLoginContent(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    verificationCode: String,
    onVerificationCodeChange: (String) -> Unit,
    phoneVerificationStep: PhoneVerificationStep,
    onSendCode: () -> Unit,
    onVerifyCode: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    val extColors = LocalExtendedColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Volver")
        }

        AnimatedContent(
            targetState = phoneVerificationStep,
            transitionSpec = {
                fadeIn() + slideInHorizontally() togetherWith
                        fadeOut() + slideOutHorizontally()
            },
            label = "PhoneVerification"
        ) { step ->
            when (step) {
                PhoneVerificationStep.NONE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Ingresa tu número de teléfono",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 9) onPhoneNumberChange(it) },
                            label = { Text("Número de teléfono") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            prefix = { Text("+56 ") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (phoneNumber.length >= 9) onSendCode() }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = onSendCode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = phoneNumber.length >= 9 && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extColors.mint.color,
                                contentColor = extColors.mint.onColor
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text("Enviar código")
                        }
                    }
                }

                PhoneVerificationStep.CODE_SENT -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Código de verificación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Ingresa el código de 6 dígitos enviado a\n+56 $phoneNumber",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { if (it.length <= 6) onVerificationCodeChange(it) },
                            label = { Text("Código de verificación") },
                            leadingIcon = {
                                Icon(Icons.Default.Pin, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (verificationCode.length == 6) onVerifyCode() }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { /* TODO: Implement resend */ },
                                enabled = !isLoading
                            ) {
                                Text("Reenviar código")
                            }

                            Button(
                                onClick = onVerifyCode,
                                enabled = verificationCode.length == 6 && !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = extColors.mint.color,
                                    contentColor = extColors.mint.onColor
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text("Verificar")
                            }
                        }
                    }
                }

                PhoneVerificationStep.VERIFIED -> {
                    // Este estado se maneja automáticamente
                }
            }
        }
    }
}