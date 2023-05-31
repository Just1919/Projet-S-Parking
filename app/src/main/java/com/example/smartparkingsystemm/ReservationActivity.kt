package com.example.smartparkingsystemm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparkingsystemm.databinding.ActivityReservationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import android.app.DatePickerDialog
import android.widget.Toast

class ReservationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReservationBinding
    private var year = 0
    private var month = 0
    private var day = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isExtension = intent.getBooleanExtra("isExtension", false)

        if (isExtension) {
            binding.durationNumberPicker.minValue = 0
            binding.durationNumberPicker.maxValue = 48
            binding.dateButton.isEnabled = false
            binding.timePicker.isEnabled = false
        } else {
            binding.durationNumberPicker.minValue = 1
            binding.durationNumberPicker.maxValue = 48
        }

        val parkingId = intent.getStringExtra("parkingId")

        // Initialize to current date
        val currentDate = Calendar.getInstance()
        year = currentDate.get(Calendar.YEAR)
        month = currentDate.get(Calendar.MONTH)
        day = currentDate.get(Calendar.DAY_OF_MONTH)

        // Set on click listener for date button
        binding.dateButton.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    year = selectedYear
                    month = selectedMonth
                    day = selectedDay
                    val selectedDate = "$day/${month + 1}/$year"
                    binding.dateButton.text = selectedDate
                },
                year, month, day
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        val reservationToModify: Reservation? = intent.getSerializableExtra("reservation") as Reservation?

        if (reservationToModify != null) {
            // Set reservation details
            val dateToModify = Calendar.getInstance()
            dateToModify.timeInMillis = reservationToModify.date
            year = dateToModify.get(Calendar.YEAR)
            month = dateToModify.get(Calendar.MONTH)
            day = dateToModify.get(Calendar.DAY_OF_MONTH)
            val selectedDate = "$day/${month + 1}/$year"
            binding.dateButton.text = selectedDate

            binding.durationNumberPicker.value = reservationToModify.duration
            binding.timePicker.currentHour = reservationToModify.timeHour
            binding.timePicker.currentMinute = reservationToModify.timeMinute
            // If it's an extension, update the title
            if (isExtension) {
                binding.durationTitleTextView.text = "Additional Duration in hours"
            }
        }

        if (reservationToModify != null) {
            // If it's a modification, update the button texts
            if (!isExtension) {
                binding.submitReservationButton.text = "Submit Modification"
                binding.cancelReservationButton.text = "Cancel Modification"
            } else {
                binding.submitReservationButton.text = "Submit Extension"
                binding.cancelReservationButton.text = "Cancel Extension"
            }
        }

        fun isDateTimeInTheFuture(year: Int, month: Int, day: Int, hour: Int, minute: Int, ignoreTime: Boolean = false): Boolean {
            val currentTime = Calendar.getInstance()
            val selectedTime = Calendar.getInstance()

            selectedTime.set(year, month, day, hour, minute)

            return if (ignoreTime) {
                selectedTime.get(Calendar.DAY_OF_YEAR) >= currentTime.get(Calendar.DAY_OF_YEAR)
            } else {
                selectedTime.timeInMillis > currentTime.timeInMillis
            }
        }

        // Submit reservation button
        binding.submitReservationButton.setOnClickListener {
            // Check if the chosen date and time are valid
            if (!isDateTimeInTheFuture(year, month, day, binding.timePicker.currentHour, binding.timePicker.currentMinute, isExtension)) {
                // Show a message that the reservation must be in the future
                Toast.makeText(this, "Reservation time must be in the future!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reservationDuration = if (isExtension) {
                reservationToModify!!.duration + binding.durationNumberPicker.value
            } else {
                binding.durationNumberPicker.value
            }
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            val reservationDateTimestamp = calendar.timeInMillis

            val reservationHour = binding.timePicker.currentHour
            val reservationMinute = binding.timePicker.currentMinute

            val parkingName = intent.getStringExtra("parkingName")

            if (!isExtension && reservationToModify != null) {
                // Update existing reservation
                val database = FirebaseDatabase.getInstance()
                val reservationRef = database.getReference("reservations")
                val existingReservationId = reservationToModify.id ?: ""

                val updatedReservation = Reservation(
                    existingReservationId,
                    userId ?: "",
                    parkingId ?: "",
                    reservationDateTimestamp,
                    reservationHour,
                    reservationMinute,
                    reservationDuration
                )
                reservationRef.child(existingReservationId).setValue(updatedReservation).addOnSuccessListener {
                    startActivity(Intent(this, MyReservationActivity::class.java).apply {
                        putExtra("parkingName", parkingName)
                        putExtra("reservation", updatedReservation)
                    })
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to update reservation: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // ... other code for new and extension reservation ...
                val intent = Intent(this, PaymentActivity::class.java)
                intent.putExtra("parkingName", parkingName)
                intent.putExtra("reservationDuration", reservationDuration)
                intent.putExtra("parkingId", parkingId)
                intent.putExtra("userId", userId)
                intent.putExtra("reservationDateTimestamp", reservationDateTimestamp)
                intent.putExtra("reservationHour", reservationHour)
                intent.putExtra("reservationMinute", reservationMinute)
                intent.putExtra("reservationId", reservationToModify?.id)
                intent.putExtra("isExtension", isExtension)
                startActivity(intent)
            }
        }

        binding.cancelReservationButton.setOnClickListener {
            // This will finish this activity and take the user back to the previous activity
            finish()
        }

    }
}