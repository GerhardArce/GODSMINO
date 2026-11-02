package com.example.godsmino.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.godsmino.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("godsmino_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(loadUserFromPrefs())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private fun loadUserFromPrefs(): User? {
        val uid = prefs.getString("user_uid", null) ?: return null
        val email = prefs.getString("user_email", "") ?: ""
        val name = prefs.getString("user_name", "") ?: ""
        return User(uid, email, name)
    }

    fun login(email: String, pass: String): Pair<Boolean, String?> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Pair(false, "Por favor completa todos los campos.")
        }
        if (!cleanEmail.contains("@") || cleanPass.length < 6) {
            return Pair(false, "Correo invalido o contraseña menor a 6 caracteres.")
        }
        val name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val user = User(uid = "usr_" + cleanEmail.hashCode(), email = cleanEmail, displayName = name)
        saveUser(user)
        return Pair(true, null)
    }

    fun register(email: String, pass: String, name: String): Pair<Boolean, String?> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        val cleanName = name.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank() || cleanName.isBlank()) {
            return Pair(false, "Por favor completa todos los campos.")
        }
        if (!cleanEmail.contains("@")) {
            return Pair(false, "Ingresa un correo electronico valido.")
        }
        if (cleanPass.length < 6) {
            return Pair(false, "La contraseña debe tener al menos 6 caracteres.")
        }
        val user = User(uid = "usr_" + cleanEmail.hashCode(), email = cleanEmail, displayName = cleanName)
        saveUser(user)
        return Pair(true, null)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun saveUser(user: User) {
        prefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .apply()
        _currentUser.value = user
    }
}
