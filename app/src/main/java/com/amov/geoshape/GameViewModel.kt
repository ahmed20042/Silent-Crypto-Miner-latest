package com.amov.geoshape

import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amov.geoshape.model.Client
import com.amov.geoshape.model.Message
import com.amov.geoshape.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

const val SERVER_PORT = 9999

class GameViewModel : ViewModel() {

    enum class State {
        STARTING, TEAM_CREATED, TEAM_CREATION_FAILED, GAME_OVER
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, NEW_CLIENT, CLIENT_CONNECTING, CONNECTION_ESTABLISHED, CONNECTION_ERROR, CONNECTION_ENDED
    }

    val state = MutableLiveData(State.STARTING)
    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    fun startGame(players: ArrayList<Client>, teamName: String) {
        state.postValue(State.TEAM_CREATED)

        var polygon = ""

        if (players.size == 3)
            polygon = "Triangle"
        if (players.size == 4)
            polygon = "Square"
        if (players.size == 5)
            polygon = "Pentagon"
        if (players.size == 6)
            polygon = "Hexagon"

        val simpleDateFormat = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")
        val currentDateAndTime: String = simpleDateFormat.format(Date())

        val hostLocation = "28°05'56\"S, 48°40'30\"O"
        val uniqueId = "[$hostLocation]; [${players.size}]; [${currentDateAndTime}];"

        val newTeam = Team(uniqueId, players.size, polygon, currentDateAndTime, hostLocation)
        newTeam.name = teamName

        createTeamOnFirebase(newTeam, players)
    }

    private fun createTeamOnFirebase(team: Team, players: ArrayList<Client>) {
        val db = Firebase.firestore
        val playersInfo = hashMapOf<String, String>()

        for (item in players) {
            playersInfo[item.id.toString()] = "${item.lat}, ${item.long}"
        }

        db.collection("Teams").document(team.uniqueId).set(playersInfo)
                .addOnSuccessListener { state.postValue(State.TEAM_CREATED) }
                .addOnFailureListener { state.postValue(State.TEAM_CREATION_FAILED) }
    }

    fun startServer() {
        if (serverSocket != null
                || socket != null
                || connectionState.value != ConnectionState.SETTING_PARAMETERS) {
            return
        }

        connectionState.postValue(ConnectionState.SERVER_CONNECTING)

        serverSocket = ServerSocket(SERVER_PORT)

        thread {
            try {
                while (true) {
                    val client: Socket = serverSocket!!.accept()
                    connectionState.postValue(ConnectionState.NEW_CLIENT)
                    ClientHandler(client).run()
                }
            } catch (_: Exception) {
                connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            } finally {
                serverSocket?.close()
                serverSocket = null
            }
        }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        serverSocket = null
    }

    fun startClient(serverIP: String, serverPort: Int = SERVER_PORT) {
        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS) {
            return
        }

        thread {
            connectionState.postValue(ConnectionState.CLIENT_CONNECTING)
            try {
                ClientConnection(serverIP, serverPort).run()
            } catch (_: Exception) {
                connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            }
        }
    }

    private fun stopGame() {
        try {
            state.postValue(State.GAME_OVER)
            connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            socket?.close()
            socket = null
        } catch (_: Exception) { }
    }

    // Class that handles a single client connection
    class ClientHandler(client: Socket) {

        private var socket: Socket = client

        private val objOutput = ObjectOutputStream(socket.getOutputStream())
        private val objInput = ObjectInputStream(socket.getInputStream())

        fun run() {
            thread {
                while (true) {
                    // Receives the message from the client
                    val client: Client = objInput.readObject() as Client
                    println("[SERVER] RECEIVED PING FROM CLIENT")

                    // Sends the response to client
                    objOutput.writeObject(client)
                    println("[CLIENT] SENDING BACK PING TO CLIENT")
                }
            }
        }
    }

    class ClientConnection(address: String, port: Int) {

        private val connection = Socket(address, port)

        private val objOutput = ObjectOutputStream(connection.getOutputStream())
        private val objInput = ObjectInputStream(connection.getInputStream())

        fun run() {
            thread {
                while (true) {
                    // Sends the message to server
                    val client = Client()
                    objOutput.writeObject(client)
                    println("[CLIENT] SENDING PING TO SERVER")

                    // Receives the response from the server and prints it
                    val response: Client = objInput.readObject() as Client
                    println("[CLIENT] RESPONSE FROM SERVER: $response")

                    Thread.sleep(2500)
                }
            }
        }
    }

}