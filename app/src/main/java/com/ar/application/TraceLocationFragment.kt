package com.ar.application

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TraceLocationFragment : Fragment() {

    private val LOCATION_REQUEST_CODE = 12310
    private val CAMERA_ZOOM_LEVEL = 18f
    private val TAG: String = GeoFenceFragment::class.java.simpleName
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
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
            }
            this.map.isMyLocationEnabled = true

            // getting last known location data
            with (map) {
                addMarker(MarkerOptions().position(LatLng(17.4479550, 78.4430250)).title("Journey"))
            }

            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    with (map) {
                        val latLng = LatLng(it.latitude, it.longitude)
                        addMarker(MarkerOptions().position(latLng))
                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                    }
                }
            }
        }
        //tracePath(map)
    }


    @SuppressLint("MissingPermission")
    private fun tracePath(map: GoogleMap) {

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
                }else{
                    Toast.makeText(requireContext(), "Creation Complete", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "No Routes Traced", Toast.LENGTH_SHORT).show()
            }
        }
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val v : View = inflater.inflate(R.layout.fragment_trace_location, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.traceRouteMap) as SupportMapFragment?
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
}