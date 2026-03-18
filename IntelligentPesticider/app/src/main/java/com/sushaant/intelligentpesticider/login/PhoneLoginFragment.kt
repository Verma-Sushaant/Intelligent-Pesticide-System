package com.sushaant.intelligentpesticider.login

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.sushaant.intelligentpesticider.home.HomeActivity
import com.sushaant.intelligentpesticider.R
import java.util.concurrent.TimeUnit

class PhoneLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var edtPhone: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var txtResend: TextView

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private var otpDialog: Dialog? = null
    private lateinit var otpBoxes: List<EditText>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_phone_login, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        edtPhone = view.findViewById(R.id.edtPhone)
        btnSendOtp = view.findViewById(R.id.btnSendOtp)
        txtResend = view.findViewById(R.id.txtResend)

        btnSendOtp.setOnClickListener {
            if(verificationId == null) sendOtp()
            else verifyOtp()
        }

        txtResend.setOnClickListener { resendOtp() }
        return view
    }

    private fun sendOtp() {
        val phone = edtPhone.text.toString().trim()

        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Enter phone number", Toast.LENGTH_SHORT).show()
            return
        }
        (activity as MainActivity).showLoading()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            (activity as MainActivity).hideLoading()
            signIn(credential)
        }

        override fun onVerificationFailed(p0: FirebaseException) {
            (activity as MainActivity).hideLoading()
            Toast.makeText(requireContext(), "Failed: ${p0.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
            (activity as MainActivity).hideLoading()

            verificationId = vid
            resendToken = token

            showOtpDialog()
            Toast.makeText(requireContext(), "OTP Sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOtpDialog() {
        otpDialog = Dialog(requireContext())
        otpDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        otpDialog!!.setContentView(R.layout.otp_dialog)
        otpDialog!!.setCancelable(false)

        otpBoxes = listOf(
            otpDialog!!.findViewById(R.id.otp1),
            otpDialog!!.findViewById(R.id.otp2),
            otpDialog!!.findViewById(R.id.otp3),
            otpDialog!!.findViewById(R.id.otp4),
            otpDialog!!.findViewById(R.id.otp5),
            otpDialog!!.findViewById(R.id.otp6)
        )

        addOtpAutoMove()
        otpDialog!!.show()
        otpDialog?.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.widthPixels * 0.957).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        otpDialog?.window?.setGravity(Gravity.CENTER)
        otpDialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }

    private fun addOtpAutoMove() {
        otpBoxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if(s!!.length == 1 && index < otpBoxes.size-1) {
                        otpBoxes[index+1].requestFocus()
                    }
                    if(index == 5 && s.length == 1) {
                        verifyOtp()
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) { }
                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int) { }
            })
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        otpBoxes[index - 1].requestFocus()
                    }
                }
                false
            }

        }
    }

    private fun verifyOtp() {
        val code = otpBoxes.joinToString("") {it.text.toString()}

        if(code.length != 6) {
            Toast.makeText(requireContext(),"Enter full OTP",Toast.LENGTH_SHORT).show()
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signIn(credential)
    }

    private fun resendOtp() {
        val phone = edtPhone.text.toString().trim()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signIn(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                (activity as MainActivity).hideLoading()

                if(otpDialog != null) otpDialog!!.dismiss()

                if(task.isSuccessful) {
                    val user = auth.currentUser
                    if(user != null) {
                        saveUserToFirestore(user.uid, user.phoneNumber!!)
                    }
                }
                else {
                    Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserToFirestore(uid: String, phone: String) {
        val userMap = hashMapOf(
            "uid" to uid,
            "phone" to phone,
            "provider" to "phone",
            "createdAt" to System.currentTimeMillis()
        )

        val userDoc = firestore.collection("users").document(uid)

        userDoc.get().addOnSuccessListener { snapShot ->
            if(!snapShot.exists()) {
                userDoc.set(userMap)
                    .addOnSuccessListener { navigateHome() }
                    .addOnFailureListener { Toast.makeText(requireContext(),"Failed to save user", Toast.LENGTH_SHORT).show() }
            } else navigateHome()
        }
    }

    private fun navigateHome() {
        Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), HomeActivity::class.java))
    }
}