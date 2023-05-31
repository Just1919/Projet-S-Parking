package com.example.smartparkingsystemm

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ModifyParkingActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var selectedParking: Parking

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_parking)

        selectedParking = intent.getParcelableExtra("SelectedParking")!!

        populateInputFields()

        val updateButton = findViewById<Button>(R.id.update_parking_submit_button)
        updateButton.setOnClickListener {
            updateParking()
        }
    }

    private fun populateInputFields() {
        val parkingNameEditText = findViewById<EditText>(R.id.parking_name_edittext)
        val parkingAddressEditText = findViewById<EditText>(R.id.parking_address_edittext)
        val parkingLatitudeEditText = findViewById<EditText>(R.id.parking_latitude_edittext)
        val parkingLongitudeEditText = findViewById<EditText>(R.id.parking_longitude_edittext)
        val parkingTotalSpacesEditText = findViewById<EditText>(R.id.parking_total_spaces_edittext)

        parkingNameEditText.setText(selectedParking.name)
        parkingAddressEditText.setText(selectedParking.address)
        parkingLatitudeEditText.setText(selectedParking.latitude.toString())
        parkingLongitudeEditText.setText(selectedParking.longitude.toString())
        parkingTotalSpacesEditText.setText(selectedParking.totalSpaces.toString())
    }

    private fun updateParking() {
        val parkingId = selectedParking.id
        val name = findViewById<EditText>(R.id.parking_name_edittext).text.toString()
        val address = findViewById<EditText>(R.id.parking_address_edittext).text.toString()
        val latitude = findViewById<EditText>(R.id.parking_latitude_edittext).text.toString().toDouble()
        val longitude = findViewById<EditText>(R.id.parking_longitude_edittext).text.toString().toDouble()
        val totalSpaces = findViewById<EditText>(R.id.parking_total_spaces_edittext).text.toString().toInt()
        val availableSpaces = totalSpaces - selectedParking.reservedSpaces

        val updatedParking = hashMapOf(
            "name" to name,
            "address" to address,
            "latitude" to latitude,
            "longitude" to longitude,
            "totalSpaces" to totalSpaces,
            "availableSpaces" to availableSpaces,
            "reservedSpaces" to selectedParking.reservedSpaces
        )

        db.collection("parkings").document(parkingId)
            .set(updatedParking)
            .addOnSuccessListener {
                Toast.makeText(this, "Parking updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating parking", Toast.LENGTH_SHORT).show()
            }
    }
}