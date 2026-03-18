package com.sushaant.intelligentpesticider.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.sushaant.intelligentpesticider.R

class HomeFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private lateinit var noDevicesTxtView: TextView
    private lateinit var fab: FloatingActionButton
    private val deviceList = mutableListOf<DeviceModel>()
    private var realtimeDbListener: ValueEventListener? = null
    private var userDevicesRef = FirebaseDatabase.getInstance().reference

//    private var firestoreListener: ListenerRegistration? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.deviceRecycler)
        noDevicesTxtView = view.findViewById(R.id.txtNoDevices)
        fab = view.findViewById(R.id.fabAddDevice)

        adapter = DeviceAdapter(deviceList) { device ->
            DeviceDetailsSheet(device).show(parentFragmentManager, "details")
        }

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)


        recycler.adapter = adapter
        fab.setOnClickListener {
            AddDeviceDialog().show(parentFragmentManager, "AddDeviceDialog")
        }
    }

    override fun onStart() {
        super.onStart()
        loadUserDevices()
    }

    override fun onStop() {
        super.onStop()
        realtimeDbListener?.let {
            userDevicesRef.removeEventListener(it)
        }
//        firestoreListener?.remove()
    }

    private fun loadUserDevices() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        userDevicesRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("devices")

        realtimeDbListener = userDevicesRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceList.clear()

                if (snapshot.exists()) {
                    for (deviceSnapshot in snapshot.children) {
                        // The key of the child is the deviceId
                        val deviceId = deviceSnapshot.key ?: continue
//                        if (deviceId != null) {
//                            val device = DeviceModel(deviceId = deviceId, name = "Loading...")
//                            deviceList.add(device)
//                        }
                        listenToDevice(deviceId)
                    }
                } else {
                    // This will now correctly reflect if devices were found or not
                    updateUI()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("HomeFragment", "Failed to read device list from Realtime DB.", error.toException())
            }
        })
//        FirebaseFirestore.getInstance()
//            .collection("users")
//            .document(uid)
//            .collection("devices")
//            .addSnapshotListener { snap, _ ->
//                deviceList.clear()
//                snap?.documents?.forEach {
//                    deviceList.add(it.toObject(DeviceModel::class.java)!!)
//                }
//                adapter.notifyDataSetChanged()
//            }
    }

    private fun listenToDevice(deviceId: String) {
        val uid = FirebaseAuth.getInstance().uid ?: return

        val deviceRef = FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(deviceId)

        deviceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()) return

                val ownerId = snapshot.child("ownerId")
                    .getValue(String::class.java)

                val paired = snapshot.child("paired")
                    .getValue(Boolean::class.java) ?: false

                // 🔒 HARD FILTER — SOURCE OF TRUTH
                if (ownerId != uid || !paired) {
                    deviceList.removeAll { it.deviceId == deviceId }
                    updateUI()
                    return
                }

                val name = snapshot
                    .child("meta")
                    .child("deviceName")
                    .getValue(String::class.java) ?: deviceId

                val lastUpdated = snapshot
                    .child("lastUpdated")
                    .getValue(Long::class.java) ?: 0L

                val lastSeen = snapshot
                    .child("lastOnline").getValue(Long::class.java) ?: 0L

                val online = System.currentTimeMillis() - lastSeen < 30_000

                // Update or add device
                val index = deviceList.indexOfFirst{ it.deviceId == deviceId }

                if(index >= 0) {
                    deviceList[index].name = name
                    deviceList[index].online = online
                    deviceList[index].lastSeen = lastSeen
                    deviceList[index].lastUpdated = lastUpdated
                } else {
                    deviceList.add(DeviceModel(deviceId, name, online, lastSeen, lastUpdated))
                }
                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Device $deviceId read failed", error.toException())
            }
        })
    }

    private fun updateUI() {
        noDevicesTxtView.isVisible = deviceList.isEmpty()
        recycler.isVisible = deviceList.isNotEmpty()
        adapter.notifyDataSetChanged()
    }
}
