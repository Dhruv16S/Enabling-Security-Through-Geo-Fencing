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

class Geofencing : AppCompatActivity(), OnMapReadyCallback {

    private val LOCATION_REQUEST_CODE = 123
    private val GEOFENCE_REQUEST_CODE = 456 // only for android version >= 10
    private val CAMERA_ZOOM_LEVEL = 18f
    private val GEOFENCE_RADIUS = 500 // change later
    private val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
    private val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000 // 10 days, change later
    private val GEOFENCE_DWELL_DELAY = 10 * 1000 // 10 secs, change later
    private val TAG: String = Geofencing::class.java.simpleName

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
    //@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
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
//                askForLocationPerms()
            }
            this.map.isMyLocationEnabled = true

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
        setLongClick(map)
        setPoiClick(map)
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

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )?.showInfoWindow()

            //scheduleJob()
        }
    }

    private fun setLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latlng ->
            map.addMarker(
                MarkerOptions().position(latlng)
                    .title("Current location")
            )?.showInfoWindow()
            map.addCircle(
                CircleOptions()
                    .center(latlng)
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(70, 150, 150, 150))
                    .radius(GEOFENCE_RADIUS.toDouble())
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_LEVEL))

            val database = Firebase.database
            val reference = database.getReference("fences")
            val key = reference.push().key
            if (key != null) {
                val reminder = Fence(key, latlng.latitude, latlng.longitude)
                reference.child(key).setValue(reminder)
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
            }
            createGeofence(latlng, key!!, geofencingClient)
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