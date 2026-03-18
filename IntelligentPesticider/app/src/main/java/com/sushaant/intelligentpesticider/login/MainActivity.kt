package com.sushaant.intelligentpesticider.login

import LoginPagerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.sushaant.intelligentpesticider.home.HomeActivity
import com.sushaant.intelligentpesticider.R
import com.sushaant.intelligentpesticider.utils.UserFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var blurOverlay: View
    private lateinit var progressBar: View
    private lateinit var googleBtn: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private val GOOGLE_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        blurOverlay = findViewById(R.id.blurOverlay)
        progressBar = findViewById(R.id.loadingProgress)
        googleBtn = findViewById(R.id.btnGoogle)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        val fragments = listOf(
            EmailLoginFragment(),
            PhoneLoginFragment()
        )

        val adapter = LoginPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = if (pos == 0) "Email" else "Phone"
        }.attach()

        googleBtn.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onStart() {
        super.onStart()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun signInWithGoogle() {
        lifecycleScope.launch {
            try {
                showLoading()

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // 🚀 New API (suspend)
                val result = credentialManager.getCredential(this@MainActivity,request)

                onGoogleCredentialReceived(result.credential)

            } catch (e: Exception) {
                hideLoading()
                Toast.makeText(
                    this@MainActivity,
                    "Google Sign-in failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun onGoogleCredentialReceived(credential: Credential) {
        try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleCredential.idToken)
        } catch (e: Exception) {
            hideLoading()
            Toast.makeText(this, "Invalid Google Credential", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task ->
                hideLoading()
                if(task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    UserFirestore.createUserIfNotExists(
                        uid = uid,
                        provider = "google"
                    ) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                } else Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
    }
    fun showLoading() {
        blurOverlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }
    fun hideLoading() {
        blurOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
    }
}