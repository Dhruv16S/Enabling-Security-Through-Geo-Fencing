package com.ar.application

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {

    // log_userName refers to the user email id
    lateinit var log_userName : TextInputEditText
    lateinit var log_pwd : TextInputEditText
    lateinit var log_userNameField : TextInputLayout
    lateinit var log_pwdField : TextInputLayout
    lateinit var login : Button
    lateinit var create_account : TextView
    private lateinit var auth: FirebaseAuth
    lateinit var sharedPreferences: SharedPreferences
    var remember : Boolean = false
    val db = Firebase.firestore

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        log_userName = findViewById(R.id.log_userName)
        log_pwd = findViewById(R.id.log_pwd)

        log_userNameField = findViewById(R.id.log_userNameTextField)
        log_pwdField = findViewById(R.id.log_pwdTextField)
        create_account = findViewById(R.id.create_acc)
        login = findViewById(R.id.login_button)
        auth = Firebase.auth

        sharedPreferences = getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        remember = sharedPreferences.getBoolean("CHECKBOX", false)

        if(remember){
            GoToHomePage()
        }

        create_account.setOnClickListener {
            var goToSignUp = Intent(this@Login, SignUp::class.java)
            startActivity(goToSignUp)
            finish()
        }


        login.setOnClickListener {v->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)

            log_userNameField.boxStrokeColor = Color.parseColor("#6505ee")
            log_pwdField.boxStrokeColor = Color.parseColor("#6505ee")

            if(logNotEmpty()){
                auth.signInWithEmailAndPassword(log_userName.text.toString(), log_pwd.text.toString())
                    .addOnCompleteListener(this){task->
                        if(task.isSuccessful){
                            val userUID = auth.currentUser?.uid
                            val editor : SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putString("User UID", userUID)
                            editor.putBoolean("CHECKBOX", true)
                            editor.commit()
                            GoToHomePage()
                        }
                        else
                            Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                    } // add CompleteListener
            } // end logNotEmpty
        } // end onClickListener
    } // end onCreate

    private fun logNotEmpty() : Boolean {
        if(!Patterns.EMAIL_ADDRESS.matcher(log_userName.text.toString()).matches()){
            log_userNameField.boxStrokeColor = Color.parseColor("#b00020")
            Toast.makeText(this, "Enter Valid Email Id", Toast.LENGTH_SHORT).show()
            log_userName.requestFocus()
            return false
        }

        if(log_pwd.length() == 0){
            log_pwdField.boxStrokeColor = Color.parseColor("#b00020")
            log_pwd.requestFocus()
            return false
        }

        return true
    }

    private fun GoToHomePage() : Unit{
        val login_Home = Intent(this, MainActivity::class.java)
        startActivity(login_Home)
        finish()
    }
}