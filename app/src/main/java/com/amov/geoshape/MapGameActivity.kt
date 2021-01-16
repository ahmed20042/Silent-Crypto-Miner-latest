package com.amov.geoshape

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import kotlinx.android.synthetic.main.activity_map_game.*


class MapGameActivity : AppCompatActivity(), OnMapReadyCallback {

    val ISEC = LatLng(40.1925, -8.4115)
    val DEIS = LatLng(40.1925, -8.4128)
    val Teatro = LatLng(40.1935, -8.4123)
    val LOCATION_REQUEST_CODE = 1111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_game)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMap
            .addMarker(
                MarkerOptions()
                    .position(ISEC)
                    .title("ISEC")
            )

        googleMap
            .addMarker(
                MarkerOptions()
                    .position(DEIS)
                    .title("DEIS")
            )

        googleMap
            .addMarker(
                MarkerOptions()
                    .position(Teatro)
                    .title("Teatro")
            )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), LOCATION_REQUEST_CODE
                )
        }

        googleMap.isMyLocationEnabled = true
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        val cp = CameraPosition.Builder().target(ISEC).zoom(17f)
            .bearing(0f).tilt(0f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp))

        val options = PolygonOptions()
        options.fillColor(Color.RED)

        googleMap.addPolygon(
            options
                .add(ISEC)
                .add(DEIS)
                .add(Teatro)
        )

        isecDeisDistance.text = "Distancia ISEC-DEIS: " + distance(ISEC.latitude, DEIS.latitude, ISEC.longitude, DEIS.longitude).toString() + " meters"
        deisTeatroDistance.text = "Distancia DEIS-Teatro: " + distance(DEIS.latitude, Teatro.latitude, DEIS.longitude, Teatro.longitude).toString() + " meters"
        teatroDeisDistance.text = "Distancia Teatro-ISEC: " + distance(Teatro.latitude, ISEC.latitude, Teatro.longitude, ISEC.longitude).toString() + " meters"
    }

    fun distance(
        lat1: Double,
        lat2: Double,
        lon1: Double,
        lon2: Double
    ): Double {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        var lat1 = lat1
        var lat2 = lat2
        var lon1 = lon1
        var lon2 = lon2
        lon1 = Math.toRadians(lon1)
        lon2 = Math.toRadians(lon2)
        lat1 = Math.toRadians(lat1)
        lat2 = Math.toRadians(lat2)

        // Haversine formula
        val dlon = lon2 - lon1
        val dlat = lat2 - lat1
        val a = (Math.pow(Math.sin(dlat / 2), 2.0)
                + (Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2.0)))
        val c = 2 * Math.asin(Math.sqrt(a))

        val r = 6371.0

        // calculate the result
        return c * r * 1000
    }
}