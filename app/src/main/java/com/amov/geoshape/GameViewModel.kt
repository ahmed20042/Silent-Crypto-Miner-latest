package com.amov.geoshape

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amov.geoshape.model.Message
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

const val SERVER_PORT = 9999

class GameViewModel : ViewModel() {

    enum class State {
        STARTING,
        GAME_OVER
    }

    enum class ConnectionState {
        SETTING_PARAMETERS,
        SERVER_CONNECTING,
        NEW_CLIENT,
        CLIENT_CONNECTING,
        CONNECTION_ESTABLISHED,
        CONNECTION_ERROR,
        CONNECTION_ENDED
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

    fun startClient(serverIP: String, serverPort: Int = SERVER_PORT) {
        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS) {
            return
        }

        thread {
            connectionState.postValue(ConnectionState.CLIENT_CONNECTING)
            try {
                Client(serverIP, serverPort).run()
                //startCommunication(newSocket)
            } catch (_: Exception) {
                connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            }
        }
    }

    /*
    private fun startCommunication(newSocket: Socket) {

        if (threadCommunication != null) {
            return
        }

        println("New client connected")

        socket = newSocket
        threadCommunication = thread {
            try {
                if (inputStream == null) {
                    return@thread
                }

                connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                val bufI = inputStream!!.bufferedReader()

                /*while (state.value != State.GAME_OVER) {
                    val coordinates = bufI.readLine()
                }*/
            } catch (_: Exception) {
            } finally {
                stopGame()
            }
        }
    }
     */

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
                val msg: Message = objInput.readObject() as Message
                println("Message received from client: ${msg.message}")

                objOutput.writeObject(Message("[ECHO SERVER] ${msg.message}"))
            }
        }
    }

    class Client(address: String, port: Int) {

        private val connection = Socket(address, port)

        private val objOutput = ObjectOutputStream(connection.getOutputStream())
        private val objInput = ObjectInputStream(connection.getInputStream())

        fun run() {
            thread {
                objOutput.writeObject(Message("[MESSAGE]"))

                val response: Message = objInput.readObject() as Message
                println("RESPONSE: ${response.message}")
            }
        }


    }

}