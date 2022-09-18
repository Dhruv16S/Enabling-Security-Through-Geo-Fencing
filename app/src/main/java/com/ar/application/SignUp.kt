package com.ar.application

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUp : AppCompatActivity() {

    lateinit var userName : TextInputEditText
    lateinit var email : TextInputEditText
    lateinit var contact : TextInputEditText
    lateinit var pwd : TextInputEditText
    lateinit var cpwd : TextInputEditText
    lateinit var signUp : Button

    lateinit var userNameField : TextInputLayout
    lateinit var emailField : TextInputLayout
    lateinit var contactField : TextInputLayout
    lateinit var pwdField : TextInputLayout
    lateinit var cpwdField : TextInputLayout

    lateinit var login_acc : TextView

    private lateinit var auth: FirebaseAuth
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        userName = findViewById(R.id.userName)
        email = findViewById(R.id.email)
        contact = findViewById(R.id.phone)
        pwd = findViewById(R.id.pwd)
        cpwd = findViewById(R.id.cpwd)
        signUp = findViewById(R.id.signup_button)

        login_acc = findViewById(R.id.login_acc)

        userNameField = findViewById(R.id.userNameTextField)
        emailField = findViewById(R.id.emailTextField)
        contactField = findViewById(R.id.phoneTextField)
        pwdField = findViewById(R.id.pwdTextField)
        cpwdField = findViewById(R.id.cpwdTextField)

        auth = Firebase.auth

        login_acc.setOnClickListener {
            val goToLogin = Intent(this, Login::class.java)
            startActivity(goToLogin)
        }

        signUp.setOnClickListener { v->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)

            userNameField.boxStrokeColor = Color.parseColor("#6505ee")
            emailField.boxStrokeColor = Color.parseColor("#6505ee")
            contactField.boxStrokeColor = Color.parseColor("#6505ee")
            pwdField.boxStrokeColor = Color.parseColor("#6505ee")
            cpwdField.boxStrokeColor = Color.parseColor("#6505ee")

            if(checkCredentials()){
                auth.createUserWithEmailAndPassword(
                    email.text.toString(),
                    pwd.text.toString()
                ).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userUID = auth.currentUser?.uid
                        val user = hashMapOf(
                            "User UID" to userUID,
                            "Username" to userName.text.toString(),
                            "Email ID" to email.text.toString(),
                            "Contact" to contact.text.toString(),
                        )
                        db.collection("users")
                            .add(user)
                            .addOnSuccessListener { documentReference ->
                                Log.d("Message", "DocumentSnapshot added with ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Message", "Error adding document", e)
                            }

                        Toast.makeText(baseContext, "Authentication Successful.", Toast.LENGTH_SHORT).show()
                        val signup_Home = Intent(this, MainActivity::class.java)
                        startActivity(signup_Home)
                    }
                    else {
                        try {
                            throw task.exception!!
                        }
                        catch(error : FirebaseAuthUserCollisionException){
                            Toast.makeText(this, "This Account Already Exists", Toast.LENGTH_SHORT).show()
                        }
                        finally {
                            Toast.makeText(baseContext, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } // end addOnCompleteListener
            } // end checkCredentials
        } // end OnClickListener
    } // end onCreate

    private fun checkCredentials() : Boolean{
        if(userName.length() == 0){
            userNameField.boxStrokeColor = Color.parseColor("#b00020")
            userName.requestFocus()
            return false
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){
            emailField.boxStrokeColor = Color.parseColor("#b00020")
            Toast.makeText(this, "Enter Valid Email Id", Toast.LENGTH_SHORT).show()
            email.requestFocus()
            return false
        }

        if(contact.length() != 10){
            contactField.boxStrokeColor = Color.parseColor("#b00020")
            Toast.makeText(this, "Incorrect Data", Toast.LENGTH_SHORT).show()
            contact.requestFocus()
            return false
        }

        if(pwd.length() == 0 || pwd.length() < 6){
            pwdField.boxStrokeColor = Color.parseColor("#b00020")
            Toast.makeText(this, "Minimum Length Of 6 Characters", Toast.LENGTH_SHORT).show()
            pwd.requestFocus()
            return false
        }
        if(pwd.text.toString() != cpwd.text.toString()){
            cpwdField.boxStrokeColor = Color.parseColor("#b00020")
            Toast.makeText(this, "Re Confirm The Password", Toast.LENGTH_SHORT).show()
            cpwd.requestFocus()
            return false
        }
        return true
    }
}