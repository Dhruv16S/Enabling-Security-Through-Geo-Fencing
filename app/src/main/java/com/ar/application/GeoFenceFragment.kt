package com.ar.application

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class GeoFenceFragment : Fragment() {

    private val LOCATION_REQUEST_CODE = 123
    private val GEOFENCE_REQUEST_CODE = 456 // only for android version >= 10
    private val CAMERA_ZOOM_LEVEL = 18f
    private val GEOFENCE_RADIUS = 10f // change later
    private val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
    private val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000 // 10 days, change later
    private val GEOFENCE_DWELL_DELAY = 1 * 1000 // 10 secs, change later
    private val TAG: String = GeoFenceFragment::class.java.simpleName
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback{ googleMap ->
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (!areLocationPermsGranted()) {
            askForLocationPerms()
        }
        else {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
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
                        moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                    }
                }
                else {
                    with (map) {
                        addMarker(MarkerOptions().position(LatLng(17.429, 78.455)))
                        moveCamera(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
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
        //readRealTime()
        dynamicGeoFence(map)
        setLongClick(map)
        setPoiClick(map)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        val v : View = inflater.inflate(R.layout.fragment_geo_fence, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapAgain) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
    private fun areLocationPermsGranted(): Boolean { // checks if location permissions have been granted
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
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
            requireActivity(),
            permissions.toTypedArray(),
            LOCATION_REQUEST_CODE
        )
    }

    private fun askForBackgroundLocationPerm() { // requests for only background location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
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

    @SuppressLint("MissingPermission")
    private fun readRealTime() {
        val path = FirebaseDatabase.getInstance().reference.child("fences")
        //for (i in 1..Constants.PIN_COUNTER){
            path.child("Point 1")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(@NonNull dataSnapshot: DataSnapshot) {
                        var chosenLatLng = LatLng(dataSnapshot.child("lat").value.toString().toDouble(),dataSnapshot.child("lng").value.toString().toDouble() )

                        map.addMarker(
                            MarkerOptions().position(chosenLatLng)
                                .title("GeoFence 1")
                        )?.showInfoWindow()
                        map.addCircle(
                            CircleOptions()
                                .center(chosenLatLng)
                                .strokeColor(Color.argb(50, 70, 70, 70))
                                .fillColor(Color.argb(70, 150, 150, 150))
                                .radius(GEOFENCE_RADIUS.toDouble())
                        )
                        createGeofence(chosenLatLng, "Dynamic", geofencingClient)

                    }

                    override fun onCancelled(@NonNull databaseError: DatabaseError) {}
                })
      //}

    }


    @SuppressLint("MissingPermission")
    private fun dynamicGeoFence(map: GoogleMap) {

        val database = Firebase.database
        val reference = database.getReference("fences")
        val key = reference.push().key
        reference.child("PIN_COUNTER").get().addOnSuccessListener {
            Constants.PIN_COUNTER = it.value.toString().toLong()
        }

        for (i in 1..Constants.PIN_COUNTER){
            reference.child("Point $i").get().addOnSuccessListener {
                if(it.exists()){

                    var pinLatitude : Double = it.child("lat").value.toString().toDouble()
                    var pinLongitude : Double = it.child("lng").value.toString().toDouble()
                    var chosenLatLng = LatLng(pinLatitude, pinLongitude)
                    map.addMarker(
                        MarkerOptions().position(chosenLatLng)
                            .title("GeoFence $i")
                    )?.showInfoWindow()
                    map.addCircle(
                        CircleOptions()
                            .center(chosenLatLng)
                            .strokeColor(Color.argb(50, 70, 70, 70))
                            .fillColor(Color.argb(70, 150, 150, 150))
                            .radius(GEOFENCE_RADIUS.toDouble())
                    )
                    createGeofence(chosenLatLng, "Dynamic", geofencingClient)

                }else{
                    Toast.makeText(requireContext(), "Creation Complete", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "No Routes Traced", Toast.LENGTH_SHORT).show()
            }
        }
            //map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_LEVEL))



            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
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

    }

    @SuppressLint("MissingPermission")
    private fun setLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latlng ->
            map.addMarker(
                MarkerOptions().position(latlng)
                    .title("Geofence centre")
            )?.showInfoWindow()
            map.addCircle(
                CircleOptions()
                    .center(latlng)
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(70, 150, 150, 150))
                    .radius(GEOFENCE_RADIUS.toDouble())
            )
            //map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_LEVEL))

            val database = Firebase.database
            val reference = database.getReference("fences")
            val key = reference.push().key
            if (key != null) {
                val reminder = Fence(key, latlng.latitude, latlng.longitude)
                reference.child(key).setValue(reminder)
            }

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
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
            .apply {
                setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER
                        or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                addGeofence(geofence)
            }
            .build()

        val intent = Intent(requireActivity(), GeofenceReceiver::class.java)
            .putExtra("key", key)
            .putExtra("message", "Geofence detected!")

        val pendingIntent = PendingIntent.getBroadcast(
            requireActivity().applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity().applicationContext,
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

    companion object {
        fun showNotification(context: Context, message: String) {

            Log.d("notif", "I am here")
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