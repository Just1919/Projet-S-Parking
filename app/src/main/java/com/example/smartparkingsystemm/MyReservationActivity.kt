package com.example.smartparkingsystemm

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparkingsystemm.databinding.ActivityMyReservationBinding
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*


class MyReservationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyReservationBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reservation: Reservation = intent.getSerializableExtra("reservation") as Reservation
        val reservationId = intent.getStringExtra("reservationId") ?: ""
        val parkingName = intent.getStringExtra("parkingName") ?: reservation.parkingId

        // Display reservation details
        val createdAtFormatted =
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(reservation.createdAt)
        val reservationDateFormatted =
            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(reservation.date))

        binding.reservationInfoTextView.text = "Your reservation details: \n" +
                "Created at: $createdAtFormatted\n" +
                "Reservation starts at: $reservationDateFormatted\n" +
                "Parking Name: $parkingName\n" +
                "Duration: ${reservation.duration} hour(s)"

        // Extract year, month, and day from the reservation date
        val calendarDate = Calendar.getInstance()
        calendarDate.timeInMillis = reservation.date
        val reservationYear = calendarDate.get(Calendar.YEAR)
        val reservationMonth = calendarDate.get(Calendar.MONTH)
        val reservationDay = calendarDate.get(Calendar.DAY_OF_MONTH)

        // Set the calendar for reservation start datetime
        val calendar = Calendar.getInstance()
        calendar.set(
            reservationYear,
            reservationMonth,
            reservationDay,
            reservation.timeHour,
            reservation.timeMinute
        )
        val currentTime = System.currentTimeMillis()
        val diffMillis = calendar.timeInMillis - currentTime

        // Enable the Extend button when the reservation is in effect, and
        // set 'Modify' button initially enabled or disabled based on the reservation time
        if (diffMillis <= 0) {
            binding.extendButton.isEnabled = true
            binding.modifyButton.isEnabled = false
        } else {
            binding.extendButton.isEnabled = false
            binding.modifyButton.isEnabled = true
        }

        countDownTimer = object : CountDownTimer(diffMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val days = (millisUntilFinished / (3600 * 24 * 1000))
                val hours = ((millisUntilFinished % (3600 * 24 * 1000)) / (3600 * 1000)).toInt()
                val minutes = ((millisUntilFinished % (3600 * 1000)) / (60 * 1000)).toInt()
                val seconds = ((millisUntilFinished % (60 * 1000)) / 1000).toInt()
                binding.remainingTimeTextView.text =
                    "Time until reservation starts: ${days}d ${hours}h ${minutes}m ${seconds}s"

                if (millisUntilFinished <= 0) {
                    binding.modifyButton.isEnabled = false
                }
            }

            override fun onFinish() {
                binding.remainingTimeTextView.text = "Reservation in progress."
                binding.reservationInfoTextView.text = "Your reservation details: \n" +
                        "Created at: $createdAtFormatted\n" +
                        "Reservation in progress.\n" +
                        "Parking Name: $parkingName\n" +
                        "Duration: ${reservation.duration} hour(s)"
                binding.modifyButton.isEnabled = false
                binding.extendButton.isEnabled = true
            }
        }.start()

        // Modify Reservation button
        // Modify Reservation button
        binding.modifyButton.setOnClickListener {
            // Redirect to ReservationActivity with reservation data
            val intent = Intent(this, ReservationActivity::class.java)
            intent.putExtra("reservation", reservation)
            intent.putExtra("reservationId", reservationId) // Add this line
            startActivity(intent)
        }

        // Extend Reservation button
        binding.extendButton.setOnClickListener {
            // Redirect to ReservationActivity and set a flag to indicate it's an extension
            val intent = Intent(this, ReservationActivity::class.java)
            intent.putExtra("reservation", reservation)
            intent.putExtra("isExtension", true)
            startActivity(intent)
        }

        // Cancel Reservation button
        binding.cancelButton.setOnClickListener {
            // Remove the reservation from the database
            val database = FirebaseDatabase.getInstance()
            val reservationRef = database.getReference("reservations")
            reservationRef.child(reservationId).removeValue().addOnSuccessListener {
                // Return to MainActivity when the reservation is canceled
                val intent = Intent(this, ParkingActivity::class.java)
                startActivity(intent)
            }.addOnFailureListener { exception ->
                // Show error message
                Toast.makeText(
                    this,
                    "Failed to cancel reservation: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBackPressed() {
        val intent = Intent(this, ParkingActivity::class.java)
        startActivity(intent)
    }
}