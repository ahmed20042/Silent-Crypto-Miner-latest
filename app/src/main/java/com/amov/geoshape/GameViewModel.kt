package com.amov.geoshape

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.InputStream
import java.io.OutputStream
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

    private val inputStream: InputStream?
        get() = socket?.getInputStream()
    private val outputStream: OutputStream?
        get() = socket?.getOutputStream()

    fun startServer() {
        if (serverSocket != null
                || socket != null
                || connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return

        connectionState.postValue(ConnectionState.SERVER_CONNECTING)

        thread {
            while (true) {
                serverSocket = ServerSocket(SERVER_PORT)
                serverSocket?.apply {
                    try {
                        startCommunication(serverSocket!!.accept())
                        connectionState.postValue(ConnectionState.NEW_CLIENT)
                    } catch (_: Exception) {
                        connectionState.postValue(ConnectionState.CONNECTION_ERROR)
                    } finally {
                        serverSocket?.close()
                        serverSocket = null
                    }
                }
            }
        }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        serverSocket = null
    }

    fun startClient(serverIP: String, serverPort: Int = SERVER_PORT) {
        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return
        thread {
            connectionState.postValue(ConnectionState.CLIENT_CONNECTING)
            try {
                val newSocket = Socket(serverIP, serverPort)
                startCommunication(newSocket)
            } catch (_: Exception) {
                connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            }
        }
    }

    private fun startCommunication(newSocket: Socket) {
        if (threadCommunication != null) {
            return
        }

        socket = newSocket
        threadCommunication = thread {
            try {
                if (inputStream == null) {
                    return@thread
                }

                connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                val bufI = inputStream!!.bufferedReader()

                while (state.value != State.GAME_OVER) {
                    val message = bufI.readLine()
                    //val move = message.toIntOrNull() ?: MOVE_NONE
                    //changeOtherMove(move)
                }
            } catch (_: Exception) {
            } finally {
                stopGame()
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

}