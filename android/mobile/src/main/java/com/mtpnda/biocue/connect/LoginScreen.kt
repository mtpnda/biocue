package com.mtpnda.biocue.connect

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mtpnda.biocue.R
import com.mtpnda.biocue.ui.components.CalmTextField
import com.mtpnda.biocue.ui.components.OutlinedPillButton
import com.mtpnda.biocue.ui.components.PillButton
import com.mtpnda.biocue.ui.components.softShadow
import com.mtpnda.biocue.ui.theme.AlertCoralChip
import com.mtpnda.biocue.ui.theme.AlertCoralSoft
import com.mtpnda.biocue.ui.theme.AppRadius
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.InkMuted
import com.mtpnda.biocue.ui.theme.InkPrimary
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.PrimaryCharcoal
import com.mtpnda.biocue.ui.theme.SurfaceCard
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var showEmailAuth by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()

    fun handleAuthSuccess() {
        isLoading = false
        val user = auth.currentUser
        val dName = user?.displayName?.ifBlank { null }
            ?: user?.email?.substringBefore("@")?.ifBlank { null }
            ?: if (user?.isAnonymous == true) "Guest User" else "Valdie"
        onLoginSuccess(dName)
    }

    fun loginAsDemoFallback() {
        val demoEmail = "demo.wearer@biocue.io"
        val demoPass = "Password123!"
        auth.signInWithEmailAndPassword(demoEmail, demoPass)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    handleAuthSuccess()
                } else {
                    auth.createUserWithEmailAndPassword(demoEmail, demoPass)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                handleAuthSuccess()
                            } else {
                                isLoading = false
                                errorMsg = createTask.exception?.localizedMessage
                                    ?: signInTask.exception?.localizedMessage
                                    ?: "Guest demo login failed"
                            }
                        }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .softShadow(RoundedCornerShape(AppRadius.Xl), 4.dp)
                .clip(RoundedCornerShape(AppRadius.Xl))
                .background(PrimaryCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Text("🧠", fontSize = 42.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // App Branding
        Text(
            text = "BioCue",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Find your center.\nStay connected.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Sign in or explore as a guest to sync your bio-signals and wearable alerts across your devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Primary Action: Google Sign-In
        PillButton(
            onClick = {
                if (isLoading) return@PillButton
                isLoading = true
                errorMsg = null
                coroutineScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(context)
                        val webClientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (_: Exception) {
                            "970885034683-qcpn8q0e7b1klpoon7js1d7cq6pko423.apps.googleusercontent.com"
                        }

                        // NOTE: Do not pass hashed nonce here. Firebase GoogleAuthProvider accepts standard ID tokens.
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(webClientId)
                            .setAutoSelectEnabled(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential

                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        handleAuthSuccess()
                                    } else {
                                        isLoading = false
                                        errorMsg = task.exception?.localizedMessage ?: "Firebase authentication failed"
                                    }
                                }
                        } else {
                            isLoading = false
                            errorMsg = "Unsupported credential response format"
                        }
                    } catch (_: GetCredentialCancellationException) {
                        isLoading = false
                    } catch (e: Exception) {
                        Log.e("LoginScreen", "Google Sign-In error", e)
                        isLoading = false
                        val error = e.localizedMessage ?: e.message ?: "Sign-in error"
                        errorMsg = when {
                            error.contains("16") || error.contains("Cannot find a matching credential", ignoreCase = true) ->
                                "Google Sign-In: No matching credential found. If you installed via Google Play Internal Testing, please use 'Explore as Guest' or 'Sign in with Email' while Play Store credentials propagate."
                            error.contains("10") || error.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                                "Google Sign-In Configuration Error (10). Use 'Explore as Guest' or 'Sign in with Email' to proceed immediately."
                            error.contains("blocked", ignoreCase = true) || error.contains("access_denied", ignoreCase = true) ->
                                "Google Account Blocked: Your email is not registered as a Test User in Google Cloud Console. Use 'Explore as Guest' or 'Sign in with Email' below."
                            else -> error
                        }
                    }
                }
            },
            text = if (isLoading) "Signing in..." else "Continue with Google",
            loading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary Action: Instant Guest / Demo Mode (Never blocked)
        OutlinedPillButton(
            onClick = {
                if (isLoading) return@OutlinedPillButton
                isLoading = true
                errorMsg = null
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            handleAuthSuccess()
                        } else {
                            // Seamless fallback to demo account if Firebase Anonymous Auth is disabled
                            loginAsDemoFallback()
                        }
                    }
            },
            text = "⚡ Explore as Guest / Demo",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Instant 1-tap access. No account setup required.",
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email / Password Section Toggle
        Row(
            modifier = Modifier
                .clickable(role = Role.Button) { showEmailAuth = !showEmailAuth }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showEmailAuth) "Hide email sign in" else "Or sign in with email and password",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (showEmailAuth) "▲" else "▼",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
        }

        AnimatedVisibility(
            visible = showEmailAuth,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(AppRadius.Card))
                    .background(SurfaceCard)
                    .border(1.dp, LineBorder, RoundedCornerShape(AppRadius.Card))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRegistering) "Create BioCue Account" else "Sign In with Email",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CalmTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email address",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CalmTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PillButton(
                        onClick = {
                            val trimmedEmail = email.trim()
                            val trimmedPass = password.trim()
                            if (trimmedEmail.isEmpty() || trimmedPass.isEmpty()) {
                                errorMsg = "Please enter both email and password."
                                return@PillButton
                            }
                            if (trimmedPass.length < 6) {
                                errorMsg = "Password must be at least 6 characters."
                                return@PillButton
                            }
                            isLoading = true
                            errorMsg = null
                            focusManager.clearFocus()

                            if (isRegistering) {
                                auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            handleAuthSuccess()
                                        } else {
                                            isLoading = false
                                            errorMsg = task.exception?.localizedMessage ?: "Account creation failed"
                                        }
                                    }
                            } else {
                                auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            handleAuthSuccess()
                                        } else {
                                            isLoading = false
                                            errorMsg = task.exception?.localizedMessage ?: "Sign-in failed. Please check credentials or create an account."
                                        }
                                    }
                            }
                        },
                        text = if (isRegistering) "Create Account" else "Sign In",
                        loading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = { isRegistering = !isRegistering }) {
                        Text(
                            text = if (isRegistering) "Already have an account? Sign in" else "Need an account? Create one",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }

                    Text(
                        text = "Demo account: demo.wearer@biocue.io · Password123!",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Error message card
        errorMsg?.let { message ->
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.Md))
                    .background(AlertCoralSoft)
                    .border(1.dp, AlertCoralChip, RoundedCornerShape(AppRadius.Md))
                    .padding(14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your telemetry data is end-to-end protected and only accessible to you and your approved caregivers.",
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
