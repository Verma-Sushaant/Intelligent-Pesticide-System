package com.sushaant.intelligentpesticider.home

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sushaant.intelligentpesticider.R
import androidx.core.view.get
import androidx.core.view.size
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import androidx.core.graphics.toColorInt

class DeviceDetailsSheet(
    private val device: DeviceModel
) : BottomSheetDialogFragment() {
    private var isDeviceOnline = true

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.layoutParams?.height =
            (resources.displayMetrics.heightPixels * 0.9).toInt()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.sheet_device_details, container, false)

        val tempCard = view.findViewById<View>(R.id.cardTemp)
        val humCard = view.findViewById<View>(R.id.cardHumidity)
        val soilCard = view.findViewById<View>(R.id.cardSoil)
        val infCard = view.findViewById<View>(R.id.cardInfection)

        val btnMenu = view.findViewById<ImageView>(R.id.btnMenu)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnSpray = view.findViewById<View>(R.id.btnSpray)
        val txtLastSprayed = view.findViewById<TextView>(R.id.txtLastSprayed)

        val db = FirebaseDatabase.getInstance()
            .reference.child("devices").child(device.deviceId)

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                setGauge(tempCard, "Temperature", s.child("sensors/temperature").getValue(Float::class.java) ?: 0f)
                setGauge(humCard, "Humidity", s.child("sensors/humidity").getValue(Float::class.java) ?: 0f)
                setGauge(soilCard, "Moisture", s.child("sensors/soilMoisture").getValue(Float::class.java) ?: 0f)
                setGauge(infCard, "Infection", s.child("infection/probability").getValue(Float::class.java) ?: 0f)

                val lastTimeStamp = s.child("pesticide/lastSprayedAt").getValue(Long::class.java) ?: 0L
                if(lastTimeStamp > 0) {
                    txtLastSprayed.text = "Last sprayed: " +
                            android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", lastTimeStamp)
                }
                else {
                    txtLastSprayed.text = "Last sprayed: Never"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnSpray.setOnClickListener {
            if(!isDeviceOnline) {
                Toast.makeText(context, "Device offline", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.child("commands/sprayNow").setValue(true)
                .addOnSuccessListener {
                    Toast.makeText(context, "Spray command sent", Toast.LENGTH_SHORT).show()
                }
        }

        btnMenu.setOnClickListener {
            showMenu(it)
        }

        btnClose.setOnClickListener {
            dismiss()
        }

        return view
    }

//    private fun setGauge(card: View, label: String, value: Float) {
//        card.findViewById<TextView>(R.id.txtLabel).text = label
//        card.findViewById<TextView>(R.id.txtValue).text = "${value.toInt()}"
//        card.findViewById<com.sushaant.intelligentpesticider.ui.SemiCircularGaugeView>(R.id.gauge).value = value
//    }
    private fun setGauge(card: View, label: String, value: Float) {
        val gauge = card.findViewById<com.sushaant.intelligentpesticider.ui.SemiCircularGaugeView>(R.id.gauge)
        card.findViewById<TextView>(R.id.txtLabel).text = label
//        card.findViewById<TextView>(R.id.txtValue).text = "${value.toInt()}"

        // Define the color gradients based on the label
        when (label) {
            "Temperature" -> {
                // Gradient: Light Blue -> Yellow -> Red
                val colors = intArrayOf(
                    "#81D4FA".toColorInt(), // Light Blue
                    "#FFEB3B".toColorInt(), // Yellow
                    "#F44336".toColorInt()  // Red
                )
                val positions = floatArrayOf(0f, 0.5f, 1f)
                gauge.setGradient(colors, positions)
            }
            "Humidity" -> {
                // Gradient: Reddish Pink -> Light Blue (optimal) -> Light Purple
                val colors = intArrayOf(
                    "#F06292".toColorInt(), // Reddish Pink (low)
                    "#81D4FA".toColorInt(), // Light Blue (optimal)
                    "#B39DDB".toColorInt()  // Light Purple (high)
                )
                // Position the optimal blue color in the middle
                val positions = floatArrayOf(0f, 0.5f, 1f)
                gauge.setGradient(colors, positions)
            }
            "Soil" -> {
                // Gradient: Light Brown -> Dark Blue
                val colors = intArrayOf(
                    "#A1887F".toColorInt(), // Light Brown (low/dry)
                    "#1565C0".toColorInt()  // Dark Blue (high/wet)
                )
                gauge.setGradient(colors)
            }
            "Infection" -> {
                // Gradient: Green -> Yellow -> Orange
                val colors = intArrayOf(
                    "#66BB6A".toColorInt(), // Green
                    "#FFEE58".toColorInt(), // Yellow
                    "#FFA726".toColorInt()  // Orange
                )
                val positions = floatArrayOf(0f, 0.5f, 1f)
                gauge.setGradient(colors, positions)
            }
        }

        // Finally, set the value to trigger the drawing
        gauge.value = value
    }

    private fun showMenu(anchor: View) {
        val db = FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(device.deviceId)

        db.child("commands/startMonitoring").get().addOnSuccessListener { dataSnapshot ->
            val isCurrentlyMonitoring = dataSnapshot.getValue(Boolean::class.java) ?: true
            val popup = PopupMenu(requireContext(), anchor)
            val monitoringMenuTitle = if(isCurrentlyMonitoring) {
                "Stop Monitoring"
            }
            else {
                "Start Monitoring"
            }
            popup.menu.add(0, 1, 0, "Delete Device")
            popup.menu.add(0, 2, 1, monitoringMenuTitle)
            popup.menu.add(0, 3, 2, "Reset Device")
            popup.menu.add(0, 4, 3, "AutoCapture (Dev) 🔒")

            for (i in 0 until popup.menu.size) {
                popup.menu[i].isEnabled = isDeviceOnline
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    1 -> confirmDeleteDevice()

                    2 -> toggleMonitoring()

                    3 -> resetDevice()

                    4 -> showDevPinDialog()
                }
                true
            }
            popup.show()
        }.addOnFailureListener {
            Toast.makeText(context, "Could not get device status", Toast.LENGTH_SHORT).show()
        }

    }

//    private fun confirmDeleteDevice() {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Delete Device")
//            .setMessage("Are you sure you want to remove this device?")
//            .setPositiveButton("Delete") { _, _ ->
//                val uid = FirebaseAuth.getInstance().uid ?: return@setPositiveButton
//                val db = FirebaseDatabase.getInstance().reference
//
//                db.child("users").child(uid).child("devices")
//                    .child(device.deviceId).removeValue()
//
//                db.child("devices").child(device.deviceId).removeValue()
//
//                Toast.makeText(context, "Device deleted", Toast.LENGTH_SHORT).show()
//                dismiss()
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
    private fun confirmDeleteDevice() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Device")
            .setMessage("This will unpair the device. You can re-pair it later.")
            .setPositiveButton("Unpair") { _, _ ->

                val uid = FirebaseAuth.getInstance().uid ?: return@setPositiveButton
                val root = FirebaseDatabase.getInstance().reference
                val deviceRef = root.child("devices").child(device.deviceId)

                val updates = hashMapOf<String, Any?>(
                    "paired" to false,
                    "ownerId" to null,
                    "status" to "UNPAIRED",
                    "unpairedAt" to System.currentTimeMillis()
                )

                deviceRef.updateChildren(updates)
                deviceRef.child("commands").setValue(
                    mapOf(
                        "autoCaptureEnabled" to true,
                        "startMonitoring" to true,
                        "sprayNow" to false,
                        "resetDevice" to false
                    )
                )
                // Remove device from user list ONLY
                root.child("users")
                    .child(uid)
                    .child("devices")
                    .child(device.deviceId)
                    .removeValue()

                Toast.makeText(context, "Device unpaired", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleMonitoring() {
        val ref = FirebaseDatabase.getInstance()
            .reference.child("devices")
            .child(device.deviceId)
            .child("commands")
            .child("startMonitoring")

        ref.get().addOnSuccessListener {
            val current = it.getValue(Boolean::class.java) ?: true
            ref.setValue(!current)

            Toast.makeText(
                context,
                if (!current) "Monitoring Started" else "Monitoring Paused",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun resetDevice() {
        FirebaseDatabase.getInstance()
            .reference.child("devices")
            .child(device.deviceId)
            .child("commands")
            .child("resetDevice")
            .setValue(true)

        Toast.makeText(context, "Reset command sent", Toast.LENGTH_SHORT).show()
    }

    private fun showDevPinDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter Dev PIN"

        AlertDialog.Builder(requireContext())
            .setTitle("Developer Verification")
            .setView(input)
            .setPositiveButton("Verify") { _, _ ->
                verifyDevPinAndProceed(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyDevPinAndProceed(pin: String) {
        if (pin != getDevPinFromRemoteConfig()) {
            Toast.makeText(context, "Invalid PIN", Toast.LENGTH_SHORT).show()
            return
        }
        confirmAutoCaptureDisable()
    }

    private fun confirmAutoCaptureDisable() {
        AlertDialog.Builder(requireContext())
            .setTitle("Disable AutoCapture")
            .setMessage("AutoCapture will be disabled for 20 minutes.")
            .setPositiveButton("Disable") { _, _ ->
                disableAutoCaptureTemporarily()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun disableAutoCaptureTemporarily() {
        val ref = FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(device.deviceId)
            .child("commands")

        val now = System.currentTimeMillis()

        ref.child("autoCaptureEnabled").setValue(false)
        ref.child("autoCaptureDisabledAt").setValue(now)

        Toast.makeText(
            context,
            "AutoCapture disabled for 20 minutes",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getDevPinFromRemoteConfig(): String {
        val rc = FirebaseRemoteConfig.getInstance()

        // Make sure this key exists in Firebase Console
        return rc.getString("DEV_PIN")
    }
}
