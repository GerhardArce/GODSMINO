package com.example.godsmino.data.datasource

import android.util.Log
import com.example.godsmino.data.model.GameSession
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class FirebaseGameDataSource {
    companion object {
        private val inMemoryRooms = mutableMapOf<String, MutableStateFlow<GameSession?>>()

        fun getInMemoryFlow(roomId: String): MutableStateFlow<GameSession?> {
            return inMemoryRooms.getOrPut(roomId) { MutableStateFlow(null) }
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance().also {
                Log.d("FirebaseGameDataSource", "Firestore initialized successfully.")
            }
        } catch (e: Exception) {
            Log.e("FirebaseGameDataSource", "Failed to initialize Firestore: ${e.message}", e)
            null
        }
    }

    fun syncGameSession(roomId: String, session: GameSession) {
        getInMemoryFlow(roomId).value = session

        try {
            firestore?.collection("games")?.document(roomId)?.set(session)
                ?.addOnSuccessListener {
                    Log.d("FirebaseGameDataSource", "Successfully synced session for room: $roomId")
                }
                ?.addOnFailureListener { e ->
                    Log.e("FirebaseGameDataSource", "Failed to sync session for room: $roomId", e)
                }
        } catch (e: Exception) {
            Log.e("FirebaseGameDataSource", "Exception in syncGameSession: ${e.message}", e)
        }
    }

    suspend fun getExistingSession(roomId: String): GameSession? {
        val local = getInMemoryFlow(roomId).value
        if (local != null) return local

        return withContext(Dispatchers.IO) {
            try {
                val instance = firestore ?: return@withContext null
                val snapshot = Tasks.await(instance.collection("games").document(roomId).get())
                if (snapshot.exists()) {
                    snapshot.toObject(GameSession::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("FirebaseGameDataSource", "Error in getExistingSession for room $roomId", e)
                null
            }
        }
    }

    fun observeGameSession(roomId: String): Flow<GameSession?> {
        val memoryFlow = getInMemoryFlow(roomId)

        try {
            firestore?.collection("games")?.document(roomId)
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val session = try {
                            snapshot.toObject(GameSession::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseGameDataSource", "Error deserializing GameSession for room $roomId", e)
                            null
                        }
                        if (session != null) {
                            memoryFlow.value = session
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseGameDataSource", "Error attaching snapshot listener for room $roomId", e)
        }

        return memoryFlow.asStateFlow()
    }
}
