package com.ar.application

import android.location.Location

object Constants {
    val FAST_UPDATE_INTERVAL : Long = 2
    val NORMAL_UPDATE_INTERVAL : Long = 10
    val PERMISSION_FINE_LOCATION : Int = 321
    var MANY_BOOL : Boolean = true
    var PIN_COUNTER : Long = 0
    var LOCATION_LIST = ArrayList<Location>()
    var onHomePage : Boolean = true
}