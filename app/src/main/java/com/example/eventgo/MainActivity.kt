package com.example.eventgo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.eventgo.databinding.ActivityMainBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding =ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        SessionManager.init(this)

        credentialManager = CredentialManager.create(this)
        auth = Firebase.auth

        dbRef = FirebaseDatabase.getInstance()

        registerEvents()
    }

    fun registerEvents() {
        binding.btnLogin.setOnClickListener {
            lifecycleScope.launch {
                val request = prepareRequest()
                loginByGoogle(request)
            }
        }
    }

    fun prepareRequest(): GetCredentialRequest {
        val serverClientId =
            "840274254049-2do4d1isbdg7sqn1lkf4kqtl57b34iht.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption
            .Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()

        val request = GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return request
    }

    suspend fun loginByGoogle(request: GetCredentialRequest) {
        try {
            val result = credentialManager.getCredential(
                context = this,
                request = request
            )

            val credential = result.credential
            val idToken = GoogleIdTokenCredential.createFrom(credential.data)

            firebaseLoginCallback(idToken.idToken)

        } catch (exc: NoCredentialException) {
            Toast.makeText(this, "Login gagal :" + exc.message, Toast.LENGTH_LONG).show()
        } catch (exc: Exception) {
            Toast.makeText(this, "Login gagal :" + exc.message, Toast.LENGTH_LONG).show()
        }
    }

    fun firebaseLoginCallback(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login berhasil", Toast.LENGTH_LONG).show()
                    auth.currentUser?.let {
                        checkUserRoleAndNavigate(it)
                    }
                } else {
                    Toast.makeText(this, "Login gagal", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let {
            checkUserRoleAndNavigate(it)
        }

    }

    private fun checkUserRoleAndNavigate(user: FirebaseUser) {
        val userRef = dbRef.getReference("users").child(user.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val role = snapshot.child("role").getValue(String::class.java) ?: "user"
                    SessionManager.saveUserRole(role)
                    navigateToHome(role)

                } else {
                    val defaultRole = "user"
                    val newUser = mapOf(
                        "email" to user.email,
                        "displayName" to user.displayName,
                        "role" to defaultRole
                    )

                    userRef.setValue(newUser).addOnCompleteListener {
                        SessionManager.saveUserRole(defaultRole)
                        navigateToHome(defaultRole)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal membaca database", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToHome(role: String) {
        val intent = if (role == "admin") {
            // Admin ke HomeActivity (yang ada tombol Tambah)
            Intent(this, HomeActivity::class.java)
        } else {
            // User ke UserHomeActivity (yang TIDAK ada tombol Tambah)
            Intent(this, UserHomeActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}