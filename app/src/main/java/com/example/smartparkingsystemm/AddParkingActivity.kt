package com.example.smartparkingsystemm

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddParkingActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_parking)

        val submitButton = findViewById<Button>(R.id.add_parking_submit_button)
        submitButton.setOnClickListener {
            addParking()
        }
    }

    private fun addParking() {
        val name = findViewById<EditText>(R.id.parking_name_edittext).text.toString()
        val address = findViewById<EditText>(R.id.parking_address_edittext).text.toString()
        val latitude = findViewById<EditText>(R.id.parking_latitude_edittext).text.toString().toDouble()
        val longitude = findViewById<EditText>(R.id.parking_longitude_edittext).text.toString().toDouble()
        val totalSpaces = findViewById<EditText>(R.id.parking_total_spaces_edittext).text.toString().toInt()
        val availableSpaces = totalSpaces
        val reservedSpaces = 0

        val parking = hashMapOf(
            "name" to name,
            "address" to address,
            "location" to com.google.firebase.firestore.GeoPoint(latitude, longitude),
            "totalSpaces" to totalSpaces,
            "availableSpaces" to availableSpaces,
            "reservedSpaces" to reservedSpaces
        )

        db.collection("parkings")
            .add(parking)
            .addOnSuccessListener { documentReference ->
                Log.d("AddParkingActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
                Toast.makeText(this, R.string.parking_added_success, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("AddParkingActivity", "Error adding document", e)
                Toast.makeText(this, R.string.parking_added_failure, Toast.LENGTH_SHORT).show()
            }
    }
}