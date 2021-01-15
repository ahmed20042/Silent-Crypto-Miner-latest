package com.amov.geoshape

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.text.format.Formatter
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.amov.geoshape.model.Client
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.activity_wait_clients.*
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

const val SERVER_MODE = 0
const val CLIENT_MODE = 1

class GameActivity : AppCompatActivity() {

    private lateinit var model: GameViewModel
    private var dialog: AlertDialog? = null
    private var actualMode: Int? = null

    private var clientsConnected: ArrayList<Client> = arrayListOf()
    private var clientsConnectedNames: ArrayList<String> = arrayListOf()
    private lateinit var clientsConnectedAdapter: ArrayAdapter<String>

    lateinit var fusedLocationClient : FusedLocationProviderClient
    var myLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        clientsConnectedAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, clientsConnectedNames)

        model = ViewModelProvider(this).get(GameViewModel::class.java)
        model.state.observe(this) {
            //updateUI()
        }

        model.connectionState.observe(this) {
            if (it != GameViewModel.ConnectionState.SETTING_PARAMETERS &&
                    it != GameViewModel.ConnectionState.SERVER_CONNECTING && dialog?.isShowing == true) {
                dialog?.dismiss()
                dialog = null
            }

            if (it == GameViewModel.ConnectionState.CONNECTION_ERROR ||
                    it == GameViewModel.ConnectionState.CONNECTION_ENDED) {
                finish()
            }

            if (it == GameViewModel.ConnectionState.NEW_CLIENT) {
                addClientToListView(Client())
            }
        }

        if (model.connectionState.value != GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
            when (intent.getIntExtra("mode", SERVER_MODE)) {
                SERVER_MODE -> startAsServer()
                CLIENT_MODE -> startAsClient()
            }
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
        } else {
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        myLocation = location

                        if (location != null) {
                            latitudeTv.text = location.latitude.toString()
                            longitudeTv.text = location.longitude.toString()
                        }
                    }
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1000
        )
        this.recreate()
    }

    override fun onBackPressed() {
        if (actualMode == SERVER_MODE) {
            val dialog = AlertDialog.Builder(this).run {
                setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Yes") { _, _ ->
                            model.stopServer()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                create()
            }
            dialog.show()
        }
    }

    private fun startAsServer() {
        actualMode = SERVER_MODE

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

        setContentView(R.layout.activity_wait_clients)
        clientsListView.adapter = clientsConnectedAdapter

        serverIpTv.text = String.format(getString(R.string.msg_ip_address), ipAddress)

        model.startServer()

        // Server mode is also a player (player 1),
        // so add it to clients list
        addClientToListView(Client())

        createTeamBtn.setOnClickListener {
            if (clientsConnected.size < 3) {
                Toast.makeText(this, "You need at least 3 players", Toast.LENGTH_LONG).show()
            } else {
                // TODO: Start Game
            }
        }
    }

    private fun startAsClient() {
        actualMode = CLIENT_MODE

        val ipEditText = EditText(this).apply {
            maxLines = 1
            width = 10
            filters = arrayOf(object : InputFilter {
                override fun filter(
                        source: CharSequence?,
                        start: Int,
                        end: Int,
                        dest: Spanned?,
                        dstart: Int,
                        dend: Int
                ): CharSequence? {
                    if (source?.none { it.isDigit() || it == '.' } == true)
                        return ""
                    return null
                }
            })
        }

        val dialog = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.client_mode))
            setMessage(getString(R.string.ask_server_ip))
            setPositiveButton(getString(R.string.button_connect)) { _: DialogInterface, _: Int ->
                val strIP = ipEditText.text.toString()
                if (strIP.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIP).matches()) {
                    Toast.makeText(
                            this@GameActivity,
                            getString(R.string.error_address),
                            Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    model.startClient(ipEditText.text.toString(), myLocation)
                }
            }
            setNeutralButton(getString(R.string.btn_emulator)) { _: DialogInterface, _: Int ->
                model.startClient("10.0.2.2", myLocation, SERVER_PORT-1)
                // Add port redirect on the Server Emulator:
                // telnet localhost <5554|5556|5558|...>
                // auth <key>
                // redir add tcp:9998:9999
            }
            setNegativeButton(getString(R.string.button_cancel)) { _: DialogInterface, _: Int ->
                finish()
            }
            setCancelable(false)
            setView(ipEditText)
            create()
        }

        dialog.show()
    }

    private fun addClientToListView(client: Client) {
        clientsConnected.add(client)
        clientsConnectedNames.add("Player " + client.id + " connected")
        clientsConnectedAdapter.notifyDataSetChanged()
    }
}