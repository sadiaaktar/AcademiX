package com.example.academix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnCreateAccount: Button
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Already signed in? Skip the login screen and open the dashboard.
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            loginUser()
        }

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Enter your university email"
            etEmail.requestFocus()
            return
        }

        if (!isValidStudentEmail(email)) {
            etEmail.error =
                "Use your student email, for example 20245103181@cse.bubt.edu.bd"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Enter your password"
            etPassword.requestFocus()
            return
        }

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user == null) {
                        btnLogin.isEnabled = true

                        Toast.makeText(
                            this,
                            "Login failed",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    user.reload().addOnCompleteListener {

                        if (!user.isEmailVerified) {

                            btnLogin.isEnabled = true

                            Toast.makeText(
                                this,
                                "Please verify your university email before logging in.",
                                Toast.LENGTH_LONG
                            ).show()

                            auth.signOut()

                            return@addOnCompleteListener
                        }

                        createFirestoreProfile(user.uid, email)
                    }

                } else {

                    btnLogin.isEnabled = true

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun createFirestoreProfile(
        userId: String,
        email: String
    ) {

        val preferences = getSharedPreferences(
            "academix_registration",
            MODE_PRIVATE
        )

        val fullName = preferences.getString("fullName", "") ?: ""

        // Returning users (profile already exists in Firestore) have cleared
        // prefs, so fullName is empty. Skip overwriting their existing profile.
        if (fullName.isEmpty()) {
            openMainActivity()
            return
        }

        val studentId = preferences.getString("studentId", "") ?: ""
        val department = preferences.getString("department", "CSE") ?: "CSE"
        val intake = preferences.getString("intake", "") ?: ""
        val section = preferences.getString("section", "") ?: ""
        val shift = preferences.getString("shift", "") ?: ""
        val role = preferences.getString("role", "Student") ?: "Student"
        val courses = preferences.getStringSet("courses", emptySet())
            ?.toList() ?: emptyList()

        val userData = hashMapOf<String, Any>(
            "fullName" to fullName,
            "email" to email,
            "studentId" to studentId,
            "department" to department,
            "intake" to intake,
            "section" to section,
            "shift" to shift,
            "role" to role,
            "courses" to courses,
            "emailVerified" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                preferences.edit().clear().apply()
                openMainActivity()
            }
            .addOnFailureListener { exception ->

                btnLogin.isEnabled = true

                Toast.makeText(
                    this,
                    "Profile creation failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun openMainActivity() {
        btnLogin.isEnabled = true
        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun resetPassword() {

        val email = etEmail.text.toString().trim().lowercase()

        if (email.isEmpty()) {
            etEmail.error = "Enter your university email first"
            etEmail.requestFocus()
            return
        }

        if (!isValidStudentEmail(email)) {
            etEmail.error =
                "Use your student email, for example 20245103181@cse.bubt.edu.bd"
            etEmail.requestFocus()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Password reset email sent",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Unable to send reset email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun isValidStudentEmail(email: String): Boolean {

        val studentEmailPattern =
            Regex("^[0-9]{11}@cse\\.bubt\\.edu\\.bd$")

        return studentEmailPattern.matches(email)
    }
}