package com.example.messangerfirebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.messangerfirebase.messages.NewMessageActivity
import com.example.messangerfirebase.messages.UserMessageActivity
import com.example.messangerfirebase.models.Message
import com.example.messangerfirebase.models.User
import com.example.messangerfirebase.registerLogin.LoginActivity
import com.example.messangerfirebase.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.recent_message_cell.view.*
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.example.messangerfirebase.registerLogin.RegisterActivity

@GlideModule
class AppGlideModule : AppGlideModule()

class HomeActivity : AppCompatActivity(), Utils {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var messages: MutableList<Message>
    private lateinit var allMessages: ArrayList<MutableList<Message>>
    private lateinit var adapter: MainHomeRecyclerView

    companion object {

        var originalUser: User? = null
        val MESSAGES_ARRAY = "Messages_Array"
    }

    override fun onStart() {
        super.onStart()
        messages = mutableListOf()
        allMessages = ArrayList()
        adapter = MainHomeRecyclerView(allMessages)
        home_recent_messages_recyclerView.adapter = adapter
        if (auth.currentUser == null)
            closeActivityAndOpenOnther(this, LoginActivity::class.java)
        else
            getTheMostRecentMessages()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val extras = intent.extras
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (extras != null) {
            val bundle = extras.getBundle("Bundle")
            if (bundle != null && !bundle.isEmpty) {
                originalUser = bundle.getParcelable(RegisterActivity.ORIGINAL_USER)
            }
        } else {
            db.collection("users").document(auth.uid.toString()).get().addOnSuccessListener { document ->
                originalUser = User(
                    name = document.get("name").toString(),
                    uid = document.get("uid").toString(),
                    profileImageUri = document.get("profileImageUri").toString()
                )
            }
        }
    }

    private fun getTheMostRecentMessages() {
        db.collection("chats").get().addOnSuccessListener { result ->
            if (result != null && !result.isEmpty) {
                for (document in result) {
                    if (document.id.contains(auth.currentUser!!.uid)) {
                        getSubCollection(document.id)
                    }
                }
            } else {
                home_recent_messages_recyclerView.visibility = View.INVISIBLE
                home_no_recentMessages_textView.visibility = View.VISIBLE
            }
        }.addOnFailureListener { exception ->
            Log.d("RecentMessages", "Error getting recent messages: ${exception.message}")
        }
    }

    private fun getSubCollection(documentId: String) {
        db.collection("chats").document(documentId).collection("messages").get().addOnSuccessListener { result ->
            for (document in result) {
                val message = Message(
                    message = document.getString("message")!!,
                    senderId = document.getString("senderId")!!,
                    receiverId = document.getString("receiverId")!!
                )
                message.date = document.getDate("date")
                messages.add(message)
            }
            messages.sortBy { it.date }
            allMessages.add(messages)
            adapter = MainHomeRecyclerView(allMessages)
            home_recent_messages_recyclerView.adapter = adapter
            messages = mutableListOf()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.home_menu_newMessage -> {
                val intent = Intent(this, NewMessageActivity::class.java)
                intent.putExtra(RegisterActivity.ORIGINAL_USER, originalUser)
                startActivity(intent)
            }
            R.id.home_menu_signOut -> {
                auth.signOut()
                closeActivityAndOpenOnther(this, LoginActivity::class.java)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

private class MainHomeRecyclerView(val allMessages: ArrayList<MutableList<Message>>?) :
    RecyclerView.Adapter<HomeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.recent_message_cell, parent, false)
        return HomeViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        if (!allMessages.isNullOrEmpty()) {
            val messages = allMessages[position]
            val recentMessageObj = messages[messages.size - 1]
            val recentMessage = recentMessageObj.message
            holder.view.home_recent_message_textView.text = recentMessage
            holder.messages = messages as ArrayList<Message>
            getOtherUserProfileImage(holder, recentMessageObj.receiverId, recentMessageObj.senderId)
        }
    }

    override fun getItemCount(): Int {
        return if (!allMessages.isNullOrEmpty())
            allMessages.size
        else
            return 0
    }

    private fun getOtherUserProfileImage(holder: HomeViewHolder, receiverId: String, senderId: String) {
        if (senderId == FirebaseAuth.getInstance().currentUser!!.uid) {
            FirebaseFirestore.getInstance().collection("users").document(receiverId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        GlideApp.with(holder.view.context).load(document.data!!["profileImageUri"].toString())
                            .into(holder.view.home_recent_message_profileImage_imageView)
                        val user = User(
                            uid = document.get("uid").toString(),
                            name = document.get("name").toString(),
                            profileImageUri = document.get("profileImageUri").toString()
                        )
                        holder.user = user
                    }
                }
        } else {
            FirebaseFirestore.getInstance().collection("users").document(senderId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        GlideApp.with(holder.view.context).load(document.data!!["profileImageUri"].toString())
                            .into(holder.view.home_recent_message_profileImage_imageView)
                        val user = User(
                            uid = document.get("uid").toString(),
                            name = document.get("name").toString(),
                            profileImageUri = document.get("profileImageUri").toString()
                        )
                        holder.user = user
                    }
                }
        }
    }

}

private class HomeViewHolder(val view: View, var messages: ArrayList<Message>? = null, var user: User? = null) :
    RecyclerView.ViewHolder(view) {
    init {
        view.setOnClickListener {
            val intent = Intent(view.context, UserMessageActivity::class.java)
            intent.putExtra(HomeActivity.MESSAGES_ARRAY, messages)
            intent.putExtra(NewMessageActivity.USER_OBJECT, user)
            val originalUser = HomeActivity.originalUser
            if (originalUser == null)
                Log.d("HomeActivity", "It is null !!!")
            else
                Log.d("HomeActivity", originalUser.profileImageUri)
            intent.putExtra(RegisterActivity.ORIGINAL_USER, HomeActivity.originalUser)
            view.context.startActivity(intent)
        }
    }
}