package com.example.smartparkingsystemm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparkingsystemm.databinding.ActivityPaymentBinding
import com.google.firebase.database.FirebaseDatabase

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reservationDuration = intent.getIntExtra("reservationDuration", 0)
        val amountToBePaid = reservationDuration * 10
        binding.amountToBePaidTextView.text = "MAD $amountToBePaid"

        val reservationId = intent.getStringExtra("reservationId")
        val isExtension = intent.getBooleanExtra("isExtension", false)

        binding.paymentMethodRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.paypalRadioButton -> {
                    binding.paypalLayout.visibility = View.VISIBLE
                    binding.cardLayout.visibility = View.GONE
                }
                R.id.debitCardRadioButton -> {
                    binding.paypalLayout.visibility = View.GONE
                    binding.cardLayout.visibility = View.VISIBLE
                }
            }
        }

        binding.confirmButton.setOnClickListener {
            Toast.makeText(this, "Payment Confirmed!", Toast.LENGTH_SHORT).show()

            // Save reservation data after payment confirmation
            val parkingId = intent.getStringExtra("parkingId")
            val userId = intent.getStringExtra("userId")
            val reservationDateTimestamp = intent.getLongExtra("reservationDateTimestamp", 0)
            val reservationHour = intent.getIntExtra("reservationHour", 0)
            val reservationMinute = intent.getIntExtra("reservationMinute", 0)

            val reservation = Reservation(
                reservationId ?: "",
                userId ?: "",
                parkingId ?: "",
                reservationDateTimestamp,
                reservationHour,
                reservationMinute,
                reservationDuration
            )

            if (reservationId != null && isExtension) {
                // Update existing reservation
                val database = FirebaseDatabase.getInstance()
                val reservationRef = database.getReference("reservations")
                reservationRef.child(reservationId).updateChildren(mapOf(
                    "duration" to reservationDuration
                )).addOnSuccessListener {
                    // Send the user to MyReservationActivity
                    val intent = Intent(this, MyReservationActivity::class.java)
                    intent.putExtra("reservation", reservation)
                    startActivity(intent)
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to update reservation: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Save new reservation
                val database = FirebaseDatabase.getInstance()
                val reservationRef = database.getReference("reservations")
                val newReservationRef = reservationRef.push()
                val newReservationId = newReservationRef.key ?: ""
                val parkingName = intent.getStringExtra("parkingName")

                val modifiedReservation = reservation.copy(id = newReservationId)
                newReservationRef.setValue(modifiedReservation).addOnSuccessListener {
                    // Send the user to MyReservationActivity
                    val intent = Intent(this, MyReservationActivity::class.java)
                    intent.putExtra("parkingName", parkingName)
                    intent.putExtra("reservation", modifiedReservation)
                    startActivity(intent)
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to save reservation: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            Toast.makeText(this, "Payment Canceled!", Toast.LENGTH_SHORT).show()

            // This will finish this activity and take the user back to the Reservation activity
            finish()
        }
    }
    override fun onBackPressed() {
        val intent = Intent(this, ParkingActivity::class.java)
        startActivity(intent)
    }
}