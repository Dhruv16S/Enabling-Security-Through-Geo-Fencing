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
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale


class PrimaryContacts : Fragment() {

    lateinit var recyclerView : RecyclerView
    var UIDs = ArrayList<String>()
    var tempUIDs = ArrayList<String>()
    var Emails = ArrayList<String>()
    var Contacts = ArrayList<String>()
    lateinit var searchView : SearchView
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
        searchView = v.findViewById(R.id.searchView)

        val fragmentUID = preferences.getString("User UID", " ")
        tempUIDs = arrayListOf<String>()
        searchView.clearFocus()


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tempUIDs.clear()
                val searchText = newText!!.toLowerCase(Locale.getDefault())
                if (searchText.isNotEmpty()){
                    UIDs.forEach{
                        if(it.toLowerCase(Locale.getDefault()).contains(searchText)){
                            tempUIDs.add(it)
                        }
                    }
                    recyclerView.adapter!!.notifyDataSetChanged()
                }else{
                    tempUIDs.clear()
                    tempUIDs.addAll(UIDs)
                    recyclerView.adapter!!.notifyDataSetChanged()
                }
                return false
            }

        })
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    UIDs.add(document.data["User UID"].toString().subSequence(0, 13).toString()
                        .plus("..."))
                    tempUIDs.addAll(UIDs)
                    Emails.add(document.data["Email ID"].toString())
                    Contacts.add(document.data["Contact"].toString())
                    adapter = UsersAdapter(UIDs, Emails, Contacts, requireContext())
                    recyclerView.adapter = adapter
                }
            }
        return v
    }


}