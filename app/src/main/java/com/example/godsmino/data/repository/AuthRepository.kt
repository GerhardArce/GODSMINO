package com.example.godsmino.data.repository

import android.content.Context
import com.example.godsmino.data.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(
        auth.currentUser?.let { 
            User(uid = it.uid, email = it.email ?: "", displayName = it.displayName ?: it.email?.substringBefore("@") ?: "Jugador") 
        }
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Jugador"
                )
                _currentUser.value = user
                saveUserToFirestore(user)
            } else {
                _currentUser.value = null
            }
        }
    }

    private fun saveUserToFirestore(user: User) {
        try {
            val userMap = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "lastLogin" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(user.uid).set(userMap, SetOptions.merge())
        } catch (_: Exception) {}
    }

    suspend fun login(email: String, pass: String): Pair<Boolean, String?> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Pair(false, "Por favor completa todos los campos.")
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Pair(false, "Ingresa un correo electronico valido.")
        }
        try {
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
            result.user?.let {
                val user = User(uid = it.uid, email = it.email ?: cleanEmail, displayName = it.displayName ?: cleanEmail.substringBefore("@"))
                saveUserToFirestore(user)
            }
            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, e.localizedMessage ?: "Error al iniciar sesión. Verifica tus credenciales.")
        }
    }

    suspend fun register(email: String, pass: String, name: String): Pair<Boolean, String?> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        val cleanName = name.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank() || cleanName.isBlank()) {
            return Pair(false, "Por favor completa todos los campos.")
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Pair(false, "Ingresa un correo electronico valido.")
        }
        if (cleanPass.length < 6) {
            return Pair(false, "La contraseña debe tener al menos 6 caracteres.")
        }
        try {
            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                val user = User(uid = firebaseUser.uid, email = cleanEmail, displayName = cleanName)
                _currentUser.value = user
                saveUserToFirestore(user)
            }
            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, e.localizedMessage ?: "Error al registrarse. Es posible que el correo ya esté en uso.")
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): Pair<Boolean, String?> {
        try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Usuario Google"
                val user = User(uid = firebaseUser.uid, email = firebaseUser.email ?: "", displayName = name)
                _currentUser.value = user
                saveUserToFirestore(user)
            }
            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, e.localizedMessage ?: "Error al iniciar sesión con Google.")
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}
