package com.example.godsmino.data.datasource

import android.util.Log
import com.example.godsmino.data.model.GameSession
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class FirebaseGameDataSource {
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
        return withContext(Dispatchers.IO) {
            try {
                val instance = firestore ?: run {
                    Log.w("FirebaseGameDataSource", "Firestore instance is null in getExistingSession")
                    return@withContext null
                }
                val snapshot = Tasks.await(instance.collection("games").document(roomId).get())
                if (snapshot.exists()) {
                    Log.d("FirebaseGameDataSource", "Found existing session for room: $roomId")
                    snapshot.toObject(GameSession::class.java)
                } else {
                    Log.d("FirebaseGameDataSource", "No existing session found for room: $roomId")
                    null
                }
            } catch (e: Exception) {
                Log.e("FirebaseGameDataSource", "Error in getExistingSession for room $roomId", e)
                null
            }
        }
    }

    fun observeGameSession(roomId: String): Flow<GameSession?> = callbackFlow {
        val instance = firestore
        if (instance == null) {
            Log.e("FirebaseGameDataSource", "Firestore instance is null in observeGameSession")
            trySend(null)
            close()
            return@callbackFlow
        }
        Log.d("FirebaseGameDataSource", "Starting snapshot listener for room: $roomId")
        val listener = instance.collection("games")
            .document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseGameDataSource", "Snapshot listener error for room $roomId", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val session = snapshot.toObject(GameSession::class.java)
                    Log.d("FirebaseGameDataSource", "Snapshot update received for room $roomId: status=${session?.status}, players=${session?.players?.size}")
                    trySend(session)
                } else {
                    Log.d("FirebaseGameDataSource", "Snapshot does not exist for room $roomId")
                    trySend(null)
                }
            }
        awaitClose { 
            Log.d("FirebaseGameDataSource", "Removing snapshot listener for room: $roomId")
            listener.remove() 
        }
    }
}
