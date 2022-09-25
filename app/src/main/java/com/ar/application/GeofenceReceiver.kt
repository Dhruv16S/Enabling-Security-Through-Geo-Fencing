package com.ar.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ar.application.Geofencing.Companion.showNotification
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class GeofenceReceiver: BroadcastReceiver() {
    lateinit var key: String
    lateinit var message: String
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p0 != null) {
            val geofencingEvent = p1?.let { GeofencingEvent.fromIntent(it) }
            val geofenceTransition = geofencingEvent?.geofenceTransition

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                if (p1 != null) {
                    key = p1.getStringExtra("key")!!
                    message = p1.getStringExtra("message")!!
                }

                // retrieving data from database
                val database = Firebase.database
                val reference = database.getReference("fences")
                val fenceListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fence = snapshot.getValue<Fence>()
                        if (fence != null) {
                            showNotification(
                                p0.applicationContext,
                                message
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("Fence: onCancelled: ${error.details}")
                    }
                }
                val child = reference.child(key)
                child.addValueEventListener(fenceListener)
            }
        }
    }
}
