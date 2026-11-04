package com.example.godsmino

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.godsmino.data.repository.AuthRepository
import com.example.godsmino.ui.screen.LoginScreen
import com.example.godsmino.ui.theme.GODSMINOTheme
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {
    private val authRepository by lazy { AuthRepository(this) }

    private val googleLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                scope.launch {
                    val name = accountName.substringBefore("@").replaceFirstChar { it.uppercase() }
                    Toast.makeText(this@AuthActivity, "¡Bienvenido $accountName!", Toast.LENGTH_SHORT).show()
                    authRepository.register(accountName, "google_secure_pass_123", name)
                    navigateToMain()
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la cuenta de Google.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (try { authRepository.currentUser.value != null } catch (_: Exception) { false }) {
            navigateToMain()
            return
        }

        setContent {
            GODSMINOTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                LoginScreen(
                    authRepository = authRepository,
                    onLoginSuccess = {
                        navigateToMain(context)
                    },
                    onGoogleSignInClicked = {
                        try {
                            val intent = android.accounts.AccountManager.newChooseAccountIntent(
                                null,
                                null,
                                arrayOf("com.google"),
                                null,
                                null,
                                null,
                                null
                            )
                            googleLoginLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No hay cuentas de Google en este dispositivo.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onGuestLogin = {
                        scope.launch {
                            authRepository.loginAsGuest()
                            navigateToMain(context)
                        }
                    }
                )
            }
        }
    }

    private fun navigateToMain(context: android.content.Context = this) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
            finish()
        } catch (_: Exception) {}
    }
}
