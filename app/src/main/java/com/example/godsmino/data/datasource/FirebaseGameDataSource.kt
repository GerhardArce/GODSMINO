package com.example.godsmino.data.datasource

import com.example.godsmino.data.model.GameSession
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class FirebaseGameDataSource {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun syncGameSession(roomId: String, session: GameSession) {
        try {
            firestore?.collection("games")?.document(roomId)?.set(session)
        } catch (_: Exception) {}
    }

    suspend fun getExistingSession(roomId: String): GameSession? {
        return withContext(Dispatchers.IO) {
            try {
                val instance = firestore ?: return@withContext null
                val snapshot = Tasks.await(instance.collection("games").document(roomId).get())
                if (snapshot.exists()) {
                    snapshot.toObject(GameSession::class.java)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun observeGameSession(roomId: String): Flow<GameSession?> = callbackFlow {
        val instance = firestore
        if (instance == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = instance.collection("games")
            .document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val session = snapshot.toObject(GameSession::class.java)
                    trySend(session)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }
}
