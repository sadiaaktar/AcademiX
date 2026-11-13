package com.example.academix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SubmissionsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore

    private lateinit var tvTitle: TextView
    private lateinit var submissionsContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    private var assignmentId: String = ""

    private var listener: ListenerRegistration? = null

    companion object {
        private const val ARG_ID = "assignment_id"
        private const val ARG_TITLE = "assignment_title"

        fun newInstance(assignment: Assignment): SubmissionsFragment =
            SubmissionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, assignment.id)
                    putString(ARG_TITLE, assignment.title)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_submissions, container, false)

        db = FirebaseFirestore.getInstance()

        assignmentId = arguments?.getString(ARG_ID) ?: ""
        val assignmentTitle = arguments?.getString(ARG_TITLE) ?: "Submissions"

        tvTitle = view.findViewById(R.id.tvTitle)
        submissionsContainer = view.findViewById(R.id.submissionsContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        tvTitle.text = assignmentTitle

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_assignment)
        }

        loadSubmissions()

        return view
    }

    private fun loadSubmissions() {
        listener = db.collection("assignments").document(assignmentId)
            .collection("submissions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val submissions = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Submission.fromDocument(data)
                }.sortedByDescending { it.submittedAtMillis }

                renderSubmissions(submissions)
            }
    }

    private fun renderSubmissions(submissions: List<Submission>) {
        submissionsContainer.removeAllViews()

        if (submissions.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        tvEmpty.visibility = View.GONE

        for (submission in submissions) {
            val item = layoutInflater.inflate(
                R.layout.item_submission,
                submissionsContainer,
                false
            )
            item.findViewById<TextView>(R.id.tvStudentAvatar).text =
                submission.studentName.firstOrNull()?.toString()?.uppercase() ?: "S"
            item.findViewById<TextView>(R.id.tvStudentName).text = submission.studentName
            item.findViewById<TextView>(R.id.tvSubmittedAt).text =
                "Submitted: ${formatDueDate(submission.submittedAtMillis)}"
            item.findViewById<View>(R.id.btnViewSubmission).setOnClickListener {
                val ok = AttachmentHelper.openFile(
                    requireContext(),
                    submission.pdfData,
                    "submission.pdf"
                )
                if (!ok) {
                    Toast.makeText(requireContext(), "Cannot open file", Toast.LENGTH_SHORT).show()
                }
            }
            submissionsContainer.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        listener = null
    }
}
