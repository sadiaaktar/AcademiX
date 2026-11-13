package com.example.academix

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AssignmentFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvTitle: TextView
    private lateinit var contentContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var fabCreate: FloatingActionButton

    private var listener: ListenerRegistration? = null
    private var currentRole: String = "Student"
    private var studentSection: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_assignment, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvTitle = view.findViewById(R.id.tvTitle)
        contentContainer = view.findViewById(R.id.contentContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        fabCreate = view.findViewById(R.id.fabCreate)

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_home)
        }

        fabCreate.setOnClickListener {
            (activity as? MainActivity)?.showCreateAssignment()
        }

        loadUser()
        return view
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentRole = doc.getString("role") ?: "Student"
                studentSection = doc.getString("section") ?: ""

                if (currentRole == "Teacher") showTeacherView() else showStudentView()
            }
    }

    private fun showTeacherView() {
        tvTitle.text = "My Assignments"
        fabCreate.visibility = View.VISIBLE

        val uid = auth.currentUser?.uid ?: return

        listener?.remove()
        listener = db.collection("assignments")
            .whereEqualTo("postedByUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val assignments = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Assignment.fromDocument(doc.id, data)
                }.sortedByDescending { it.createdAtMillis }

                renderTeacherAssignments(assignments)
            }
    }

    private fun showStudentView() {
        tvTitle.text = "Assignments"
        fabCreate.visibility = View.GONE

        listener?.remove()
        listener = db.collection("assignments")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val all = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Assignment.fromDocument(doc.id, data)
                }

                val assignments = all.filter {
                    it.section.isBlank() || it.section == "All" || it.section == studentSection
                }.sortedByDescending { it.createdAtMillis }

                renderStudentAssignments(assignments)
            }
    }

    private fun renderTeacherAssignments(assignments: List<Assignment>) {
        contentContainer.removeAllViews()

        if (assignments.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No assignments yet"
            return
        }

        tvEmpty.visibility = View.GONE

        for (assignment in assignments) {
            val item = layoutInflater.inflate(R.layout.item_assignment, contentContainer, false)
            bindAssignmentHeader(item, assignment)
            item.findViewById<TextView>(R.id.tvPostedBy).visibility = View.GONE
            item.findViewById<TextView>(R.id.tvStatus).visibility = View.GONE
            item.findViewById<TextView>(R.id.tvSubmittedFile).visibility = View.GONE
            item.findViewById<TextView>(R.id.tvAction).text = "View submissions"
            item.findViewById<View>(R.id.tvAction).setOnClickListener {
                (activity as? MainActivity)?.showSubmissions(assignment)
            }
            item.findViewById<View>(R.id.assignmentActions).visibility = View.VISIBLE
            item.findViewById<View>(R.id.btnEdit).setOnClickListener {
                (activity as? MainActivity)?.showEditAssignment(assignment)
            }
            item.findViewById<View>(R.id.btnDelete).setOnClickListener {
                confirmDelete(assignment)
            }
            contentContainer.addView(item)
        }
    }

    private fun confirmDelete(assignment: Assignment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete assignment")
            .setMessage("Are you sure you want to delete this assignment?")
            .setPositiveButton("Delete") { _, _ -> deleteAssignment(assignment) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAssignment(assignment: Assignment) {
        db.collection("assignments").document(assignment.id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Assignment deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun renderStudentAssignments(assignments: List<Assignment>) {
        contentContainer.removeAllViews()

        if (assignments.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No assignments"
            return
        }

        tvEmpty.visibility = View.GONE

        for (assignment in assignments) {
            val item = layoutInflater.inflate(
                R.layout.item_assignment_list,
                contentContainer,
                false
            )

            item.findViewById<TextView>(R.id.tvCourse).text =
                "${assignment.courseCode} · ${CourseCatalog.shortTitleOf(assignment.courseCode)}"
            item.findViewById<TextView>(R.id.tvSection).text = sectionLabel(assignment.section)
            item.findViewById<TextView>(R.id.tvSection).apply {
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.badge_neutral_bg)
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            item.findViewById<TextView>(R.id.tvDueDate).text =
                "Due: ${formatDueDate(assignment.dueDateMillis)}"
            item.findViewById<TextView>(R.id.tvPostedBy).text =
                "Posted by ${assignment.postedByName.ifBlank { "Teacher" }}"

            item.setOnClickListener {
                (activity as? MainActivity)?.showAssignmentDetail(assignment)
            }

            contentContainer.addView(item)
        }
    }

    private fun bindAssignmentHeader(item: View, assignment: Assignment) {
        val tvCourse = item.findViewById<TextView>(R.id.tvCourse)
        if (assignment.courseCode.isNotBlank()) {
            tvCourse.visibility = View.VISIBLE
            tvCourse.text =
                "${assignment.courseCode} · ${CourseCatalog.shortTitleOf(assignment.courseCode)}"
        } else {
            tvCourse.visibility = View.GONE
        }

        item.findViewById<TextView>(R.id.tvSection).apply {
            text = sectionLabel(assignment.section)
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.badge_neutral_bg)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
        item.findViewById<TextView>(R.id.tvDueDate).text =
            "Due: ${formatDueDate(assignment.dueDateMillis)}"
        item.findViewById<TextView>(R.id.tvTitle).text = assignment.title
        item.findViewById<TextView>(R.id.tvDescription).text = assignment.description
        item.findViewById<TextView>(R.id.tvPostedBy).text =
            "Posted by ${assignment.postedByName.ifBlank { "Teacher" }}"

        val tvAttachment = item.findViewById<TextView>(R.id.tvAttachment)
        if (assignment.hasAttachment()) {
            tvAttachment.visibility = View.VISIBLE
            tvAttachment.text = assignment.attachmentName.ifBlank { "Question PDF" }
            tvAttachment.setOnClickListener {
                val ok = AttachmentHelper.openFile(
                    requireContext(),
                    assignment.attachmentData,
                    assignment.attachmentName.ifBlank { "question.pdf" }
                )
                if (!ok) {
                    Toast.makeText(requireContext(), "Cannot open file", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            tvAttachment.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        listener = null
    }
}
