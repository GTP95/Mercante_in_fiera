package eu.gtpware.mercanteinfiera.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.gtpware.mercanteinfiera.models.GameRoom
import eu.gtpware.mercanteinfiera.models.RoomPlayer
import eu.gtpware.mercanteinfiera.models.RoomStatus
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
import kotlin.text.get

class MultiplayerViewModel : ViewModel() {

    private val database = Firebase.database("https://mercante-in-fiera-15aed-default-rtdb.europe-west1.firebasedatabase.app/").reference
    private val auth = Firebase.auth

    private val _currentRoom = MutableStateFlow<GameRoom?>(null)
    val currentRoom: StateFlow<GameRoom?> = _currentRoom.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var roomListener: ValueEventListener? = null

    fun clearError() {
        _error.value = null
    }

    fun createRoom(playerName: String) {
        viewModelScope.launch {
            _error.value = null // Reset error so UI shows loading
            try {
                Log.d("MultiplayerVM", "1. Start createRoom for: $playerName")
                val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: throw Exception("Auth failed")
                
                val code = generateRoomCode()
                val room = GameRoom(
                    code = code,
                    hostId = user.uid,
                    status = RoomStatus.LOBBY,
                    players = mapOf(user.uid to RoomPlayer(id = user.uid, name = playerName, isReady = false))
                )
                
                // If it hangs here, the client cannot establish a socket connection to the DB
                withTimeout(15000) { // Increased to 15s
                    database.child("rooms").child(code).setValue(room).await()
                }
                observeRoom(code)
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error creating room", e)
                _error.value = "Failed to create room: ${e.message}"
            }
        }
    }

    fun joinRoom(code: String, playerName: String) {
        viewModelScope.launch {
            _error.value = null // Reset error
            try {
                val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: throw Exception("Auth failed")
                val snapshot = withTimeout(15000) {
                    database.child("rooms").child(code).get().await()
                }
                
                if (snapshot.exists()) {
                    val room = snapshot.getValue(GameRoom::class.java)
                    if (room != null) {
                        val updatedPlayers = room.players.toMutableMap()
                        
                        var finalName = playerName
                        var counter = 2
                        val existingNames = room.players.values.map { it.name }
                        while (existingNames.contains(finalName)) {
                            finalName = "$playerName $counter"
                            counter++
                        }

                        updatedPlayers[user.uid] = RoomPlayer(id = user.uid, name = finalName)
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

    fun toggleReady() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        val currentPlayer = room.players[user.uid] ?: return
        
        val newReadyStatus = !currentPlayer.isReady
        
        viewModelScope.launch {
            try {
                // The field name in Firebase for 'isReady' is usually 'ready' if using Java beans naming convention,
                // but in Kotlin with data classes it is often 'isReady'. 
                // Given the RoomPlayer class has 'val isReady', we should use 'isReady'.
                database.child("rooms")
                    .child(room.code)
                    .child("players")
                    .child(user.uid)
                    .child("isReady")
                    .setValue(newReadyStatus)
                    .await()
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error toggling ready status", e)
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    private fun observeRoom(code: String) {
        roomListener?.let { database.child("rooms").child(_currentRoom.value?.code ?: "").removeEventListener(it) }
        
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val room = snapshot.getValue(GameRoom::class.java)
                    _currentRoom.value = room
                } catch (e: Exception) {
                    Log.e("MultiplayerVM", "Error parsing room data", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
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
