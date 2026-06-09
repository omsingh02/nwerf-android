package com.omsingh.nwerf

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncClient {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun syncTrackToCloud(track: Track) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(uid)
                .collection("library").document(track.id)
                .set(track)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchAllCloudTracks(): List<Track> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("library")
                .get()
                .await()
            snapshot.toObjects(Track::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun deleteTrackFromCloud(trackId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(uid)
                .collection("library").document(trackId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
