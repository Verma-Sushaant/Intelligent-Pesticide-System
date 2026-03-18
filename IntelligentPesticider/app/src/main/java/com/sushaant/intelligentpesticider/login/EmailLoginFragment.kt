package com.sushaant.intelligentpesticider.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase
import com.sushaant.intelligentpesticider.home.HomeActivity
import com.sushaant.intelligentpesticider.R
import com.sushaant.intelligentpesticider.utils.UserFirestore

class EmailLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_email_login, container, false)

        auth = FirebaseAuth.getInstance()

        val edtEmail = view.findViewById<EditText>(R.id.edtEmail)
        val edtPassword = view.findViewById<EditText>(R.id.edtPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnEmailLogin)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val pass = edtPassword.text.toString().trim()

            if(email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(),"Fields cannot be empty",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            login(email, pass)
        }
        return view
    }

    private fun login(email: String, pass: String) {
        (activity as MainActivity).showLoading()

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val user = task.result!!.user!!

                    UserFirestore.createUserIfNotExists(
                        uid = user.uid,
                        email = user.email,
                        phone = user.phoneNumber,
                        name = user.displayName,
                        provider = "email"
                    ) {
                        (activity as MainActivity).hideLoading()

                        Toast.makeText(requireContext(),"Login Successful", Toast.LENGTH_SHORT).show()
                        goToHome()
                    }
                    return@addOnCompleteListener
                }
                (activity as MainActivity).hideLoading()
                val ex = task.exception
                when(ex) {
                    is FirebaseAuthInvalidUserException -> {
                        createNewUser(email, pass)
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        if(ex.errorCode == "ERROR_USER_NOT_FOUND") {
                            createNewUser(email, pass)
                        } else {
                            Toast.makeText(requireContext(),"Invalid Credentials: ${ex.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        Toast.makeText(requireContext(),"Sign-In Failed: ${ex?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
//                else {
//                    val msg = task.exception?.message ?: ""
//
//                    if(msg.contains("no user record", ignoreCase = true) ||
//                        msg.contains("user may have been deleted", ignoreCase = true)) createNewUser(email, pass)
//                    else {
//                        (activity as MainActivity).hideLoading()
//                        Toast.makeText(requireContext(),"Error: $msg", Toast.LENGTH_SHORT).show()
//                    }
//                }
            }
    }

    private fun createNewUser(email: String, pass: String) {

        if (pass.length < 6) {
            Toast.makeText(requireContext(), "Password must be atleast 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        (activity as MainActivity).showLoading()
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                (activity as MainActivity).hideLoading()
                if(task.isSuccessful) {
                    val user = auth.currentUser!!
                    UserFirestore.createUserIfNotExists(
                        uid = user.uid,
                        email = user.email,
                        phone = user.phoneNumber,
                        name = user.displayName,
                        provider = "email"
                    ){
                        Toast.makeText(requireContext(),"Account Created Successfully", Toast.LENGTH_SHORT).show()
                        goToHome()
                    }
                }
                else {
                    val ex = task.exception
                    when(ex) {
                        is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(requireContext(),"Weak Password: ${ex.message}", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(requireContext(),"User Already Exists: ${ex.message}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(requireContext(),"Sign-up failed: ${ex?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
//                    (activity as MainActivity).hideLoading()
//                    Toast.makeText(requireContext(),"Signup failed, ${task.exception?.message}",Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToHome() {
        startActivity(Intent(requireContext(), HomeActivity::class.java))
        requireActivity().finish()
    }
}