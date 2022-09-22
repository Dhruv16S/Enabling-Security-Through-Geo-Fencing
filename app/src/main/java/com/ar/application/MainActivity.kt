package com.ar.application

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var inApp : Button
    private lateinit var geoFence : Button
    private lateinit var traceRoute : Button
    private lateinit var showData : Button

    private lateinit var home_name : TextView
    private lateinit var home_id : TextView
    private lateinit var home_email : TextView
    private lateinit var home_contact : TextView

    lateinit var preferences: SharedPreferences
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inApp = findViewById(R.id.in_app)
        geoFence = findViewById(R.id.geofence)
        traceRoute = findViewById(R.id.trace_route)
        home_name = findViewById(R.id.home_name)
        home_id = findViewById(R.id.home_unique_id)
        home_email = findViewById(R.id.home_email)
        home_contact = findViewById(R.id.home_contact)
        showData = findViewById(R.id.showData)

        preferences = getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val UID = preferences.getString("User UID", " ")

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if(document.data["User UID"] == UID){
                        home_name.text = document.data["Username"].toString()
                        home_id.text = document.data["User UID"].toString().subSequence(0, 13)
                        home_id.text = home_id.text.toString().plus("...")
                        home_email.text = document.data["Email ID"].toString()
                        home_contact.text = document.data["Contact"].toString()
                    }
                }
            }

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

        showData.setOnClickListener{
            val fragmentManager : FragmentManager = supportFragmentManager
            val fragmentTransaction : FragmentTransaction = fragmentManager.beginTransaction()

            val firstFragment = PrimaryContacts()
            fragmentTransaction.add(R.id.frame, firstFragment)

            fragmentTransaction.commit()
        }

    }
}