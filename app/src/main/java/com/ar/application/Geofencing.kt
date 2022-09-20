package com.ar.application

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.ar.application.databinding.ActivityGeofencingBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

const val LOCATION_REQUEST_CODE = 123
const val GEOFENCE_REQUEST_CODE = 456 // only for android version >= 10
const val CAMERA_ZOOM_LEVEL = 12f
const val GEOFENCE_RADIUS = 500 // change later
const val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
const val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000 // 10 days, change later
const val GEOFENCE_DWELL_DELAY = 10 * 1000 // 10 secs, change later

class Geofencing : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityGeofencingBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGeofencingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapGeofence) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (!areLocationPermsGranted()) {
            askForLocationPerms()
        }
        else {
            if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                askForLocationPerms()
            }
            map.isMyLocationEnabled = true

            // getting last known location data
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    with (map) {
                        val latLng = LatLng(it.latitude, it.longitude)
                        addMarker(MarkerOptions().position(latLng))
                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                    }
                }
                else {
                    with (map) {
                        addMarker(MarkerOptions().position(LatLng(17.429, 78.455)))
                        moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(17.429, 78.455), CAMERA_ZOOM_LEVEL)
                                // Panjagutta coordinates
                        )
                    }
                    TODO("Manually setting the last known location for testing," +
                            "but that doesn't make sense." +
                            "So, think of something else.")
                }
            }
        }
        createFence(map)
    }

    private fun areLocationPermsGranted(): Boolean { // checks if location permissions have been granted
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun askForLocationPerms() { // requests for location permissions
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            LOCATION_REQUEST_CODE
        )
    }

    private fun askForBackgroundLocationPerm() { // requests for only background location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                GEOFENCE_REQUEST_CODE
            )
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun createFence(map: GoogleMap) { // creates the physical, viewable fence; not the actual geofence
        map.setOnMapLongClickListener {
            map.addMarker(
                MarkerOptions().position(it)
            )?.showInfoWindow()
            map.addCircle(
                CircleOptions().center(it)
                    .strokeColor(Color.argb(1, 0, 0, 0))
                    .fillColor(Color.argb(1, 183, 183, 183))
                    .radius(GEOFENCE_RADIUS.toDouble())
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(it.latitude, it.longitude),
                CAMERA_ZOOM_LEVEL)
            )

            // pushing data (lat & lng of the centre of the fence) to firebase
            val database = Firebase.database
            val reference = database.getReference("fences")
            val key = reference.push().key
            if (key != null) {
                val fence = Fence(key, it.latitude, it.longitude)
                reference.child(key).setValue(fence)
            }
            createGeofence(it, key!!, geofencingClient)
            TODO("Have to communicate with Abhiram and figure out how we're going to" +
                    "dynamically get coords from the database and create fences accordingly." +
                    "For now tho, implemented creation of fences when long pressed" +
                    "on a particular location.")
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun createGeofence(location: LatLng, key: String, geofencingClient: GeofencingClient) {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
            .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
                // defines the lifetime of the geofence
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
            )
                // transitions of the user when actions are to be triggered
            .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
                // min duration of user in geofence for action to be triggered
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(this, GeofenceReceiver::class.java)
            .putExtra("key", key)
            .putExtra("message", "Geofence detected!")

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                askForBackgroundLocationPerm()
            }
            else {
                geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            }
        }
        else {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GEOFENCE_REQUEST_CODE) {
            if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Please grant background location permission!",
                    Toast.LENGTH_SHORT
                )
                    .show()
                askForBackgroundLocationPerm()
            }
        }
        else if (requestCode == LOCATION_REQUEST_CODE) {
            if (permissions.isNotEmpty() &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                    askForLocationPerms()
                }
                map.isMyLocationEnabled = true
                onMapReady(map)
            }
            else {
                Toast.makeText(
                    this,
                    "Please grant location permissions!",
                    Toast.LENGTH_SHORT
                )
                    .show()
                askForLocationPerms()
            }
        }
    }

    companion object {
        fun showNotification(context: Context, message: String) {
            val channelID = "FENCE_NOTIFICATION_CHANNEL"
            var notifID = 420
            notifID += Random(notifID).nextInt(1, 69)

            val notifBuilder = NotificationCompat.Builder(context.applicationContext, channelID)
                .setSmallIcon(R.drawable.ic_fence_detected)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.app_name) }

                notifManager.createNotificationChannel(channel)
            }
            notifManager.notify(notifID, notifBuilder.build())
        }
    }
}