package com.example.godsmino.data.datasource

import com.example.godsmino.data.model.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuente de datos para la sincronización en tiempo real con Firebase Firestore
 * y salas multijugador online.
 */
class FirebaseGameDataSource {
    companion object {
        // Almacenamiento en memoria compartido para simular múltiples jugadores en el mismo dispositivo o red
        private val activeRooms = mutableMapOf<String, MutableStateFlow<GameSession?>>()
    }

    fun syncGameSession(roomId: String, session: GameSession) {
        val flow = activeRooms.getOrPut(roomId) { MutableStateFlow(null) }
        flow.value = session
        // Aquí se conectaría con FirebaseFirestore.getInstance().collection("games").document(roomId).set(session)
    }

    fun observeGameSession(roomId: String): Flow<GameSession?> {
        val flow = activeRooms.getOrPut(roomId) { MutableStateFlow(null) }
        // Aquí se conectaría con FirebaseFirestore.getInstance().collection("games").document(roomId).addSnapshotListener { ... }
        return flow.asStateFlow()
    }
}
