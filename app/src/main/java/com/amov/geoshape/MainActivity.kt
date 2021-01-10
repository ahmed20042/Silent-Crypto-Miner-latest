package com.amov.geoshape

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.amov.geoshape.model.Client

class MainActivity : AppCompatActivity(), LocationListener {
    var locEnable = false

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    lateinit var lm: LocationManager
    val TAG = "MyMessage"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: should verify if the network is available

        findViewById<Button>(R.id.serverModeBtn).setOnClickListener {
            startGame(SERVER_MODE)
        }

        findViewById<Button>(R.id.clientModeBtn).setOnClickListener {
            startGame(CLIENT_MODE)
        }
        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        startLocation(false)
    }

    fun startLocation(askPerm: Boolean){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if(askPerm)
                ActivityCompat.requestPermissions(this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION), 25)
            else
                finish()
            return
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,100f,this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 25)
            startLocation(false)

    }

    override fun onResume() {
        super.onResume()
        startLocation(true)
    }

    override fun onPause() {
        super.onPause()
        if (locEnable) {
            lm.removeUpdates(this)
            locEnable = false
        }
    }


    private fun startGame(mode : Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("mode", mode)
        }
        startActivity(intent)
    }

    override fun onLocationChanged(location: Location) {
        longitude = location.longitude
        latitude = location.latitude
        Log.i(TAG, "rpiuvpicn: $longitude $latitude")
    }

    fun rpiuvpicn(client: Client)
    {
        client.long = longitude.toString()
        client.lat = latitude.toString()
        
    }

}