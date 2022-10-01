package com.ar.application

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserViewHolder>{
    var UIDs = ArrayList<String>()
    var Emails = ArrayList<String>()
    var Contacts = ArrayList<String>()
    val db = Firebase.firestore
    lateinit var preferences : SharedPreferences
    var docID = ""

    lateinit var context : Context

    constructor(
        UIDs: ArrayList<String>,
        Emails: ArrayList<String>,
        Contacts: ArrayList<String>,
        context: Context
    ) {
        this.UIDs = UIDs
        this.Emails = Emails
        this.Contacts = Contacts
        this.context = context
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var card_UID : TextView = itemView.findViewById(R.id.card_UID)
        var card_email : TextView = itemView.findViewById(R.id.card_email)
        var card_contact : TextView = itemView.findViewById(R.id.card_contact)
        var cardView : CardView = itemView.findViewById(R.id.cardView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.card_design, parent, false)
        return UserViewHolder(view)

    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int){
        holder.card_UID.text = UIDs.get(position)
        holder.card_email.text = Emails.get(position)
        holder.card_contact.text = Contacts.get(position)

        preferences = context.getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val UID = preferences.getString("User UID", " ").toString()

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if(document.data["User UID"] == UID)
                        docID = document.id
                }
            }

        holder.cardView.setOnClickListener{
            db.collection("users").document(docID).update("contacts", FieldValue.arrayUnion(Contacts.get(position).toString()))
            Toast.makeText(context, "User Added As Primary Contact", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return UIDs.size
    }

}