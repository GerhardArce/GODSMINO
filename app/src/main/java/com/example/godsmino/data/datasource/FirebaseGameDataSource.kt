package com.example.godsmino.data.datasource

import com.example.godsmino.data.model.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuente de datos para la sincronización en tiempo real con Firebase Firestore.
 * Abstrae las lecturas y escrituras distribuidas entre salas de juego.
 */
class FirebaseGameDataSource {
    private val _remoteSession = MutableStateFlow<GameSession?>(null)
    val remoteSession: Flow<GameSession?> = _remoteSession.asStateFlow()

    fun syncGameSession(roomId: String, session: GameSession) {
        // Estructura lista para conexión con FirebaseFirestore.getInstance().collection("games").document(roomId).set(session)
        _remoteSession.value = session
    }

    fun observeGameSession(roomId: String): Flow<GameSession?> {
        // FirebaseFirestore.getInstance().collection("games").document(roomId).addSnapshotListener { ... }
        return remoteSession
    }
}
