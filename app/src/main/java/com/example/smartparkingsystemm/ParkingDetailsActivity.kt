package com.example.smartparkingsystemm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparkingsystemm.databinding.ActivityParkingDetailsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.Toast;

class ParkingDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityParkingDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParkingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up map view, text views and button functionality here
        val parking = intent.getParcelableExtra<Parking>("SelectedParking")

        print(parking)

        val parkingName = parking?.name ?: "Name not available"
        val parkingAddress = parking?.address ?: "Address not available"
        val parkingTotalSpaces = parking?.totalSpaces ?: 0
        val parkingAvailableSpaces = parking?.availableSpaces ?: 0
        val parkingReservedSpaces = parking?.reservedSpaces ?: 0

        parking?.let {
            val parkingLocation = LatLng(it.latitude, it.longitude)

            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapView) as SupportMapFragment

            mapFragment.getMapAsync { googleMap ->
                googleMap.addMarker(
                    MarkerOptions()
                        .position(parkingLocation)
                        .title(it.name)
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(parkingLocation, 15f))
            }
            binding.parkingName.text = it.name
            binding.parkingDetails.text = "Address: ${it.address}\nAvailable Spaces: ${it.availableSpaces}"

            binding.addReservationButton.setOnClickListener {
                // Check if the user has an active reservation before proceeding
                checkForActiveReservations()
            }
        }

    }
    private fun checkForActiveReservations() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        val parking = intent.getParcelableExtra<Parking>("SelectedParking")

        // Check if the parking has available spaces
        if (parking?.availableSpaces == 0) {
            Toast.makeText(this@ParkingDetailsActivity, "There are no available spaces in this parking.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val reservationRef = database.getReference("reservations")

        reservationRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                var hasActiveReservation = false

                for (reservationSnapshot in dataSnapshot.children) {
                    val reservation = reservationSnapshot.getValue(Reservation::class.java)

                    if (reservation != null) {
                        val reservationEndTime = reservation.date + reservation.duration * 60 * 60 * 1000

                        if (reservationEndTime >= currentTime) {
                            hasActiveReservation = true
                            break
                        }
                    }
                }

                if (hasActiveReservation) {
                    Toast.makeText(this@ParkingDetailsActivity, "You already have an active reservation.", Toast.LENGTH_SHORT).show()
                } else {
                    val parkingId = intent.getStringExtra("parkingId")
                    val parkingName = intent.getStringExtra("parkingName")

                    val intent = Intent(this@ParkingDetailsActivity, ReservationActivity::class.java)
                    intent.putExtra("parkingId", parkingId)
                    intent.putExtra("parkingName", parkingName)
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ParkingDetailsActivity, "Failed to check active reservations: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

