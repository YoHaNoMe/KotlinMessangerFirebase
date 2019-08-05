package com.example.messangerfirebase.messages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messangerfirebase.GlideApp
import com.example.messangerfirebase.HomeActivity
import com.example.messangerfirebase.R
import com.example.messangerfirebase.models.Message
import com.example.messangerfirebase.models.User
import com.example.messangerfirebase.registerLogin.RegisterActivity
import com.example.messangerfirebase.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_user_message.*
import kotlinx.android.synthetic.main.new_message_from_user_cell.view.*
import kotlinx.android.synthetic.main.new_message_to_user_cell.view.*
import java.lang.RuntimeException

class UserMessageActivity : AppCompatActivity(), Utils {

    private lateinit var db: FirebaseFirestore
    private lateinit var chatRef: CollectionReference
    private lateinit var documentId: String
    private lateinit var adapter: MainRecyclerView

    companion object {
        var messages = ArrayList<Message>()
        var user: User? = null
        var originalUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_message)

        val extras = intent.extras

        if (extras != null) {
            if (extras.containsKey(NewMessageActivity.USER_OBJECT)) {
                user = extras.getParcelable(NewMessageActivity.USER_OBJECT)
                supportActionBar?.title = user!!.name
            }
        }

        if (extras != null) {
            if (extras.containsKey(RegisterActivity.ORIGINAL_USER)) {
                originalUser = extras.getParcelable(RegisterActivity.ORIGINAL_USER)
            }
        }

        val senderId = FirebaseAuth.getInstance().uid.toString()
        val receiverId = user!!.uid
        documentId = compareTwoIds(senderId, receiverId)
        db = FirebaseFirestore.getInstance()
        chatRef = db.collection("chats").document(documentId).collection("messages")

        if (extras != null) {
            messages = arrayListOf()
            if (extras.containsKey(HomeActivity.MESSAGES_ARRAY)) {
                messages = intent.getParcelableArrayListExtra(HomeActivity.MESSAGES_ARRAY)
                if (!messages.isNullOrEmpty()) {
                    adapter = MainRecyclerView(messages)
                    user_message_recyclerView.adapter = adapter
                    user_message_recyclerView.scrollToPosition(messages.size - 1)
                }
            } else {
                getMessagesBetweenUsers()
            }
        }
        user_message_send_button.setOnClickListener {
            addMessage(senderId, receiverId)
        }
    }

    private fun addMessage(senderId: String, receiverId: String) {
        val message = user_message_messageText_editText.text.toString()
        if (message.isNotEmpty()) {
            val mObj = Message(
                senderId = senderId,
                receiverId = receiverId,
                message = message
            )
            messages.add(mObj)
            val hashMap = hashMapOf(
                "id" to documentId
            )
            db.collection("chats").document(documentId).set(hashMap).addOnSuccessListener {
                chatRef.add(
                    mObj
                ).addOnSuccessListener {
                    if (messages.size == 1) {
                        user_message_noMessages_textView.visibility = View.INVISIBLE
                        adapter = MainRecyclerView(messages)
                        user_message_recyclerView.adapter = adapter
                    } else {
                        user_message_noMessages_textView.visibility = View.INVISIBLE
                        user_message_recyclerView.scrollToPosition(messages.size - 1)
                        user_message_recyclerView.adapter!!.notifyItemInserted(messages.size - 1)
                    }
                    user_message_messageText_editText.setText("")
                }
            }
        } else {
            Log.d("SeeMessage", "There is no message")
        }
    }

    private fun getMessagesBetweenUsers() {
        chatRef.get().addOnSuccessListener { result ->
            if (result != null && !result.isEmpty) {
                Log.d("MessagesBetweenUsers", "There is a result")
                user_message_noMessages_textView.visibility = View.INVISIBLE
                for (document in result) {
                    val message = Message(
                        message = document.getString("message")!!,
                        senderId = document.getString("senderId")!!,
                        receiverId = document.getString("receiverId")!!
                    )
                    message.date = document.getDate("date")
                    messages.add(message)
                }
                messages.sortBy {
                    it.date
                }
                adapter = MainRecyclerView(messages)
                user_message_recyclerView.adapter = adapter
                user_message_recyclerView.scrollToPosition(messages.size - 1)
            } else {
                user_message_noMessages_textView.visibility = View.VISIBLE
                Log.d("MessagesBetweenUsers", "No such Result")
            }
        }.addOnFailureListener { exception ->
            Log.d("MessagesBetweenUsers", "get failed with ", exception)
        }
    }
}

private class MainRecyclerView(val messages: MutableList<Message>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val MESSAGE_FROM = 1
    private val MESSAGE_TO = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MESSAGE_TO -> {
                val layoutInflater = LayoutInflater.from(parent.context)
                val cellForRow = layoutInflater.inflate(R.layout.new_message_to_user_cell, parent, false)
                MessageToViewHolder(cellForRow)
            }
            MESSAGE_FROM -> {
                val layoutInflater = LayoutInflater.from(parent.context)
                val cellForRow = layoutInflater.inflate(R.layout.new_message_from_user_cell, parent, false)
                MessageFromViewHolder(cellForRow)
            }
            else -> throw RuntimeException("There is no type for what you called")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            MESSAGE_TO -> {
                initLayoutMessageTo(holder as MessageToViewHolder, position)
            }
            MESSAGE_FROM -> {
                initLayoutMessageFrom(holder as MessageFromViewHolder, position)
            }
            else -> {

            }
        }
    }

    override fun getItemCount(): Int {
        if (messages != null)
            return messages!!.size
        return 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages != null) {
            val message = messages!![position]
            if (message.senderId == FirebaseAuth.getInstance().uid) {
                MESSAGE_FROM
            } else {
                MESSAGE_TO
            }
        } else {
            -1
        }
    }

    private fun initLayoutMessageFrom(holder: MessageFromViewHolder, position: Int) {
        holder.view.user_cell_message_from_textView.text = messages!![position].message
        var originalUserProfileImageUri = ""
        val originalUser = UserMessageActivity.originalUser
        if (originalUser != null)
            originalUserProfileImageUri = originalUser.profileImageUri
        GlideApp.with(holder.view.context)
            .load(originalUserProfileImageUri)
            .into(holder.view.user_cell_message_from_profileImage_imageView)
    }

    private fun initLayoutMessageTo(holder: MessageToViewHolder, position: Int) {
        holder.view.user_cell_message_to_textView.text = messages!![position].message
        var otherUserProfileImageUri = ""
        val otherUser = UserMessageActivity.user
        if (otherUser != null)
            otherUserProfileImageUri = otherUser.profileImageUri
        GlideApp.with(holder.view.context)
            .load(otherUserProfileImageUri)
            .into(holder.view.user_cell_message_to_profileImage_imageView)
    }
}

private class MessageFromViewHolder(val view: View) : RecyclerView.ViewHolder(view)

private class MessageToViewHolder(val view: View) : RecyclerView.ViewHolder(view)

