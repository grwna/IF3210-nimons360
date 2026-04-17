package com.example.nimons360_far.backend.utils

import android.content.Context
import android.content.Intent

// Activity navigation
fun <T> Context.navigateTo(activityClass: Class<T>, clearStack: Boolean = false) {
    val intent = Intent(this, activityClass)
    if (clearStack) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
}
