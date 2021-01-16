package com.amov.geoshape

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class MainActivity : AppCompatActivity() {

    var fusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Checks if network is available
        // If not, shows a message and ends the game
        if (!isNetworkAvailable()) {
            val dialog = AlertDialog.Builder(this).run {
                setMessage("You need network access to play the game")
                    .setPositiveButton("Ok") { _, _ ->
                        finish()
                    }
                create()
            }
            dialog.show()
        }

        findViewById<Button>(R.id.serverModeBtn).setOnClickListener {
            startGame(SERVER_MODE)
        }

        findViewById<Button>(R.id.clientModeBtn).setOnClickListener {
            startGame(CLIENT_MODE)
        }
    }

    private fun startGame(mode: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("mode", mode)
        }
        startActivity(intent)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}