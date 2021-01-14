package com.amov.geoshape

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

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
    }

    private fun startGame(mode : Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("mode", mode)
        }
        startActivity(intent)
    }

}