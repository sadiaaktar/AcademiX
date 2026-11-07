package com.example.academix

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostNoticeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: TextInputEditText
    private lateinit var categoryContainer: LinearLayout
    private lateinit var sectionContainer: LinearLayout
    private lateinit var btnPost: MaterialButton

    private var selectedCategory: String = NoticeCategories.GENERAL
    private var selectedSection: String = "All"
    private var posterName: String = ""
    private var editingNoticeId: String? = null

    companion object {
        private const val ARG_NOTICE_ID = "notice_id"
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"
        private const val ARG_CATEGORY = "category"
        private const val ARG_SECTION = "section"

        fun editInstance(notice: Notice): PostNoticeFragment =
            PostNoticeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTICE_ID, notice.id)
                    putString(ARG_TITLE, notice.title)
                    putString(ARG_CONTENT, notice.content)
                    putString(ARG_CATEGORY, notice.category)
                    putString(ARG_SECTION, notice.section)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_post_notice, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        sectionContainer = view.findViewById(R.id.sectionContainer)
        btnPost = view.findViewById(R.id.btnPost)

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_notice)
        }

        readEditArgs()
        buildCategoryChips()
        buildSectionChips()
        loadPosterName()

        btnPost.setOnClickListener {
            postNotice()
        }

        return view
    }

    private fun readEditArgs() {
        val args = arguments ?: return
        val noticeId = args.getString(ARG_NOTICE_ID) ?: return

        editingNoticeId = noticeId
        etTitle.setText(args.getString(ARG_TITLE) ?: "")
        etContent.setText(args.getString(ARG_CONTENT) ?: "")
        selectedCategory = args.getString(ARG_CATEGORY) ?: NoticeCategories.GENERAL
        selectedSection = args.getString(ARG_SECTION) ?: "All"
        btnPost.text = "Update Notice"
    }

    private fun buildCategoryChips() {
        categoryContainer.removeAllViews()

        for (category in NoticeCategories.all) {
            val chip = TextView(requireContext())
            chip.text = category
            chip.tag = category
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

            chip.setOnClickListener { selectCategory(category) }
            categoryContainer.addView(chip)
        }

        selectCategory(selectedCategory)
    }

    private fun selectCategory(category: String) {
        selectedCategory = category

        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == category
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

    private fun loadPosterName() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                posterName = doc.getString("fullName") ?: "Teacher"
            }
    }

    private fun postNotice() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Enter a title"
            etTitle.requestFocus()
            return
        }

        if (content.isEmpty()) {
            etContent.error = "Enter a description"
            etContent.requestFocus()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        btnPost.isEnabled = false

        val notice = hashMapOf<String, Any>(
            "title" to title,
            "content" to content,
            "category" to selectedCategory,
            "section" to selectedSection,
            "postedByUid" to uid,
            "postedByName" to posterName.ifBlank { "Teacher" },
            "timestamp" to FieldValue.serverTimestamp()
        )

        val isEditing = editingNoticeId != null
        val successMessage = if (isEditing) "Notice updated" else "Notice posted"

        val writeTask = if (isEditing) {
            db.collection("notices").document(editingNoticeId!!).set(notice)
        } else {
            db.collection("notices").add(notice)
        }

        writeTask
            .addOnSuccessListener {
                btnPost.isEnabled = true
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.selectTab(R.id.nav_notice)
            }
            .addOnFailureListener { e ->
                btnPost.isEnabled = true
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
