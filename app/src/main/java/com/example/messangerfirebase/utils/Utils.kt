package com.example.messangerfirebase.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

interface Utils {
    fun closeActivityAndOpenOnther(
        currentContext: Context,
        intendedContext: Class<out Any>,
        bundle: Bundle?=null
    ) {
        val intent = Intent(currentContext, intendedContext)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (bundle != null && !bundle.isEmpty) {
                intent.putExtra("Bundle", bundle)
        }
        currentContext.startActivity(intent)
    }

    fun compareTwoIds(p0: String, p1: String): String {
        for (i in 0 until p0.length) {
            val asciiCharP0 = p0[i].toByte().toInt()
            val asciiCharP1 = p1[i].toByte().toInt()
            return if (asciiCharP0 < asciiCharP1) {
                Log.d("SendMessage", "p0 is the first")
                "$p0$p1"
            } else if (asciiCharP0 > asciiCharP1) {
                Log.d("SendMessage", "p1 is the first")
                "$p1$p0"
            } else {
                Log.d("SendMessage", "This is a continue")
                continue
            }
        }
        Log.d("SendMessage", "Why i am here?")
        return ""
    }
}