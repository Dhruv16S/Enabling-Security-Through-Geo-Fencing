package com.ar.application

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var inApp : Button
    private lateinit var geoFence : Button
    private lateinit var traceRoute : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inApp = findViewById(R.id.in_app)
        geoFence = findViewById(R.id.geofence)
        traceRoute = findViewById(R.id.trace_route)

        inApp.setOnClickListener{
            var inAppIntent = Intent(this, InAppMessaging::class.java)
            startActivity(inAppIntent)
        }

        geoFence.setOnClickListener{
            var geoFenceIntent = Intent(this, Geofencing::class.java)
            startActivity(geoFenceIntent)
        }

        traceRoute.setOnClickListener{
            var traceRouteIntent = Intent(this, TracingRoute::class.java)
            startActivity(traceRouteIntent)
        }

    }
}