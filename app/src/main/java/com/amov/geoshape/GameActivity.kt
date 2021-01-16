package com.amov.geoshape

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
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
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.amov.geoshape.model.Client
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_wait_clients.*
import kotlinx.android.synthetic.main.activity_wait_start_game.*
import java.util.*

const val SERVER_MODE = 0
const val CLIENT_MODE = 1
const val TAG = "MyMessage"

class GameActivity : AppCompatActivity(), LocationListener {

    private lateinit var model: GameViewModel
    private var dialog: AlertDialog? = null
    private var actualMode: Int? = null

    private var clientsConnected: ArrayList<Client> = arrayListOf()
    private var clientsConnectedNames: ArrayList<String> = arrayListOf()
    private lateinit var clientsConnectedAdapter: ArrayAdapter<String>

    lateinit var fLoc: FusedLocationProviderClient
    var locEnable = false
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_start_game)

        fLoc = FusedLocationProviderClient(this)

        clientsConnectedAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            clientsConnectedNames
        )

        model = ViewModelProvider(this).get(GameViewModel::class.java)
        model.state.observe(this) {
            if (it == GameViewModel.State.TEAM_CREATED) {
                Toast.makeText(this, "Team created with success", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MapGameActivity::class.java))
            } else if (it == GameViewModel.State.TEAM_CREATION_FAILED) {
                Toast.makeText(this, "Error creating team", Toast.LENGTH_LONG).show()
            }
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

    override fun onStart() {
        super.onStart()
        startLocation()
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
        val client = Client()
        client.lat = latitude.toString()
        client.long = longitude.toString()
        addClientToListView(client)

        createTeamBtn.setOnClickListener {
            if (clientsConnected.size < 3) {
                Toast.makeText(this, "You need at least 3 players", Toast.LENGTH_LONG).show()
            } else {
                val input = EditText(this)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                input.layoutParams = lp
                val dialog = AlertDialog.Builder(this).run {
                    setTitle("Team name")
                        .setPositiveButton("Confirm") { _, _ ->
                            model.startGame(clientsConnected, input.text.toString())
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                    setView(input)
                    create()
                }
                dialog.show()
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
                    model.startClient(ipEditText.text.toString())
                }
            }
            setNeutralButton(getString(R.string.btn_emulator)) { _: DialogInterface, _: Int ->
                //model.startClient("10.0.2.2", SERVER_PORT-1)
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

    private fun startLocation() {
        val locReq = LocationRequest().apply {
            interval = 4000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 1000
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 25)
        }

        fLoc.requestLocationUpdates(locReq, locationCallback, null)
        locEnable = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 25)
            startLocation()
    }

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            Log.i(TAG, "onLocationResult: ")
            p0?.locations?.forEach {
                Log.i(TAG, "locationCallback: ${it.latitude} ${it.longitude}")
                latitude = it.latitude
                longitude = it.longitude
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        longitude = location.longitude
        latitude = location.latitude
        Log.i(TAG, "Location: $longitude $latitude")
    }

    private fun addClientToListView(client: Client) {
        clientsConnected.add(client)
        clientsConnectedNames.add("Player " + client.id + " connected")
        clientsConnectedAdapter.notifyDataSetChanged()
    }
}