package com.example.scheduleme

data class StoredAlarm(
    val time: Long,
    val title: String,
    val message: String,
    val requestCode: Int
)
