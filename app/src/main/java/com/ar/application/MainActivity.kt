package com.ar.application

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.ar.application.Constants.FAST_UPDATE_INTERVAL
import com.ar.application.Constants.NORMAL_UPDATE_INTERVAL
import com.ar.application.Constants.PERMISSION_FINE_LOCATION
import com.google.android.gms.location.*
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private val mHandler = Handler()

    private lateinit var inApp : Button
    private lateinit var geoFence : ConstraintLayout
    private lateinit var traceRoute : Button
    private lateinit var showData : ConstraintLayout
    private lateinit var frame : FrameLayout

    private lateinit var home_name : TextView
    private lateinit var home_id : TextView
    private lateinit var home_email : TextView
    private lateinit var home_contact : TextView
    private lateinit var initiateTrackButton : Button
    private lateinit var initiateTrackImage : ImageView
    private lateinit var trackingConstraint : ConstraintLayout
    private lateinit var mainConstraintLayout: ConstraintLayout

    lateinit var locSwitch: Switch
    lateinit var gpsSwitch: Switch

    //utils
    lateinit var locationCallback : LocationCallback
    lateinit var flpc: FusedLocationProviderClient
    lateinit var locreq: LocationRequest
    lateinit var geocoder: Geocoder

    //location
    lateinit var currentLocation : Location

    //buttons
    lateinit var newWayPointBtn : CardView
    lateinit var stopWayPointBtn : CardView

    private val CHANNEL_ID = "channel_tracking"
    private val notificationID = 101

    lateinit var preferences: SharedPreferences
    val db = Firebase.firestore

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkLocationPermission()
        createNotificationChannel()

        inApp = findViewById(R.id.in_app)
        geoFence = findViewById(R.id.geoFence)
//        traceRoute = findViewById(R.id.trace_route)
        home_name = findViewById(R.id.home_name)
        home_id = findViewById(R.id.home_unique_id)
        home_email = findViewById(R.id.home_email)
        home_contact = findViewById(R.id.home_contact)
        showData = findViewById(R.id.showData)
        frame = findViewById(R.id.frame)
        locSwitch = findViewById(R.id.location_update_switch)
        gpsSwitch = findViewById(R.id.gps_switch)
        newWayPointBtn = findViewById(R.id.cardStartTracking)
        stopWayPointBtn = findViewById(R.id.cardStopTracking)
        initiateTrackButton = findViewById(R.id.initiateTrackButton)
        initiateTrackImage = findViewById(R.id.initiateTrackImg)
        trackingConstraint = findViewById(R.id.trackingConstraint)
        mainConstraintLayout = findViewById(R.id.mainConstraintLayout)

        geocoder = Geocoder(this)
        locreq = LocationRequest.create()

        //the update intervals are in constants.kt

        locreq.interval = 1000 * NORMAL_UPDATE_INTERVAL
        locreq.fastestInterval = 1000 * FAST_UPDATE_INTERVAL
        locreq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)

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

        val fragmentManager : FragmentManager = supportFragmentManager
        var fragmentTransaction : FragmentTransaction

        fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.frame, TraceLocationFragment())
        fragmentTransaction.commit()

        inApp.setOnClickListener{
            var inAppIntent = Intent(this, InAppMessaging::class.java)
            startActivity(inAppIntent)
        }

        geoFence.setOnClickListener{
            Constants.onHomePage = false
            trackingConstraint.visibility = View.GONE
            frame.removeAllViews()
            fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.frame, GeoFenceFragment())
            fragmentTransaction.commit()
        }

        showData.setOnClickListener{
            Constants.onHomePage = false
            trackingConstraint.visibility = View.GONE
            frame.removeAllViews()
            fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.frame, PrimaryContacts())
            fragmentTransaction.commit()
        }

        gpsSwitch.setOnClickListener() {
            if (gpsSwitch.isChecked) {
                locreq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            } else {
                locreq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            }
        }

        newWayPointBtn.setOnClickListener{
            sendNotification("Tracking Enabled", "Return to the app to turn off")
            startRepeating(window.decorView.findViewById(android.R.id.content))
            Constants.LOCATION_LIST.add(currentLocation)
        }

        stopWayPointBtn.setOnClickListener {
            sendNotification("Tracking Disabled", "No longer tracking")
            stopRepeating(window.decorView.findViewById(android.R.id.content))
        }


        mainConstraintLayout.setOnClickListener {
            trackingConstraint.visibility = View.GONE
        }

        frame.setOnClickListener {
            trackingConstraint.visibility = View.GONE
        }

        // Display buttons
        // NOTE: Did not combine image and button
        initiateTrackImage.setOnClickListener {
            if(trackingConstraint.visibility == View.VISIBLE)
                trackingConstraint.visibility = View.GONE
            else
                trackingConstraint.visibility = View.VISIBLE
            if(!Constants.onHomePage){
                frame.removeAllViews()
                fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.frame, TraceLocationFragment())
                fragmentTransaction.commit()
            }
        }

        initiateTrackButton.setOnClickListener {
            if(trackingConstraint.visibility == View.VISIBLE)
                trackingConstraint.visibility = View.GONE
            else
                trackingConstraint.visibility = View.VISIBLE
            if(!Constants.onHomePage){
                frame.removeAllViews()
                fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.frame, TraceLocationFragment())
                fragmentTransaction.commit()
            }
        }

        locSwitch.setOnClickListener {
            if (locSwitch.isChecked) {
                startLocationUpdates()
            } else {
                stopLocationUpdates()
            }
        }

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return
                for(location in p0.locations){
                    updateUIValues(location)
                }
            }
        }
        updateGPS()
    }


    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "Notification Title"
            val descriptionText = "Notification Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
                .apply { descriptionText }
            val notificationManager : NotificationManager = getSystemService(Context. NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title : String, desc : String){
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_my_location_24)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationID, builder.build())
        }
    }


    @SuppressLint("MissingPermission")
    private fun updateGPS() {
        //get permissions before updating stuff
        flpc = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            flpc.lastLocation.addOnSuccessListener { location : Location? ->
                if (location != null) {
                    updateUIValues(location)
                }
            }
        } else {
            //request the permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_FINE_LOCATION
                )
            }
        }
    }

    private fun updateUIValues(location: Location) {
        //updating all text view objects with new location
        currentLocation = location
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            flpc.requestLocationUpdates(locreq,
                locationCallback,
                Looper.getMainLooper())
            updateGPS()
        }
    }

    private fun stopLocationUpdates() {
        flpc.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66
    }

    fun startRepeating(v: View?) {
        //mHandler.postDelayed(mToastRunnable, 5000);
        locationTracer.run()
    }

    fun stopRepeating(v: View?) {
        mHandler.removeCallbacks(locationTracer)
    }

    private val locationTracer: Runnable = object : Runnable {
        override fun run() {
            val database = Firebase.database
            val reference = database.getReference("fences")
            Constants.PIN_COUNTER += 1
            val key = "Point " + Constants.PIN_COUNTER
            reference.child("PIN_COUNTER").setValue(Constants.PIN_COUNTER)

            if (key != null) {
                val reminder = Fence(key, currentLocation.latitude, currentLocation.longitude)
                reference.child(key).setValue(reminder)
            }
            mHandler.postDelayed(this, 1000*10)
        }
    }


}