package com.mtpnda.biocue.connect

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mtpnda.biocue.ui.theme.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSlate)
    ) {
        // Ambient Background
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-200).dp)
                .size(500.dp)
                .background(Color(0xFF0EA5E9).copy(alpha = 0.15f), shape = RoundedCornerShape(250.dp))
                .blur(100.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(500.dp)
                .background(Color(0xFF34D399).copy(alpha = 0.15f), shape = RoundedCornerShape(250.dp))
                .blur(100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Logo / Header
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 40.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "BioCue",
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Find your center.\nStay connected.",
                fontSize = 16.sp,
                color = TextDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))

            if (isRegisterMode) {
                TextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("Full Name", color = TextDim) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email address", color = TextDim) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", color = TextDim) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isRegisterMode && fullName.isBlank()) {
                        errorMsg = "Please enter your full name."
                        return@Button
                    }
                    isLoading = true
                    errorMsg = null
                    
                    if (isRegisterMode) {
                        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build()
                                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                    onLoginSuccess(fullName)
                                }
                            } else {
                                errorMsg = task.exception?.localizedMessage ?: "Registration failed"
                                isLoading = false
                            }
                        }
                    } else {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    var dName = user?.displayName
                                    if (dName.isNullOrBlank()) dName = "User"
                                    onLoginSuccess(dName)
                                } else {
                                    errorMsg = task.exception?.localizedMessage ?: "Sign in failed"
                                    isLoading = false
                                }
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (isRegisterMode) "Create Account" else "Sign In", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = GlassBorder)
                Text(" OR ", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(modifier = Modifier.weight(1f), color = GlassBorder)
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    errorMsg = null
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val rawNonce = UUID.randomUUID().toString()
                            val bytes = rawNonce.toByteArray()
                            val md = MessageDigest.getInstance("SHA-256")
                            val digest = md.digest(bytes)
                            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                            val webClientId = "970885034683-qcpn8q0e7b1klpoon7js1d7cq6pko423.apps.googleusercontent.com"
                            
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(webClientId)
                                .setNonce(hashedNonce)
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
                                            val user = auth.currentUser
                                            var dName = user?.displayName
                                            if (dName.isNullOrBlank()) dName = "Google User"
                                            onLoginSuccess(dName)
                                        } else {
                                            errorMsg = task.exception?.localizedMessage ?: "Firebase auth failed"
                                            isLoading = false
                                        }
                                    }
                            } else {
                                errorMsg = "Unsupported credential type"
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "Google Sign-In error", e)
                            errorMsg = e.localizedMessage
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface)
            ) {
                Text("Continue with Google", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.clickable { isRegisterMode = !isRegisterMode }
            ) {
                Text(
                    text = if (isRegisterMode) "Already have an account? Sign In" else "Don't have an account? Create one",
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMsg!!, color = DangerColor, fontSize = 14.sp)
            }
        }
    }
}
