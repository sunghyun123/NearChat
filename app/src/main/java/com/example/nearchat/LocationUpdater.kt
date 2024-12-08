package com.example.nearchat

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat

class LocationUpdater(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun updateLocation(isOnline: Boolean, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onFailure(SecurityException("위치 권한이 없습니다."))
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = 37.302688//location.latitude
                    val longitude = 127.925298//location.longitude
                    val userId = firebaseAuth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"

                    val updates = mapOf(
                        "location" to mapOf(
                            "latitude" to latitude,
                            "longitude" to longitude
                        ),
                        "isOnline" to isOnline
                    )

                    db.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                } else {
                    onFailure(IllegalStateException("위치를 가져올 수 없습니다."))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
