package com.example.academix

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CreateAssignmentFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var tvDueDate: TextView
    private lateinit var sectionContainer: LinearLayout
    private lateinit var courseContainer: LinearLayout
    private lateinit var btnCreate: MaterialButton

    private lateinit var attachmentPreview: LinearLayout
    private lateinit var tvAttachmentName: TextView
    private lateinit var btnRemoveAttachment: TextView

    private var selectedSection: String = "All"
    private var selectedCourse: String = ""
    private var dueDateMillis: Long = 0L
    private var posterName: String = ""
    private var attachmentData: String = ""
    private var attachmentName: String = ""
    private var editingAssignmentId: String? = null

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) attachPdf(uri)
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_DUE = "due"
        private const val ARG_SECTION = "section"
        private const val ARG_COURSE = "course"
        private const val ARG_ATTACH_DATA = "attachment_data"
        private const val ARG_ATTACH_NAME = "attachment_name"

        fun editInstance(assignment: Assignment): CreateAssignmentFragment =
            CreateAssignmentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, assignment.id)
                    putString(ARG_TITLE, assignment.title)
                    putString(ARG_DESCRIPTION, assignment.description)
                    putLong(ARG_DUE, assignment.dueDateMillis)
                    putString(ARG_SECTION, assignment.section)
                    putString(ARG_COURSE, assignment.courseCode)
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
        val view = inflater.inflate(R.layout.fragment_create_assignment, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        tvDueDate = view.findViewById(R.id.tvDueDate)
        sectionContainer = view.findViewById(R.id.sectionContainer)
        courseContainer = view.findViewById(R.id.courseContainer)
        btnCreate = view.findViewById(R.id.btnCreate)

        attachmentPreview = view.findViewById(R.id.attachmentPreview)
        tvAttachmentName = view.findViewById(R.id.tvAttachmentName)
        btnRemoveAttachment = view.findViewById(R.id.btnRemoveAttachment)

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_assignment)
        }

        tvDueDate.setOnClickListener { pickDueDate() }

        view.findViewById<View>(R.id.btnAttachPdf).setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }
        btnRemoveAttachment.setOnClickListener {
            attachmentData = ""
            attachmentName = ""
            attachmentPreview.visibility = View.GONE
        }

        readEditArgs()
        buildSectionChips()
        buildCourseChips()
        loadPosterName()

        btnCreate.setOnClickListener {
            saveAssignment()
        }

        return view
    }

    private fun readEditArgs() {
        val args = arguments ?: return
        val id = args.getString(ARG_ID) ?: return

        editingAssignmentId = id
        etTitle.setText(args.getString(ARG_TITLE) ?: "")
        etDescription.setText(args.getString(ARG_DESCRIPTION) ?: "")
        dueDateMillis = args.getLong(ARG_DUE, 0L)
        if (dueDateMillis > 0) tvDueDate.text = formatDueDate(dueDateMillis)
        selectedSection = args.getString(ARG_SECTION) ?: "All"
        selectedCourse = args.getString(ARG_COURSE) ?: ""
        attachmentData = args.getString(ARG_ATTACH_DATA) ?: ""
        attachmentName = args.getString(ARG_ATTACH_NAME) ?: ""
        if (attachmentData.isNotEmpty()) {
            attachmentPreview.visibility = View.VISIBLE
            tvAttachmentName.text = attachmentName
        }
        btnCreate.text = "Update Assignment"
    }

    private fun pickDueDate() {
        val cal = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        val dueCal = Calendar.getInstance()
                        dueCal.set(year, month, day, hour, minute, 0)
                        dueDateMillis = dueCal.timeInMillis
                        tvDueDate.text = formatDueDate(dueDateMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun attachPdf(uri: Uri) {
        val base64 = AttachmentHelper.encodePdfBase64(requireContext(), uri)
        if (base64 == null) {
            Toast.makeText(requireContext(), "PDF too large (max ~700KB)", Toast.LENGTH_LONG).show()
            return
        }

        attachmentData = base64
        attachmentName = queryFileName(uri)

        attachmentPreview.visibility = View.VISIBLE
        tvAttachmentName.text = attachmentName
    }

    private fun queryFileName(uri: Uri): String {
        var name = "question.pdf"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }

    private fun buildSectionChips() {
        sectionContainer.removeAllViews()

        val options = listOf("All") + RoutineCatalog.teacherSections()

        for (option in options) {
            val label = if (option == "All") "All sections" else "Section $option"

            val chip = TextView(requireContext())
            chip.text = label
            chip.tag = option
            chip.textSize = 12f
            chip.gravity = Gravity.CENTER
            chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip)
            chip.setPadding(dp(14), dp(8), dp(14), dp(8))

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            chip.layoutParams = lp

            chip.setOnClickListener { selectSection(option) }
            sectionContainer.addView(chip)
        }

        selectSection(selectedSection)
    }

    private fun selectSection(section: String) {
        selectedSection = section

        for (i in 0 until sectionContainer.childCount) {
            val chip = sectionContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == section
            if (isSelected) {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand_purple)
                )
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    private fun buildCourseChips() {
        courseContainer.removeAllViews()

        val courses = RoutineCatalog.teacherCourseCodes()

        for (code in courses) {
            val label = "$code · ${CourseCatalog.titleOf(code)}"

            val chip = TextView(requireContext())
            chip.text = label
            chip.tag = code
            chip.textSize = 12f
            chip.gravity = Gravity.CENTER
            chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip)
            chip.setPadding(dp(14), dp(8), dp(14), dp(8))

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            chip.layoutParams = lp

            chip.setOnClickListener { selectCourse(code) }
            courseContainer.addView(chip)
        }

        selectCourse(selectedCourse.ifBlank { courses.firstOrNull() ?: "" })
    }

    private fun selectCourse(course: String) {
        selectedCourse = course

        for (i in 0 until courseContainer.childCount) {
            val chip = courseContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == course
            if (isSelected) {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand_purple)
                )
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    private fun loadPosterName() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                posterName = doc.getString("fullName") ?: "Teacher"
            }
    }

    private fun saveAssignment() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Enter a title"
            etTitle.requestFocus()
            return
        }

        if (dueDateMillis <= 0) {
            Toast.makeText(requireContext(), "Pick a due date", Toast.LENGTH_LONG).show()
            return
        }

        if (selectedCourse.isEmpty()) {
            Toast.makeText(requireContext(), "Select a course", Toast.LENGTH_LONG).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        btnCreate.isEnabled = false

        val isEditing = editingAssignmentId != null

        val writeMap = hashMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "dueDateMillis" to dueDateMillis,
            "section" to selectedSection,
            "courseCode" to selectedCourse
        )

        if (attachmentData.isNotEmpty()) {
            writeMap["attachmentData"] = attachmentData
            writeMap["attachmentName"] = attachmentName
        } else if (isEditing) {
            writeMap["attachmentData"] = FieldValue.delete()
            writeMap["attachmentName"] = FieldValue.delete()
        }

        val successMessage = if (isEditing) "Assignment updated" else "Assignment created"

        val writeTask = if (isEditing) {
            db.collection("assignments").document(editingAssignmentId!!).update(writeMap)
        } else {
            writeMap["postedByUid"] = uid
            writeMap["postedByName"] = posterName.ifBlank { "Teacher" }
            writeMap["createdAtMillis"] = System.currentTimeMillis()
            db.collection("assignments").add(writeMap)
        }

        writeTask
            .addOnSuccessListener {
                btnCreate.isEnabled = true
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.selectTab(R.id.nav_assignment)
            }
            .addOnFailureListener { e ->
                btnCreate.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
