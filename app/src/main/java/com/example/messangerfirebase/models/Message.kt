package com.example.messangerfirebase.models

import android.os.Parcelable
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Message(val message: String, val senderId: String, val receiverId: String) : Parcelable {
    @ServerTimestamp
    var date: Date? = null
}