package com.example.godsmino.data.repository

import android.content.Context
import android.content.SharedPreferences
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
    private val prefs: SharedPreferences = context.getSharedPreferences("godsmino_user_session", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    private val firestore: FirebaseFirestore? by lazy { try { FirebaseFirestore.getInstance() } catch (_: Exception) { null } }

    private val _currentUser = MutableStateFlow<User?>(getSavedUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    val rawName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Jugador"
                    val displayName = if (rawName == "Jugador" && !firebaseUser.email.isNullOrBlank()) {
                        firebaseUser.email!!.substringBefore("@").replaceFirstChar { it.uppercase() }
                    } else {
                        rawName
                    }
                    val user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = displayName
                    )
                    _currentUser.value = user
                    saveUserSession(user)
                    saveUserToFirestore(user)
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveUserSession(user: User) {
        try {
            prefs.edit().apply {
                putString("uid", user.uid)
                putString("email", user.email)
                putString("displayName", user.displayName)
                apply()
            }
        } catch (_: Exception) {}
    }

    private fun getSavedUser(): User? {
        val uid = prefs.getString("uid", null) ?: return null
        val email = prefs.getString("email", "") ?: ""
        val savedName = prefs.getString("displayName", "Jugador") ?: "Jugador"
        val displayName = if ((savedName == "Jugador" || savedName.isBlank()) && email.isNotBlank()) {
            email.substringBefore("@").replaceFirstChar { it.uppercase() }
        } else {
            savedName
        }
        return User(uid = uid, email = email, displayName = displayName)
    }

    private fun saveUserToFirestore(user: User) {
        try {
            val userMap = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "lastLogin" to com.google.firebase.Timestamp.now()
            )
            firestore?.collection("users")?.document(user.uid)?.set(userMap, SetOptions.merge())
        } catch (_: Exception) {}
    }

    suspend fun login(email: String, pass: String): Pair<Boolean, String?> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Pair(false, "Por favor completa todos los campos.")
        }
        val defaultName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        try {
            val firebaseAuth = auth
            if (firebaseAuth != null) {
                val result = firebaseAuth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val rawName = firebaseUser.displayName ?: defaultName
                    val displayName = if (rawName == "Jugador" || rawName.isBlank()) defaultName else rawName
                    val user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: cleanEmail,
                        displayName = displayName
                    )
                    _currentUser.value = user
                    saveUserSession(user)
                    saveUserToFirestore(user)
                    return Pair(true, null)
                }
            }
            val user = User(uid = "offline_${cleanEmail.hashCode()}", email = cleanEmail, displayName = defaultName)
            _currentUser.value = user
            saveUserSession(user)
            return Pair(true, null)
        } catch (e: Exception) {
            val user = User(uid = "local_${cleanEmail.hashCode()}", email = cleanEmail, displayName = defaultName)
            _currentUser.value = user
            saveUserSession(user)
            return Pair(true, null)
        }
    }

    suspend fun register(email: String, pass: String, name: String): Pair<Boolean, String?> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Pair(false, "Por favor completa todos los campos.")
        }
        try {
            val firebaseAuth = auth
            if (firebaseAuth != null) {
                val result = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                    val user = User(uid = firebaseUser.uid, email = cleanEmail, displayName = cleanName)
                    _currentUser.value = user
                    saveUserSession(user)
                    saveUserToFirestore(user)
                    return Pair(true, null)
                }
            }
            val user = User(uid = "offline_reg_${cleanEmail.hashCode()}", email = cleanEmail, displayName = cleanName)
            _currentUser.value = user
            saveUserSession(user)
            return Pair(true, null)
        } catch (e: Exception) {
            val user = User(uid = "local_reg_${cleanEmail.hashCode()}", email = cleanEmail, displayName = cleanName)
            _currentUser.value = user
            saveUserSession(user)
            return Pair(true, null)
        }
    }

    suspend fun loginAsGuest(): Pair<Boolean, String?> {
        val guestName = "Invitado"
        try {
            val firebaseAuth = auth
            if (firebaseAuth != null) {
                try {
                    val result = firebaseAuth.signInAnonymously().await()
                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(guestName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                        val user = User(uid = firebaseUser.uid, email = "guest@godsmino.com", displayName = guestName)
                        _currentUser.value = user
                        saveUserSession(user)
                        return Pair(true, null)
                    }
                } catch (_: Exception) {}
            }
            val guestUser = User(uid = "guest_${System.currentTimeMillis()}", email = "guest@godsmino.com", displayName = guestName)
            _currentUser.value = guestUser
            saveUserSession(guestUser)
            return Pair(true, null)
        } catch (_: Exception) {
            val guestUser = User(uid = "guest_${System.currentTimeMillis()}", email = "guest@godsmino.com", displayName = guestName)
            _currentUser.value = guestUser
            saveUserSession(guestUser)
            return Pair(true, null)
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): Pair<Boolean, String?> {
        try {
            val firebaseAuth = auth ?: return Pair(false, "Firebase Auth no inicializado.")
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Usuario Google"
                val user = User(uid = firebaseUser.uid, email = firebaseUser.email ?: "", displayName = name)
                _currentUser.value = user
                saveUserSession(user)
                saveUserToFirestore(user)
            }
            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, e.localizedMessage ?: "Error al iniciar sesión con Google.")
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        try {
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
        _currentUser.value = null
    }
}
