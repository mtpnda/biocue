package com.mtpnda.biocue.connect

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var isRegisterMode by remember { mutableStateOf(false) }
    // Email/Password state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Profile state
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to BioCue", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Sign in to access your dashboard.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Toggle Register/Login
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { isRegisterMode = false }, colors = ButtonDefaults.textButtonColors(contentColor = if (!isRegisterMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Sign In") }
                TextButton(onClick = { isRegisterMode = true }, colors = ButtonDefaults.textButtonColors(contentColor = if (isRegisterMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Register") }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isRegisterMode) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = birthdate, onValueChange = { birthdate = it }, label = { Text("Birthdate (YYYY-MM-DD)") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Email/Password Form
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                                    // Save Gender/Birthdate to SharedPreferences temporarily
                                    context.getSharedPreferences("biocue_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit().putString("gender", gender).putString("birthdate", birthdate).apply()
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
                                    // Always fallback to "User", never raw email
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegisterMode) "Create Account" else "Sign In with Email")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("OR")
            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In using Credential Manager & Firebase
            OutlinedButton(
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

                            // Note: Replace with your actual Web Client ID from Firebase/Google Cloud Console
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }
        }
        
        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
