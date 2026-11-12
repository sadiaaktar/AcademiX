package com.example.academix

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TeacherNoticesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvTeacherName: TextView
    private lateinit var chipContainer: LinearLayout
    private lateinit var noticesContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    private var teacherUid: String = ""
    private var teacherName: String = ""
    private var currentFilter: String = "All"
    private var allNotices: List<Notice> = emptyList()
    private var studentSection: String = ""

    private var listener: ListenerRegistration? = null

    companion object {
        private const val ARG_UID = "teacher_uid"
        private const val ARG_NAME = "teacher_name"

        fun newInstance(uid: String, name: String): TeacherNoticesFragment =
            TeacherNoticesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_UID, uid)
                    putString(ARG_NAME, name)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_teacher_notices, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        teacherUid = arguments?.getString(ARG_UID) ?: ""
        teacherName = arguments?.getString(ARG_NAME) ?: "Teacher"

        tvTeacherName = view.findViewById(R.id.tvTeacherName)
        chipContainer = view.findViewById(R.id.chipContainer)
        noticesContainer = view.findViewById(R.id.noticesContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        tvTeacherName.text = teacherName

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_notice)
        }

        buildFilterChips()
        loadNotices()
        loadStudentSection()

        return view
    }

    private fun loadStudentSection() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                studentSection = doc.getString("section") ?: ""
                renderNotices()
            }
    }

    private fun buildFilterChips() {
        chipContainer.removeAllViews()

        val labels = listOf("All") + NoticeCategories.all

        for (label in labels) {
            val chip = TextView(requireContext())
            chip.text = label
            chip.tag = label
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

            chip.setOnClickListener { selectFilter(label) }
            chipContainer.addView(chip)
        }

        selectFilter("All")
    }

    private fun selectFilter(filter: String) {
        currentFilter = filter

        for (i in 0 until chipContainer.childCount) {
            val chip = chipContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == filter
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

        renderNotices()
    }

    private fun loadNotices() {
        listener = db.collection("notices")
            .whereEqualTo("postedByUid", teacherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                allNotices = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Notice.fromDocument(
                        doc.id,
                        data,
                        doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.timestampMillis }

                renderNotices()
            }
    }

    private fun renderNotices() {
        noticesContainer.removeAllViews()

        // Students only see notices for their own section (or "All").
        val sectionFiltered = allNotices.filter {
            it.section.isBlank() || it.section == "All" || it.section == studentSection
        }

        val filtered = if (currentFilter == "All") {
            sectionFiltered
        } else {
            sectionFiltered.filter { it.category == currentFilter }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No notices"
            return
        }

        tvEmpty.visibility = View.GONE

        for (notice in filtered) {
            val item = layoutInflater.inflate(R.layout.item_notice, noticesContainer, false)

            item.findViewById<TextView>(R.id.tvCategory).apply {
                text = notice.category
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        NoticeCategories.softColorOf(notice.category)
                    )
                )
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        NoticeCategories.colorOf(notice.category)
                    )
                )
            }
            item.findViewById<TextView>(R.id.tvSection).apply {
                text = sectionLabel(notice.section)
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.badge_neutral_bg)
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            item.findViewById<TextView>(R.id.tvTime).text = formatTimestamp(notice.timestampMillis)
            item.findViewById<TextView>(R.id.tvTitle).text = notice.title
            item.findViewById<TextView>(R.id.tvContent).text = notice.content

            val attachmentRow = item.findViewById<View>(R.id.attachmentRow)
            if (notice.hasAttachment()) {
                attachmentRow.visibility = View.VISIBLE
                val ivImage = item.findViewById<ImageView>(R.id.ivAttachmentImage)
                val tvName = item.findViewById<TextView>(R.id.tvAttachmentName)

                if (notice.attachmentType == "image") {
                    val bitmap = AttachmentHelper.decodeBase64(notice.attachmentData)
                    if (bitmap != null) {
                        ivImage.visibility = View.VISIBLE
                        ivImage.setImageBitmap(bitmap)
                        ivImage.setOnClickListener {
                            AttachmentHelper.showImageFullScreen(
                                requireContext(),
                                notice.attachmentData
                            )
                        }
                    }
                    tvName.visibility = View.GONE
                } else {
                    ivImage.visibility = View.GONE
                    tvName.text = notice.attachmentName.ifBlank { "File" }
                    tvName.visibility = View.VISIBLE
                    tvName.setOnClickListener {
                        openAttachment(notice)
                    }
                }
            } else {
                attachmentRow.visibility = View.GONE
            }

            noticesContainer.addView(item)
        }
    }

    private fun openAttachment(notice: Notice) {
        val ok = AttachmentHelper.openFile(
            requireContext(),
            notice.attachmentData,
            notice.attachmentName
        )
        if (!ok) {
            Toast.makeText(requireContext(), "Cannot open attachment", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        listener = null
    }
}
