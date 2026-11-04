package com.example.godsmino

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import com.example.godsmino.data.repository.AuthRepository
import com.example.godsmino.ui.screen.LoginScreen
import com.example.godsmino.ui.theme.GODSMINOTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider

class AuthActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authRepository = AuthRepository(this)

        if (authRepository.currentUser.value != null) {
            navigateToMain()
            return
        }

        setContent {
            GODSMINOTheme {
                val context = LocalContext.current
                LoginScreen(
                    authRepository = authRepository,
                    onLoginSuccess = {
                        navigateToMain(context)
                    },
                    onGoogleSignInClicked = {
                        val provider = OAuthProvider.newBuilder("google.com")
                        FirebaseAuth.getInstance()
                            .startActivityForSignInWithProvider(this, provider.build())
                            .addOnSuccessListener { authResult ->
                                val firebaseUser = authResult.user
                                if (firebaseUser != null) {
                                    Toast.makeText(context, "¡Bienvenido ${firebaseUser.displayName ?: firebaseUser.email}!", Toast.LENGTH_SHORT).show()
                                    navigateToMain(context)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al iniciar sesión con Google. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                            }
                    }
                )
            }
        }
    }

    private fun navigateToMain(context: android.content.Context = this) {
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
