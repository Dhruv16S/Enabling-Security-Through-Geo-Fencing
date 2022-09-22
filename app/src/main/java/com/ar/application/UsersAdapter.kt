package com.ar.application

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserViewHolder>{
    var UIDs = ArrayList<String>()
    var Emails = ArrayList<String>()
    var Contacts = ArrayList<String>()

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
        //define the card design we made aka which design is displayed
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.card_design, parent, false)
        return UserViewHolder(view)

    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int){
        //what should be done when design is connected to recycler view
        holder.card_UID.text = UIDs.get(position)
        holder.card_email.text = Emails.get(position)
        holder.card_contact.text = Contacts.get(position)


        //card view is defined here, so toast is also specified here
        holder.cardView.setOnClickListener{
            Toast.makeText(context, "You selected ${UIDs.get(position)}", Toast.LENGTH_SHORT).show()
        }

//        holder.button.setOnClickListener {
//            Toast.makeText(context, "You clicked the button of ${UIDs.get(position)}", Toast.LENGTH_SHORT).show()
//            removeItem(position, holder)
//            //countryNameList.remove(countryNameList.get(position))
//            //holder.countryName.isVisible = false
//            //holder.textViewDetail.isVisible = false
//            //holder.button.isVisible = false
//            // works when deleted from bottom to top
//
//        }
    }

//    private fun removeItem(position: Int, holder: UserViewHolder) {
//        val newPosition: Int = holder.getAdapterPosition()
//        UIDs.removeAt(newPosition)
//        notifyItemRemoved(newPosition)
//        notifyItemRangeChanged(newPosition, UIDs.size)
//    }

    override fun getItemCount(): Int {
        //amount of data to be displayed
        return UIDs.size
    }

}