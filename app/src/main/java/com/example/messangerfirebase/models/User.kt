package com.example.messangerfirebase.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(val uid: String, val name: String, val profileImageUri: String): Parcelable