package com.ar.application

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class InAppMessaging : AppCompatActivity() {
    private lateinit var sosButton : Button
    lateinit var preferences: SharedPreferences
    val db = Firebase.firestore
    var fetchedData : List<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_messaging)

        preferences = getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val UID = preferences.getString("User UID", " ")

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.SEND_SMS), 111)
        } else{
            receiveMsg()
        }


        sosButton = findViewById(R.id.sos_button)
        sosButton.setOnClickListener {
            try {
                val msgObj = SmsManager.getDefault()

                db.collection("users")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            if(document.data["User UID"] == UID){
                                fetchedData = document.data["contacts"] as List<String>
                            }
                        }
                        for (i in 0..fetchedData.size - 1)
                            msgObj.sendTextMessage(fetchedData[i], null, "Sent Via Android Studio", null, null)
                    }
            }catch(e : Exception){
                Log.d("Error is : ", e.message.toString())
                Toast.makeText(this, "Doesn't work ${e.message.toString()}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            receiveMsg()
        }
    }
    private fun receiveMsg(){
        var br = object : BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    for(sms in Telephony.Sms.Intents.getMessagesFromIntent(p1)){
                        Toast.makeText(this@InAppMessaging, sms.displayMessageBody, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        registerReceiver(br, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

    }
}