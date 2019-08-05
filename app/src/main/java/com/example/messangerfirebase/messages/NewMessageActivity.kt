package com.example.messangerfirebase.messages

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.messangerfirebase.GlideApp
import com.example.messangerfirebase.R
import com.example.messangerfirebase.models.User
import com.example.messangerfirebase.registerLogin.LoginActivity
import com.example.messangerfirebase.registerLogin.RegisterActivity
import com.example.messangerfirebase.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.new_message_user_cell.view.*

class NewMessageActivity : AppCompatActivity(), Utils {

    companion object {
        const val USER_OBJECT = "user_obj"
        var originalUser: User? = null
    }

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select User"
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(RegisterActivity.ORIGINAL_USER)) {
                originalUser = extras.getParcelable(RegisterActivity.ORIGINAL_USER)
            }
        }
        auth = FirebaseAuth.getInstance()

        new_message_recyclerView.layoutManager = LinearLayoutManager(this)
        new_message_recyclerView.visibility = View.INVISIBLE
        fetchUsers()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null)
            closeActivityAndOpenOnther(this, LoginActivity::class.java)
    }

    private fun fetchUsers() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").get().addOnSuccessListener { documents ->
            val listOfUsers = mutableListOf<DocumentSnapshot>()
            for (document in documents)
                if (document.data["uid"] != auth.uid)
                    listOfUsers.add(document)
            new_messages_progressBar.visibility = View.INVISIBLE
            new_message_recyclerView.visibility = View.VISIBLE
            new_message_recyclerView.adapter = MyAdapter(listOfUsers)
        }
    }

}

private class MyAdapter(val listOfUsers: MutableList<DocumentSnapshot>) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.new_message_user_cell, parent, false)
        return MyViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (!listOfUsers.isNullOrEmpty()) {
            val userName = listOfUsers[position].data?.get("name").toString()
            val profileImage = listOfUsers[position].data?.get("profileImageUri").toString()
            holder.view.new_messages_userName_textView.text = "$userName"
            GlideApp.with(holder.view.context).load(profileImage)
                .into(holder.view.new_messages_profileImage_circleImageView)
            holder.user =
                User(listOfUsers[position].data?.get("uid").toString(), name = userName, profileImageUri = profileImage)
        }
    }

    override fun getItemCount(): Int {
        return listOfUsers.size
    }


}

private class MyViewHolder(val view: View, var user: User? = null) : RecyclerView.ViewHolder(view), Utils {

    init {
        view.setOnClickListener {
            val intent = Intent(view.context, UserMessageActivity::class.java)
            intent.putExtra(NewMessageActivity.USER_OBJECT, user)
            intent.putExtra(RegisterActivity.ORIGINAL_USER, NewMessageActivity.originalUser)
            view.context.startActivity(intent)
            Log.d("NewMessageActivity", "Clicked")
        }
    }
}

