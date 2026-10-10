package com.example.academix

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    private lateinit var rgRole: RadioGroup
    private lateinit var coursesSection: LinearLayout
    private lateinit var coursesContainer: LinearLayout

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

        rgRole = findViewById(R.id.rgRole)
        coursesSection = findViewById(R.id.coursesSection)
        coursesContainer = findViewById(R.id.coursesContainer)

        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        tvBack = findViewById(R.id.tvBack)

        buildCourseCheckboxes()

        rgRole.setOnCheckedChangeListener { _, checkedId ->
            val isStudent = checkedId == R.id.rbStudent
            coursesSection.visibility = if (isStudent) View.VISIBLE else View.GONE
        }

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

    private fun buildCourseCheckboxes() {
        coursesContainer.removeAllViews()

        val tint = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.brand_purple)
        )
        val textColor = ContextCompat.getColor(this, R.color.text_primary)

        for (course in CourseCatalog.courses) {
            val checkbox = CheckBox(this)
            checkbox.text = "${course.courseCode} · ${course.title}"
            checkbox.tag = course.courseCode
            checkbox.textSize = 14f
            checkbox.setTextColor(textColor)
            checkbox.buttonTintList = tint
            checkbox.setPadding(0, dp(4), 0, dp(4))
            coursesContainer.addView(checkbox)
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

        val role = when (rgRole.checkedRadioButtonId) {
            R.id.rbTeacher -> "Teacher"
            R.id.rbTA -> "TA"
            else -> "Student"
        }

        // -----------------------------
        // Validate Full Name
        // -----------------------------

        if (fullName.isEmpty()) {
            etFullName.error = "Enter your full name"
            etFullName.requestFocus()
            return
        }

        // -----------------------------
        // Validate Email (depends on role)
        // -----------------------------

        if (email.isEmpty()) {
            etEmail.error = "Enter your university email"
            etEmail.requestFocus()
            return
        }

        if (!isValidEmailForRole(email, role)) {
            etEmail.error = when (role) {
                "Teacher" -> "Use your faculty email, e.g. asifur@bubt.edu.bd"
                else -> "Use your student email, e.g. 20245103181@cse.bubt.edu.bd"
            }
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

        // -----------------------------
        // Validate Selected Courses (students only)
        // -----------------------------

        val selectedCourses = mutableListOf<String>()

        if (role == "Student") {
            for (i in 0 until coursesContainer.childCount) {
                val checkbox = coursesContainer.getChildAt(i) as? CheckBox ?: continue
                if (checkbox.isChecked) {
                    selectedCourses.add(checkbox.tag as String)
                }
            }

            if (selectedCourses.isEmpty()) {
                Toast.makeText(
                    this,
                    "Select at least one course",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
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
                                    .putString("role", role)
                                    .putStringSet("courses", selectedCourses.toSet())
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

    private fun isValidEmailForRole(email: String, role: String): Boolean {
        return when (role) {
            "Teacher" -> Regex("^[a-zA-Z][a-zA-Z0-9._-]*@bubt\\.edu\\.bd$").matches(email)
            else -> Regex("^[0-9]{11}@cse\\.bubt\\.edu\\.bd$").matches(email)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
