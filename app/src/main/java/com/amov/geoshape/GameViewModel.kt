package com.amov.geoshape

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amov.geoshape.model.Client
import com.amov.geoshape.model.Message
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

const val SERVER_PORT = 9999

class GameViewModel : ViewModel() {

    enum class State {
        STARTING, GAME_OVER
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, NEW_CLIENT, CLIENT_CONNECTING, CONNECTION_ESTABLISHED, CONNECTION_ERROR, CONNECTION_ENDED
    }

    val state = MutableLiveData(State.STARTING)
    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private var threadCommunication: Thread? = null

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

    fun startClient(serverIP: String, location: Location?, serverPort: Int = SERVER_PORT) {
        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS) {
            return
        }

        thread {
            connectionState.postValue(ConnectionState.CLIENT_CONNECTING)
            try {
                ClientConnection(serverIP, serverPort, location).run()
                //startCommunication(newSocket)
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
            threadCommunication?.interrupt()
            threadCommunication = null
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
                    Log.d("[SERVER]", "${client.lat} + ${client.long}")

                    // Sends the response to client
                    objOutput.writeObject(client)
                }
            }
        }

        fun distanceTwoPlayers(player1: Client, player2: Client) {

        }
    }

    class ClientConnection(address: String, port: Int, location: Location?) {

        private val connection = Socket(address, port)

        private val objOutput = ObjectOutputStream(connection.getOutputStream())
        private val objInput = ObjectInputStream(connection.getInputStream())

        private val lat = location?.latitude
        private val long = location?.longitude

        fun run() {
            thread {
                while (true) {
                    // Sends the message to server
                    val client = Client()
                    client.lat = lat.toString()
                    client.long = long.toString()
                    objOutput.writeObject(client)

                    // Receives the response from the server and prints it
                    val response: Message = objInput.readObject() as Message
                    Log.d("[CLIENT]", response.message)

                    Thread.sleep(2500)
                }
            }
        }
    }

}