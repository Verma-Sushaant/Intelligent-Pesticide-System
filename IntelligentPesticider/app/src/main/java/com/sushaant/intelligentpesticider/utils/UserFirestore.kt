package com.sushaant.intelligentpesticider.utils

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object UserFirestore {
    private val db by lazy { FirebaseFirestore.getInstance() }

    fun createUserIfNotExists(
        uid: String,
        email: String? = null,
        phone: String? = null,
        name: String? = null,
        provider: String,
        onComplete: () -> Unit
    ){
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            if(!doc.exists()) {
                val newUser = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "phone" to phone,
                    "name" to (name ?: ""),
                    "provider" to provider,
                    "createdAt" to System.currentTimeMillis()
                )
                userRef.set(newUser).addOnSuccessListener { onComplete() }
            }
            else {
                val updates = hashMapOf<String, Any>()
                if(email != null) updates["email"] = email
                if(phone != null) updates["phone"] = phone
                if(name != null) updates["name"] = name

                if(updates.isNotEmpty()) {
                    userRef.update(updates)
                        .addOnSuccessListener { onComplete() }
                } else onComplete()
            }
        }
    }
}