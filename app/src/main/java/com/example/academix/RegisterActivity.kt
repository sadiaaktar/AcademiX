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
import com.google.firebase.firestore.SetOptions

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etIntake: EditText
    private lateinit var etSection: EditText
    private lateinit var etShift: EditText

    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var tvBack: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etIntake = findViewById(R.id.etIntake)
        etSection = findViewById(R.id.etSection)
        etShift = findViewById(R.id.etShift)

        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        tvBack = findViewById(R.id.tvBack)

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            finish()
        }

        tvBack.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {

        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val intake = etIntake.text.toString().trim()
        val section = etSection.text.toString().trim()
        val shift = etShift.text.toString().trim()

        // -----------------------------
        // Validate Full Name
        // -----------------------------

        if (fullName.isEmpty()) {
            etFullName.error = "Enter your full name"
            etFullName.requestFocus()
            return
        }

        // -----------------------------
        // Validate Email
        // -----------------------------

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

        // -----------------------------
        // Validate Password
        // -----------------------------

        if (password.isEmpty()) {
            etPassword.error = "Enter a password"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must contain at least 6 characters"
            etPassword.requestFocus()
            return
        }

        // -----------------------------
        // Validate Confirm Password
        // -----------------------------

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Confirm your password"
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        // -----------------------------
        // Validate Intake
        // -----------------------------

        if (intake.isEmpty()) {
            etIntake.error = "Enter your intake"
            etIntake.requestFocus()
            return
        }

        // -----------------------------
        // Validate Section
        // -----------------------------

        if (section.isEmpty()) {
            etSection.error = "Enter your section"
            etSection.requestFocus()
            return
        }

        // -----------------------------
        // Validate Shift
        // -----------------------------

        if (shift.isEmpty()) {
            etShift.error = "Enter your shift"
            etShift.requestFocus()
            return
        }

        // Extract information from student email
        val studentId = email.substringBefore("@")
        val department = "CSE"

        btnRegister.isEnabled = false

        // -----------------------------
        // Create Firebase Auth Account
        // -----------------------------

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user == null) {
                        btnRegister.isEnabled = true

                        Toast.makeText(
                            this,
                            "Unable to create account",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    // -----------------------------
                    // Send Email Verification
                    // -----------------------------

                    user.sendEmailVerification()
                        .addOnCompleteListener { verificationTask ->

                            if (verificationTask.isSuccessful) {

                                Toast.makeText(
                                    this,
                                    "Verification email sent. Please verify your email before logging in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                /*
                                 * We temporarily keep the registration
                                 * information locally.
                                 *
                                 * After the user verifies the email,
                                 * LoginActivity will create/update
                                 * the Firestore profile.
                                 */

                                val preferences =
                                    getSharedPreferences(
                                        "academix_registration",
                                        MODE_PRIVATE
                                    )

                                preferences.edit()
                                    .putString("fullName", fullName)
                                    .putString("email", email)
                                    .putString("studentId", studentId)
                                    .putString("department", department)
                                    .putString("intake", intake)
                                    .putString("section", section)
                                    .putString("shift", shift)
                                    .apply()

                                auth.signOut()

                                val intent = Intent(
                                    this,
                                    LoginActivity::class.java
                                )

                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                                startActivity(intent)
                                finish()

                            } else {

                                btnRegister.isEnabled = true

                                Toast.makeText(
                                    this,
                                    verificationTask.exception?.message
                                        ?: "Could not send verification email",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                } else {

                    btnRegister.isEnabled = true

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration failed",
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