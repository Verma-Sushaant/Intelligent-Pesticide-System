package com.sushaant.intelligentpesticider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvName = view.findViewById(R.id.txtName)
        tvEmail = view.findViewById(R.id.txtEmail)
        tvPhone = view.findViewById(R.id.txtPhone)


        val user = FirebaseAuth.getInstance().currentUser
        tvName.text = user?.displayName ?: "User"
        tvEmail.text = user?.email ?: "Not Available"
        tvPhone.text = user?.phoneNumber ?: "Not Available"

        return view
    }
}