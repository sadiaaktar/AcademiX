package com.example.academix

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AssignmentDetailFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvCourse: TextView
    private lateinit var tvSection: TextView
    private lateinit var tvPostedBy: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAttachment: TextView

    private lateinit var tvWorkStatus: TextView
    private lateinit var tvSubmittedFile: TextView
    private lateinit var tvAction: TextView

    private var assignment: Assignment? = null
    private var studentName: String = ""

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) confirmSubmit(uri)
    }

    companion object {
        private const val ARG_ID = "assignment_id"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_DUE = "due_date"
        private const val ARG_SECTION = "section"
        private const val ARG_COURSE = "course"
        private const val ARG_POSTED_BY = "posted_by"
        private const val ARG_ATTACH_DATA = "attachment_data"
        private const val ARG_ATTACH_NAME = "attachment_name"

        fun newInstance(assignment: Assignment): AssignmentDetailFragment =
            AssignmentDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, assignment.id)
                    putString(ARG_TITLE, assignment.title)
                    putString(ARG_DESCRIPTION, assignment.description)
                    putLong(ARG_DUE, assignment.dueDateMillis)
                    putString(ARG_SECTION, assignment.section)
                    putString(ARG_COURSE, assignment.courseCode)
                    putString(ARG_POSTED_BY, assignment.postedByName)
                    putString(ARG_ATTACH_DATA, assignment.attachmentData)
                    putString(ARG_ATTACH_NAME, assignment.attachmentName)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_assignment_detail, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvCourse = view.findViewById(R.id.tvCourse)
        tvSection = view.findViewById(R.id.tvSection)
        tvPostedBy = view.findViewById(R.id.tvPostedBy)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvDueDate = view.findViewById(R.id.tvDueDate)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvAttachment = view.findViewById(R.id.tvAttachment)

        tvWorkStatus = view.findViewById(R.id.tvWorkStatus)
        tvSubmittedFile = view.findViewById(R.id.tvSubmittedFile)
        tvAction = view.findViewById(R.id.tvAction)

        readArgs()

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_assignment)
        }

        loadStudentName()
        loadSubmissionStatus()

        return view
    }

    private fun readArgs() {
        val args = arguments ?: return
        assignment = Assignment(
            id = args.getString(ARG_ID) ?: "",
            title = args.getString(ARG_TITLE) ?: "",
            description = args.getString(ARG_DESCRIPTION) ?: "",
            dueDateMillis = args.getLong(ARG_DUE, 0L),
            section = args.getString(ARG_SECTION) ?: "All",
            courseCode = args.getString(ARG_COURSE) ?: "",
            postedByName = args.getString(ARG_POSTED_BY) ?: "",
            attachmentData = args.getString(ARG_ATTACH_DATA) ?: "",
            attachmentName = args.getString(ARG_ATTACH_NAME) ?: ""
        )

        val a = assignment ?: return

        tvCourse.text =
            "${a.courseCode} · ${CourseCatalog.shortTitleOf(a.courseCode)}"
        tvSection.text = sectionLabel(a.section)
        tvSection.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.badge_neutral_bg)
        )
        tvSection.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        tvPostedBy.text = "Posted by ${a.postedByName.ifBlank { "Teacher" }}"
        tvTitle.text = a.title
        tvDueDate.text = "Due: ${formatDueDate(a.dueDateMillis)}"
        tvDescription.text = a.description

        if (a.hasAttachment()) {
            tvAttachment.visibility = View.VISIBLE
            tvAttachment.text = a.attachmentName.ifBlank { "Question PDF" }
            tvAttachment.setOnClickListener {
                val ok = AttachmentHelper.openFile(
                    requireContext(),
                    a.attachmentData,
                    a.attachmentName.ifBlank { "question.pdf" }
                )
                if (!ok) {
                    Toast.makeText(requireContext(), "Cannot open file", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            tvAttachment.visibility = View.GONE
        }
    }

    private fun loadStudentName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                studentName = doc.getString("fullName") ?: "Student"
            }
    }

    private fun loadSubmissionStatus() {
        val a = assignment ?: return
        val uid = auth.currentUser?.uid ?: return

        db.collection("assignments").document(a.id)
            .collection("submissions").document(uid)
            .get()
            .addOnSuccessListener { subDoc ->
                val now = System.currentTimeMillis()
                val isPastDue = a.dueDateMillis > 0 && now > a.dueDateMillis

                if (subDoc.exists()) {
                    val submittedAt = subDoc.getLong("submittedAtMillis") ?: 0L
                    val isLate = a.dueDateMillis > 0 && submittedAt > a.dueDateMillis

                    if (isLate) {
                        setWorkStatus("Late", R.color.accent_courses_soft, R.color.accent_courses)
                    } else {
                        setWorkStatus("Submitted", R.color.accent_routine_soft, R.color.accent_routine)
                    }

                    val fileName = subDoc.getString("fileName") ?: "submission.pdf"
                    val pdfData = subDoc.getString("pdfData") ?: ""
                    tvSubmittedFile.visibility = View.VISIBLE
                    tvSubmittedFile.text = fileName
                    tvSubmittedFile.setOnClickListener {
                        val ok = AttachmentHelper.openFile(requireContext(), pdfData, fileName)
                        if (!ok) {
                            Toast.makeText(
                                requireContext(),
                                "Cannot open file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    tvAction.text = "Resubmit PDF"
                } else {
                    if (isPastDue) {
                        setWorkStatus("Missing", R.color.accent_help_soft, R.color.accent_help)
                    } else {
                        setWorkStatus("Assigned", R.color.badge_neutral_bg, R.color.text_secondary)
                    }
                    tvSubmittedFile.visibility = View.GONE
                    tvAction.text = "Submit PDF"
                }

                tvAction.setOnClickListener {
                    pickPdfLauncher.launch(arrayOf("application/pdf"))
                }
            }
    }

    private fun setWorkStatus(text: String, bgRes: Int, textRes: Int) {
        tvWorkStatus.text = text
        tvWorkStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), bgRes)
        )
        tvWorkStatus.setTextColor(ContextCompat.getColor(requireContext(), textRes))
    }

    private fun confirmSubmit(uri: Uri) {
        val fileName = queryFileName(uri)

        AlertDialog.Builder(requireContext())
            .setTitle("Submit assignment")
            .setMessage("Submit \"$fileName\"?")
            .setPositiveButton("Submit") { _, _ -> submitPdf(uri, fileName) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitPdf(uri: Uri, fileName: String) {
        val a = assignment ?: return

        val base64 = encodePdf(uri) ?: return
        val uid = auth.currentUser?.uid ?: return

        val submission = hashMapOf<String, Any>(
            "studentUid" to uid,
            "studentName" to studentName.ifBlank { "Student" },
            "pdfData" to base64,
            "fileName" to fileName,
            "submittedAtMillis" to System.currentTimeMillis()
        )

        db.collection("assignments").document(a.id)
            .collection("submissions").document(uid)
            .set(submission)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Submitted", Toast.LENGTH_SHORT).show()
                loadSubmissionStatus()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Submit failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun encodePdf(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.size > 700 * 1024) {
                Toast.makeText(
                    requireContext(),
                    "PDF too large (max ~700KB)",
                    Toast.LENGTH_LONG
                ).show()
                return null
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun queryFileName(uri: Uri): String {
        var name = "submission.pdf"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }
}
