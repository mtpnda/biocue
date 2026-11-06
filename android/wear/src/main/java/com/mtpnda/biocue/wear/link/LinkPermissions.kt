package com.mtpnda.biocue.wear.link

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Runtime permissions the BLE link needs. Only requested when the wearer picks "iPhone". */
object LinkPermissions {
    /** API 31+ made Bluetooth permissions runtime; before that they are install-time. */
    fun required(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            emptyList()
        }

    fun missing(context: Context): List<String> = required().filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    fun hasAll(context: Context): Boolean = missing(context).isEmpty()
}
