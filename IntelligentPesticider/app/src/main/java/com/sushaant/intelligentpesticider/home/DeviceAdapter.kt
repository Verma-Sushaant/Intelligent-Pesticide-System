package com.sushaant.intelligentpesticider.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sushaant.intelligentpesticider.R

class DeviceAdapter(
    private val list: List<DeviceModel>,
    private val onClick: (DeviceModel) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val dname = view.findViewById<TextView>(R.id.txtDeviceName)
        val statusDot = view.findViewById<View>(R.id.statusDot)
        val status = view.findViewById<TextView>(R.id.txtStatus)
//        val moisture = view.findViewById<TextView>(R.id.txtMoisture)
//        val temp = view.findViewById<TextView>(R.id.txtTemp)
//        val humidity = view.findViewById<TextView>(R.id.txtHumidity)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.devices, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val device = list[pos]
        holder.dname.text = device.name

        if(device.online) {
            holder.status.text = "Online"
            holder.status.setTextColor(
                holder.itemView.context.getColor(R.color.green)
            )
            holder.statusDot.setBackgroundResource(R.drawable.green_dot)
        }
        else {
            holder.status.text = "Offline"
            holder.status.setTextColor(
                holder.itemView.context.getColor(R.color.red)
            )
            holder.statusDot.setBackgroundResource(R.drawable.red_dot)
        }

//        FirebaseDatabase.getInstance()
//            .getReference("devices/${device.deviceId}")
//            .addValueEventListener(object: ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
////                    val moistureVal =
////                        snapshot.child("sensors/soilMoisture").getValue(Int::class.java) ?: 0
//
//                    val lastSeen =
//                        snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L
//
//                    val online =
//                        System.currentTimeMillis() - lastSeen < 2 * 60 * 1000 // 2 min heartbeat
//
//                    holder.status.text = if (online) "Online" else "Offline"
////                    "Moisture: $moistureVal%".also { holder.moisture.text = it }
//
//                }
//
//                override fun onCancelled(error: DatabaseError) {  }
//            })
        holder.itemView.setOnClickListener { onClick(device) }
    }
    override fun getItemCount() = list.size
}