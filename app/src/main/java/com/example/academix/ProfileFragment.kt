package com.example.academix

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvIntake: TextView
    private lateinit var tvSection: TextView
    private lateinit var tvShift: TextView
    private lateinit var tvBack: TextView
    private lateinit var btnLogout: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvAvatar = view.findViewById(R.id.tvAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvStudentId = view.findViewById(R.id.tvStudentId)
        tvDepartment = view.findViewById(R.id.tvDepartment)
        tvIntake = view.findViewById(R.id.tvIntake)
        tvSection = view.findViewById(R.id.tvSection)
        tvShift = view.findViewById(R.id.tvShift)
        tvBack = view.findViewById(R.id.tvBack)
        btnLogout = view.findViewById(R.id.btnLogout)

        tvBack.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_home)
        }

        loadProfile()

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("fullName") ?: "Student"
                val email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                val studentId = doc.getString("studentId") ?: ""
                val department = doc.getString("department") ?: "CSE"
                val intake = doc.getString("intake") ?: "-"
                val section = doc.getString("section") ?: "-"
                val shift = doc.getString("shift") ?: "-"

                tvName.text = name
                tvEmail.text = email
                tvStudentId.text = studentId
                tvDepartment.text = department
                tvIntake.text = intake
                tvSection.text = section
                tvShift.text = shift
                tvAvatar.text = name.firstOrNull()?.toString()?.uppercase() ?: "S"
            }
    }
}
