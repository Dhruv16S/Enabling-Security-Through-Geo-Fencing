package com.ar.application

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class PrimaryContacts : Fragment() {

    lateinit var recyclerView : RecyclerView
    var UIDs = ArrayList<String>()
    var Emails = ArrayList<String>()
    var Contacts = ArrayList<String>()
    val db = Firebase.firestore

    lateinit var adapter: UsersAdapter
    lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v : View = inflater.inflate(R.layout.fragment_primary_contacts, container, false)

        recyclerView = v.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        preferences = this.requireActivity()!!.getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)!!
        val fragmentUID = preferences.getString("User UID", " ")

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    //if (document.data["User UID"] == fragmentUID) {

                    UIDs.add(document.data["User UID"].toString().subSequence(0, 13).toString()
                        .plus("..."))
                    Emails.add(document.data["Email ID"].toString())
                    Contacts.add(document.data["Contact"].toString())
                    adapter = UsersAdapter(UIDs, Emails, Contacts, requireContext())
                        recyclerView.adapter = adapter
                    //}
                }
            }
        return v
    }
}