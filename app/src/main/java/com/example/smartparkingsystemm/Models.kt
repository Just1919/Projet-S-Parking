package com.example.smartparkingsystemm

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable


// ParkingLot data class
data class Parking(
    var id: String = "",
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var totalSpaces: Int = 0,
    var availableSpaces: Int = 0,
    var reservedSpaces: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeInt(totalSpaces)
        parcel.writeInt(availableSpaces)
        parcel.writeInt(reservedSpaces)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Parking> {
        override fun createFromParcel(parcel: Parcel): Parking {
            return Parking(parcel)
        }

        override fun newArray(size: Int): Array<Parking?> {
            return arrayOfNulls(size)
        }
    }
}

// Reservation data class
data class Reservation(
    val id: String? = null,
    val userId: String = "",
    val parkingId: String = "",
    val date: Long = 0L,
    val timeHour: Int = 0,
    val timeMinute: Int = 0,
    val duration: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {

    // Add default no-arg constructor for Firebase
    constructor() : this(null, "", "", 0L, 0, 0, 0)
}

// User data class
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
)