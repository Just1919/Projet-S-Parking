package com.example.smartparkingsystemm

import com.google.firebase.database.*

class FirebaseDatabaseHelper {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun addParkingLot(parkingLot: Parking) {
        val parkingLotsRef: DatabaseReference = database.getReference("parkingLots")
        parkingLotsRef.child(parkingLot.id).setValue(parkingLot)
    }

    fun getAllParkingLots(callback: (List<Parking>) -> Unit) {
        val parkingLotsRef: DatabaseReference = database.getReference("parkingLots")
        parkingLotsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val parkingLots = mutableListOf<Parking>()
                for (lotSnapshot in dataSnapshot.children) {
                    val parkingLot = lotSnapshot.getValue(Parking::class.java)
                    if (parkingLot != null) {
                        parkingLots.add(parkingLot)
                    }
                }
                callback(parkingLots)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // TODO: Handle error
            }
        })
    }
}
