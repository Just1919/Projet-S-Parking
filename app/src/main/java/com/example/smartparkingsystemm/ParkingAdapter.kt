package com.example.smartparkingsystemm

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ParkingAdapter(private var parkings: List<Parking>, private val isAdmin: Boolean) : RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder>() {

    inner class ParkingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.parkingName)
        val status: TextView = view.findViewById(R.id.parkingStatus)
        val details: View = view.findViewById(R.id.parkingDetails)
        val address: TextView = view.findViewById(R.id.parkingAddress)
        val availableSpaces: TextView = view.findViewById(R.id.availableSpaces)
        val modifyIcon: View = view.findViewById(R.id.modify_icon)
        val deleteIcon: View = view.findViewById(R.id.delete_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.parking_item, parent, false)
        return ParkingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = parkings[position]
        holder.name.text = parking.name
        holder.status.text = if (parking.availableSpaces > 0) "Available" else "Full"
        holder.address.text = parking.address
        holder.availableSpaces.text = "Available spaces: ${parking.availableSpaces}"

        holder.name.setOnClickListener {
            if (holder.details.visibility == View.GONE) {
                holder.details.visibility = View.VISIBLE
            } else {
                holder.details.visibility = View.GONE
            }
        }
        holder.details.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ParkingDetailsActivity::class.java)
            intent.putExtra("SelectedParking", parking)
            context.startActivity(intent)
        }
        if (isAdmin) {
            holder.modifyIcon.visibility = View.VISIBLE
            holder.deleteIcon.visibility = View.VISIBLE

            holder.modifyIcon.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, ModifyParkingActivity::class.java)
                intent.putExtra("SelectedParking", parking)
                context.startActivity(intent)
            }
        } else {
            holder.modifyIcon.visibility = View.GONE
            holder.deleteIcon.visibility = View.GONE
        }
    }

    override fun getItemCount() = parkings.size

    fun updateParkings(newParkings: List<Parking>) {
        this.parkings = newParkings
        notifyDataSetChanged()
    }
}
