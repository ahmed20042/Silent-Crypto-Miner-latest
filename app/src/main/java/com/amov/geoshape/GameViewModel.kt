package com.amov.geoshape

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amov.geoshape.model.Client
import com.amov.geoshape.model.Message
import com.amov.geoshape.model.Team
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_wait_start_game.*
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
var CLIENT_ID = 1

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
    var uniqueTeamId: String = ""

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

        val hostLocation = "${players[0].lat}, ${players[0].long}"
        uniqueTeamId = "[$hostLocation]; [${players.size}]; [${currentDateAndTime}];"

        val newTeam = Team(uniqueTeamId, players.size, polygon, currentDateAndTime, hostLocation)
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
                    CLIENT_ID++
                    ClientHandler(client, CLIENT_ID).run()
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
    inner class ClientHandler(client: Socket, clientId: Int) {

        private var socket: Socket = client

        private val objOutput = ObjectOutputStream(socket.getOutputStream())
        private val objInput = ObjectInputStream(socket.getInputStream())

        private var client: Int = clientId

        fun run() {
            thread {
                val messageId = Message(client.toString())
                objOutput.writeObject(messageId)
                println("[SERVER] ENVIEI ID DO CLIENTE ${messageId.message}")

                while (true) {
                    // Receives the message from the client
                    if (uniqueTeamId != "") {
                        val message = Message(uniqueTeamId)
                        println("[SERVER] ENVIEI ID DA EQUIPA ${message.message}")
                        objOutput.writeObject(message)
                    }
                }
            }
        }
    }

    inner class ClientConnection(address: String, port: Int) {

        private val connection = Socket(address, port)

        private val objOutput = ObjectOutputStream(connection.getOutputStream())
        private val objInput = ObjectInputStream(connection.getInputStream())

        private var myId: Int = 0

        fun run() {
            thread {
                val message = objInput.readObject() as Message
                myId = message.message.toInt()
                println("[CLIENT] I RECEIVED MY ID: ${message.message}")

                while (true) {
                    // Receives the response from the server and prints it
                    val message: Message = objInput.readObject() as Message
                    println("[CLIENT] I RECEIVED THE TEAM ID: ${message.message}")

                    val db = Firebase.firestore

                    val v = db.collection("Teams").document(message.message)
                    v.get(Source.SERVER)
                        .addOnSuccessListener {
                            //v.update(myId, "123, 123")
                        }

                    Thread.sleep(2500)
                }
            }
        }
    }

}