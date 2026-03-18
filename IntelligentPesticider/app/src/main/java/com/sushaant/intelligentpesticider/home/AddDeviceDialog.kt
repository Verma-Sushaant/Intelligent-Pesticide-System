package com.sushaant.intelligentpesticider.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.sushaant.intelligentpesticider.R

class AddDeviceDialog : DialogFragment() {

    private lateinit var deviceName: EditText
    private lateinit var crop: Spinner
    private lateinit var code: EditText
    private lateinit var btnAdd: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val view = inflater.inflate(R.layout.add_device_dialog, container, false)

        deviceName = view.findViewById(R.id.edtDeviceName)
        crop = view.findViewById(R.id.spinnerCrop)
        code = view.findViewById(R.id.edtCode)
        btnAdd = view.findViewById(R.id.btnPair)

        val crops = resources.getStringArray(R.array.crop_list)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, crops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        crop.adapter = adapter

        btnAdd.setOnClickListener {
//            addFakeDeviceForTesting()
            addDevice()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun addFakeDeviceForTesting() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val deviceId = "test_device_001"

        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(uid).child("devices").child(deviceId)
            .setValue(true)
            .addOnSuccessListener {
                Toast.makeText(context, "Test device added", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error pairing device", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDevice() {
        val deviceName = deviceName.text.toString().trim()
        val cropName = crop.selectedItem.toString()
        val deviceId = code.text.toString().trim()

        if (deviceName.isEmpty() || cropName.isEmpty() || !deviceId.startsWith("ESP32_")) {
            Toast.makeText(context, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().uid ?: return
        val db = FirebaseDatabase.getInstance().reference

        db.child("pairingCodes").child(deviceId).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(context, "Device not available for pairing", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val deviceRef = db.child("devices").child(deviceId)

                deviceRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(data: MutableData): Transaction.Result {
                        if (data.value == null) return Transaction.abort()
                        if (data.child("ownerId").value != null) return Transaction.abort()
                        if (data.child("paired").getValue(Boolean::class.java) == true) {
                            return Transaction.abort()
                        }

                        // Core fields
                        data.child("ownerId").value = uid
                        data.child("paired").value = true
                        data.child("crop").value = cropName
                        data.child("lastUpdated").value = System.currentTimeMillis()

                        // Meta
                        data.child("meta/deviceName").value = deviceName
                        data.child("meta/createdAt").value = System.currentTimeMillis()

                        // Commands
                        data.child("commands/autoCaptureEnabled").value = true
                        data.child("commands/startMonitoring").value = true
                        data.child("commands/sprayNow").value = false
                        data.child("commands/resetDevice").value = false

                        // Pesticide info
                        data.child("pesticide/lastSprayedAt").value = 0
                        data.child("pesticide/status").value = "Never sprayed"
                        data.child("pesticide/sprayCount").value = 0

                        return Transaction.success(data)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        snapshot: DataSnapshot?
                    ) {
                        if (!committed) {
                            Toast.makeText(context, "Already paired", Toast.LENGTH_SHORT).show()
                            return
                        }

                        db.child("users").child(uid)
                            .child("devices").child(deviceId)
                            .setValue(true)

                        db.child("pairingCodes").child(deviceId).removeValue()

                        Toast.makeText(context, "Device added successfully", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                })
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error pairing device", Toast.LENGTH_SHORT).show()
            }
    }

//    private fun addDevice() {
//        val deviceName = deviceName.text.toString().trim()
//        val cropName = crop.selectedItem.toString()
//        val deviceId = code.text.toString().trim()
//
//        if (deviceName.isEmpty() || cropName.isEmpty() || !deviceId.startsWith("ESP32_")) {
//            Toast.makeText(context, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val uid = FirebaseAuth.getInstance().uid ?: return
//        val db = FirebaseDatabase.getInstance().reference
//
//        // 🔹 pairingCodes/{code} → deviceId
//        db.child("pairingCodes").child(deviceId).get()
//            .addOnSuccessListener { snapshot ->
//                if (!snapshot.exists()) {
//                    Toast.makeText(context, "Device not available for pairing", Toast.LENGTH_SHORT).show()
//                    return@addOnSuccessListener
//                }
//
//                val deviceData = mapOf(
//                    "ownerId" to uid,
//                    "paired" to true,
//                    "crop" to cropName,
//                    "lastUpdated" to System.currentTimeMillis(),
//
//                    "commands" to mapOf(
//                        "autoCaptureEnabled" to true,
//                        "startMonitoring" to true,
//                        "sprayNow" to false,
//                        "resetDevice" to false
//                    ),
//
//                    "pesticide" to mapOf(
//                        "lastSprayedAt" to 0,
//                        "status" to "Never sprayed",
//                        "sprayCount" to 0
//                    ),
//                    "meta" to mapOf(
//                        "deviceName" to deviceName,
//                        "createdAt" to System.currentTimeMillis()
//                    )
//                )
//
//                // 🔹 devices/{deviceId}
//                // db.child("devices").child(deviceId).updateChildren(deviceData)
//
//                db.child("devices").child(deviceId)
//                    .updateChildren(deviceData)
//                    .addOnSuccessListener {
//                        Toast.makeText(context, "Device paired", Toast.LENGTH_SHORT).show()
//                    }
//                    .addOnFailureListener {
//                        Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
//                    }
//
//
//                // 🔹 users/{uid}/devices/{deviceId} : true
//                db.child("users")
//                    .child(uid)
//                    .child("devices")
//                    .child(deviceId)
//                    .setValue(true)
//
//                // 🔹 Remove pairing entry after successful pairing
//                db.child("pairingCodes").child(deviceId).removeValue()
//
//                Toast.makeText(context, "Device added successfully", Toast.LENGTH_SHORT).show()
//                dismiss()
//            }
//            .addOnFailureListener {
//                Toast.makeText(context, "Error pairing device", Toast.LENGTH_SHORT).show()
//            }
//    }
//    private fun addDevice() {
//
//        val deviceNameText = deviceName.text.toString().trim()
//        val cropName = crop.selectedItem.toString()
//        val deviceId = code.text.toString().trim()
//
//        if (deviceNameText.isEmpty() || cropName.isEmpty() || !deviceId.startsWith("ESP32_")) {
//            Toast.makeText(context, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val uid = FirebaseAuth.getInstance().uid ?: return
//        val db = FirebaseDatabase.getInstance().reference
//
//        // Step 1: Check pairing code exists
//        db.child("pairingCodes").child(deviceId).get()
//            .addOnSuccessListener { snapshot ->
//
//                if (!snapshot.exists()) {
//                    Toast.makeText(context, "Device not available for pairing", Toast.LENGTH_SHORT).show()
//                    return@addOnSuccessListener
//                }
//
//                val deviceRef = db.child("devices").child(deviceId)
//
//                // Step 2: TRANSACTION (IMPORTANT)
//                deviceRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
//
//                    override fun doTransaction(currentData: com.google.firebase.database.MutableData):
//                            com.google.firebase.database.Transaction.Result {
//
//                        // Device must already exist (created by ESP)
//                        if (currentData.value == null) {
//                            return com.google.firebase.database.Transaction.abort()
//                        }
//
//                        // Prevent double pairing
//                        if (currentData.child("ownerId").value != null) {
//                            return com.google.firebase.database.Transaction.abort()
//                        }
//
//                        // 🔐 CLAIM OWNERSHIP
//                        currentData.child("ownerId").value = uid
//                        currentData.child("paired").value = true
//                        currentData.child("crop").value = cropName
//                        currentData.child("lastUpdated").value = System.currentTimeMillis()
//
//                        currentData.child("commands").child("autoCaptureEnabled").value = true
//                        currentData.child("commands").child("startMonitoring").value = true
//                        currentData.child("commands").child("sprayNow").value = false
//                        currentData.child("commands").child("resetDevice").value = false
//
//                        currentData.child("pesticide").child("lastSprayedAt").value = 0
//                        currentData.child("pesticide").child("status").value = "Never sprayed"
//                        currentData.child("pesticide").child("sprayCount").value = 0
//
//                        currentData.child("meta").child("deviceName").value = deviceNameText
//                        currentData.child("meta").child("createdAt").value = System.currentTimeMillis()
//
//                        return com.google.firebase.database.Transaction.success(currentData)
//                    }
//
//                    override fun onComplete(
//                        error: com.google.firebase.database.DatabaseError?,
//                        committed: Boolean,
//                        snapshot: com.google.firebase.database.DataSnapshot?
//                    ) {
//                        if (!committed) {
//                            Toast.makeText(context, "Device already paired", Toast.LENGTH_SHORT).show()
//                            return
//                        }
//
//                        // Step 3: Link device to user
//                        db.child("users")
//                            .child(uid)
//                            .child("devices")
//                            .child(deviceId)
//                            .setValue(true)
//
//                        // Step 4: Remove pairing code
//                        db.child("pairingCodes").child(deviceId).removeValue()
//
//                        Toast.makeText(context, "Device paired successfully", Toast.LENGTH_SHORT).show()
//                        dismiss()
//                    }
//                })
//            }
//            .addOnFailureListener {
//                Toast.makeText(context, "Pairing failed", Toast.LENGTH_SHORT).show()
//            }
//    }
//    private fun addDevice() {
//        val deviceNameText = deviceName.text.toString().trim()
//        val cropName = crop.selectedItem.toString()
//        val deviceId = code.text.toString().trim()
//
//        if (deviceNameText.isEmpty() || cropName.isEmpty() || !deviceId.startsWith("ESP32_")) {
//            Toast.makeText(context, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val uid = FirebaseAuth.getInstance().uid ?: return
//        val db = FirebaseDatabase.getInstance().reference
//
//        // Step 1: Check if the pairing code exists and is valid.
//        db.child("pairingCodes").child(deviceId).get()
//            .addOnSuccessListener { snapshot ->
//                if (!snapshot.exists()) {
//                    Toast.makeText(context, "Device not available for pairing", Toast.LENGTH_SHORT).show()
//                    return@addOnSuccessListener
//                }
//
//                // If the code exists, proceed to claim the device in a transaction.
//                claimDevice(db, deviceId, uid, deviceNameText, cropName)
//            }
//            .addOnFailureListener {
//                Toast.makeText(context, "Error checking pairing code", Toast.LENGTH_SHORT).show()
//            }
//    }
//    private fun claimDevice(db: com.google.firebase.database.DatabaseReference, deviceId: String, uid: String, deviceNameText: String, cropName: String) {
//        val deviceRef = db.child("devices").child(deviceId)
//
//        // Step 2: Use a transaction to safely claim the device.
//        deviceRef.runTransaction(object : Transaction.Handler {
//            override fun doTransaction(currentData: MutableData): Transaction.Result {
//                // The device node must already exist (created by the ESP32).
//                // If not, abort the transaction.
//                if (currentData.value == null) {
//                    return Transaction.abort()
//                }
//
//                // Prevent another user from claiming an already-paired device.
//                // If ownerId already exists, abort.
//                if (currentData.child("ownerId").value != null) {
//                    return Transaction.abort()
//                }
//
//                // If the device is available, claim it by setting its properties.
//                currentData.child("ownerId").value = uid
//                currentData.child("paired").value = true
//                currentData.child("crop").value = cropName
//                currentData.child("lastUpdated").value = System.currentTimeMillis()
//
//                currentData.child("commands/autoCaptureEnabled").value = true
//                currentData.child("commands/startMonitoring").value = true
//
//                currentData.child("pesticide/lastSprayedAt").value = 0
//                currentData.child("pesticide/status").value = "Never sprayed"
//
//                currentData.child("meta/deviceName").value = deviceNameText
//                currentData.child("meta/createdAt").value = System.currentTimeMillis()
//
//                // If all modifications are successful, return the new data.
//                return Transaction.success(currentData)
//            }
//            override fun onComplete(
//                error: DatabaseError?,
//                committed: Boolean,
//                snapshot: DataSnapshot?
//            ) {
//                // This 'onComplete' block runs *after* the transaction is finished.
//                if (error != null) {
//                    // A database-level error occurred.
//                    Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
//                    return
//                }
//
//                if (!committed) {
//                    // The transaction was aborted (e.g., device was already paired).
//                    Toast.makeText(context, "Device already paired by another user", Toast.LENGTH_SHORT).show()
//                    return
//                }
//
//                // If the transaction was successful (committed), link the device to the user
//                // and remove the temporary pairing code.
//                db.child("users").child(uid).child("devices").child(deviceId).setValue(true)
//                db.child("pairingCodes").child(deviceId).removeValue()
//
//                // NOW it is safe to show the success message and dismiss the dialog.
//                Toast.makeText(context, "Device paired successfully", Toast.LENGTH_SHORT).show()
//                dismiss()
//            }
//        })
//    }
}
