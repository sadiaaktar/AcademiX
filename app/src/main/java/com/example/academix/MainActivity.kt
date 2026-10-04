package com.example.academix

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        tvWelcome = findViewById(R.id.tvWelcome)
        btnLogout = findViewById(R.id.btnLogout)

        val user = auth.currentUser

        tvWelcome.text = "Welcome to AcademiX\n\nLogin Successful!\n\n${user?.email}"

        btnLogout.setOnClickListener {
            auth.signOut()
            finish()
        }
    }
}