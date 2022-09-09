package com.ar.application

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class InAppMessaging : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_messaging)
        Toast.makeText(this, "You are in In-App Messaging, Dhruv edit here", Toast.LENGTH_LONG).show()
    }
}