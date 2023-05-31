package com.example.smartparkingsystemm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystemm.databinding.ActivityParkingBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.pow
import kotlin.math.sqrt
import android.util.Log
import com.google.firebase.firestore.SetOptions



class ParkingActivity : AppCompatActivity() {

    private var isAdmin = false
    private lateinit var binding: ActivityParkingBinding
    private var parkingList = mutableListOf<Parking>()
    private var adapter = ParkingAdapter(emptyList(), isAdmin)
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fusedLocationClient: FusedLocationProviderClient




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityParkingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAdmin = intent.getBooleanExtra("isAdmin", false)
        val addParkingFab = findViewById<FloatingActionButton>(R.id.add_parking_fab)

        isAdmin = intent.getBooleanExtra("isAdmin", false)
        if (isAdmin) {
            addParkingFab.visibility = View.VISIBLE
        } else {
            addParkingFab.visibility = View.GONE
        }

        addParkingFab.setOnClickListener {
            startActivity(Intent(this, AddParkingActivity::class.java))
        }

        // Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        recyclerView = findViewById(R.id.recyclerView)

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchParking(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchParking(newText ?: "")
                return true
            }
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
        binding.fab.setOnClickListener {
            checkForActiveReservation()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchParkingsFromFirebase()
    }

    private fun searchParking(query: String) {
        val filteredList = parkingList.filter { parking ->
            parking.name.contains(query, ignoreCase = true) ||
                    parking.address.contains(query, ignoreCase = true)
        }

        updateParkingList(filteredList)
    }

    private fun updateParkingList(parkingList: List<Parking>) {
        val parkingAdapter = ParkingAdapter(parkingList, isAdmin)
        recyclerView.adapter = parkingAdapter
        parkingAdapter.notifyDataSetChanged()
    }

    private fun checkForActiveReservation() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        val database = FirebaseDatabase.getInstance()
        val reservationRef = database.getReference("reservations")

        reservationRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                var activeReservation: Reservation? = null

                for (reservationSnapshot in dataSnapshot.children) {
                    val reservation = reservationSnapshot.getValue(Reservation::class.java)

                    if (reservation != null) {
                        val reservationEndTime = reservation.date + reservation.duration * 60 * 60 * 1000

                        if (reservationEndTime >= currentTime) {
                            activeReservation = reservation
                            break
                        }
                    }
                }

                if (activeReservation != null) {
                    // Start MyReservationActivity and pass the active reservation
                    val intent = Intent(this@ParkingActivity, MyReservationActivity::class.java)
                    intent.putExtra("reservation", activeReservation)
                    startActivity(intent)
                } else {
                    // Show a message that there is no active reservation
                    Toast.makeText(this@ParkingActivity, "You don't have any active reservations.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ParkingActivity, "Failed to check active reservations: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchParkingsFromFirebase() {
        parkingList.clear()
        val db = Firebase.firestore
        db.collection("parkings").get().addOnSuccessListener { result ->
            for (document in result) {
                // Get the location as a GeoPoint object
                val geoPoint = document.getGeoPoint("location")
                val latitude = geoPoint?.latitude ?: 0.0
                val longitude = geoPoint?.longitude ?: 0.0

                // Create a parking object with the latitude and longitude values
                val parking = Parking(
                    id = document.getString("id") ?: "",
                    name = document.getString("name") ?: "",
                    address = document.getString("address") ?: "",
                    latitude = latitude,
                    longitude = longitude,
                    totalSpaces = document.getLong("totalSpaces")?.toInt() ?: 0,
                    availableSpaces = document.getLong("availableSpaces")?.toInt() ?: 0,
                    reservedSpaces = document.getLong("reservedSpaces")?.toInt() ?: 0
                )
                parkingList.add(parking)
                val parkingToTrack = parkingList.find { it.id == "1" }
                if (parkingToTrack != null) {
                    listenToSensorStatusChanges(parkingToTrack)
                } else {
                    Log.d("ParkingActivity", "Parking with ID 1 not found.")
                }
                getCurrentLocation()
            }
            adapter.updateParkings(parkingList)
        }
    }
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Call the super implementation
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    // Permission not granted, handle accordingly
                }
            }
        }
    }
    private fun getCurrentLocation() {
        val task: Task<Location> = fusedLocationClient.lastLocation
        task.addOnSuccessListener { result ->
            if (result != null) {
                // Get user's current latitude and longitude
                val latitude = result.latitude
                val longitude = result.longitude

                recommendParkings(latitude, longitude)
            } else {
                // Handle situation when location is null
            }
        }
    }

    private fun euclideanDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return sqrt((lat1 - lat2).pow(2.0) + (lon1 - lon2).pow(2.0))
    }

    private fun recommendParkings(userLatitude: Double, userLongitude: Double) {
        // Calculate distances between the user's location and each parking
        val parkingDistances = parkingList.map { parking ->
            euclideanDistance(userLatitude, userLongitude, parking.latitude, parking.longitude) to parking
        }

        // Sort parking list by the calculated distances in ascending order
        val recommendedList = parkingDistances.sortedBy { it.first }.map { it.second }

        // Update the parking list in the UI
        updateParkingList(recommendedList)
    }

    private var sensor1Status: String = ""
    private var sensor2Status: String = ""

    private fun listenToSensorStatusChanges(parking: Parking) {
        val database = FirebaseDatabase.getInstance()
        val sensor1StatusReference = database.getReference("sensor1_status")
        val sensor2StatusReference = database.getReference("sensor2_status")

        val sensor1ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                sensor1Status = dataSnapshot.getValue(String::class.java) ?: ""
                updateParkingSpaces(parking)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
            }
        }

        val sensor2ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                sensor2Status = dataSnapshot.getValue(String::class.java) ?: ""
                updateParkingSpaces(parking)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
            }
        }

        sensor1StatusReference.addValueEventListener(sensor1ValueEventListener)
        sensor2StatusReference.addValueEventListener(sensor2ValueEventListener)
    }

    private fun updateParkingSpaces(parking: Parking) {
        var availableSpaces = 0
        var reservedSpaces = 0

        if (sensor1Status == "free") {
            availableSpaces++
        } else {
            reservedSpaces++
        }

        if (sensor2Status == "free") {
            availableSpaces++
        } else {
            reservedSpaces++
        }

        // Proceed with updating Firestore availableSpaces and reservedSpaces fields
        val db = Firebase.firestore
        val parkingRef = db.collection("parkings").document(parking.id)

        val data = hashMapOf<String, Any>(
            "availableSpaces" to availableSpaces,
            "reservedSpaces" to reservedSpaces
        )

        parkingRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                // Update parking object in the list
                parking.availableSpaces = availableSpaces
                parking.reservedSpaces = reservedSpaces

                // Refresh the ParkingActivity UI elements
                getCurrentLocation()
            }
            .addOnFailureListener {
                // Handle any errors
            }
    }

}