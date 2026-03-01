package com.example.mercanteinfiera.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mercanteinfiera.models.GameRoom
import com.example.mercanteinfiera.models.RoomPlayer
import com.example.mercanteinfiera.models.RoomStatus
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

class MultiplayerViewModel : ViewModel() {

    // Using the URL from google-services.json. If it hangs, the URL might be incorrect or unreachable.
    private val database = Firebase.database("https://mercante-in-fiera-15aed-default-rtdb.europe-west1.firebasedatabase.app/").reference
    private val auth = Firebase.auth

    private val _currentRoom = MutableStateFlow<GameRoom?>(null)
    val currentRoom: StateFlow<GameRoom?> = _currentRoom.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var roomListener: ValueEventListener? = null

    fun createRoom(playerName: String) {
        viewModelScope.launch {
            try {
                Log.d("MultiplayerVM", "1. Start createRoom for: $playerName")
                
                val user = auth.currentUser ?: run {
                    Log.d("MultiplayerVM", "2. Signing in anonymously...")
                    auth.signInAnonymously().await().user
                } ?: throw Exception("Auth failed")
                
                Log.d("MultiplayerVM", "3. User UID: ${user.uid}")
                
                val code = generateRoomCode()
                Log.d("MultiplayerVM", "4. Generated room code: $code")
                
                val room = GameRoom(
                    code = code,
                    hostId = user.uid,
                    status = RoomStatus.LOBBY,
                    players = mapOf(user.uid to RoomPlayer(id = user.uid, name = playerName, isReady = true))
                )
                Log.d("MultiplayerVM", "5. Room object: $room")
                
                Log.d("MultiplayerVM", "6. Sending to Firebase...")
                // Adding a timeout to see if it's a connectivity issue
                withTimeout(10000) {
                    database.child("rooms").child(code).setValue(room).await()
                }
                Log.d("MultiplayerVM", "7. Firebase write success!")
                
                observeRoom(code)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("MultiplayerVM", "8. Firebase timeout! Check connection and Database URL")
                _error.value = "Connection timeout. Please check your internet."
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "9. Error creating room", e)
                _error.value = "Failed to create room: ${e.message}"
            }
        }
    }

    fun joinRoom(code: String, playerName: String) {
        viewModelScope.launch {
            try {
                Log.d("MultiplayerVM", "Joining room: $code")
                val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: throw Exception("Auth failed")
                
                val snapshot = withTimeout(10000) {
                    database.child("rooms").child(code).get().await()
                }
                
                if (snapshot.exists()) {
                    val room = snapshot.getValue(GameRoom::class.java)
                    if (room != null) {
                        val updatedPlayers = room.players.toMutableMap()
                        updatedPlayers[user.uid] = RoomPlayer(id = user.uid, name = playerName)
                        database.child("rooms").child(code).child("players").setValue(updatedPlayers).await()
                        observeRoom(code)
                    } else {
                        _error.value = "Invalid room data"
                    }
                } else {
                    _error.value = "Room not found"
                }
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error joining room", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    private fun observeRoom(code: String) {
        Log.d("MultiplayerVM", "Starting observation for room: $code")
        roomListener?.let { database.child("rooms").child(_currentRoom.value?.code ?: "").removeEventListener(it) }
        
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val room = snapshot.getValue(GameRoom::class.java)
                    Log.d("MultiplayerVM", "Data changed: $room")
                    _currentRoom.value = room
                } catch (e: Exception) {
                    Log.e("MultiplayerVM", "Error parsing room data", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MultiplayerVM", "Database error: ${error.message}")
                _error.value = error.message
            }
        }
        database.child("rooms").child(code).addValueEventListener(roomListener!!)
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    override fun onCleared() {
        super.onCleared()
        _currentRoom.value?.code?.let { code ->
            roomListener?.let { database.child("rooms").child(code).removeEventListener(it) }
        }
    }
}
