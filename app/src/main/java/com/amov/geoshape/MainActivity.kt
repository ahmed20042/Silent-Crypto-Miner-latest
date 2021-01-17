package com.amov.geoshape

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

    fun topPolygons(view: View) {
        val db = Firebase.firestore

        val polygons = arrayOf("Triangle", "Square", "Pentagon", "Hexagon")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose a polygon")

        builder.setItems(polygons) { dialog, which ->
            when (which) {
                0 -> { db.collection("Top Polygons").document("Triangle")
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Toast.makeText(this, "Error observing firebase document", Toast.LENGTH_LONG).show()
                        }

                        if (value != null && value.exists()) {
                            val area = value.get("area")
                            val team = value.get("team")

                            val dialog = AlertDialog.Builder(this).run {
                                setTitle("Triangle")
                                setMessage("Team $team --> Area: $area")
                                create()
                            }
                            dialog.show()
                        } else {
                            Toast.makeText(this, "There is no records of triangles", Toast.LENGTH_LONG).show()
                        }
                    } }
                1 -> { db.collection("Top Polygons").document("Square")
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Toast.makeText(this, "Error observing firebase document", Toast.LENGTH_LONG).show()
                        }

                        if (value != null && value.exists()) {
                            val area = value.get("area")
                            val team = value.get("team")

                            val dialog = AlertDialog.Builder(this).run {
                                setTitle("Square")
                                setMessage("Team $team --> Area: $area")
                                create()
                            }
                            dialog.show()
                        } else {
                            Toast.makeText(this, "There is no records of squares", Toast.LENGTH_LONG).show()
                        }
                    } }
                2 -> { db.collection("Top Polygons").document("Pentagon")
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Toast.makeText(this, "Error observing firebase document", Toast.LENGTH_LONG).show()
                        }

                        if (value != null && value.exists()) {
                            val area = value.get("area")
                            val team = value.get("team")

                            val dialog = AlertDialog.Builder(this).run {
                                setTitle("Pentagon")
                                setMessage("Team $team --> Area: $area")
                                create()
                            }
                            dialog.show()
                        } else {
                            Toast.makeText(this, "There is no records of pentagons", Toast.LENGTH_LONG).show()
                        }
                    } }
                3 -> { db.collection("Top Polygons").document("Hexagon")
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Toast.makeText(this, "Error observing firebase document", Toast.LENGTH_LONG).show()
                        }

                        if (value != null && value.exists()) {
                            val area = value.get("area")
                            val team = value.get("team")

                            val dialog = AlertDialog.Builder(this).run {
                                setTitle("Hexagon")
                                setMessage("Team $team --> Area: $area")
                                create()
                            }
                            dialog.show()
                        } else {
                            Toast.makeText(this, "There is no records of hexagons", Toast.LENGTH_LONG).show()
                        }
                    } }
            }
        }

        val dialog = builder.create()
        dialog.show()


    }
}