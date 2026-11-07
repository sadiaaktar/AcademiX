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

class NoticeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvTitle: TextView
    private lateinit var contentContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var fabPost: FloatingActionButton

    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notice, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvTitle = view.findViewById(R.id.tvTitle)
        contentContainer = view.findViewById(R.id.contentContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        fabPost = view.findViewById(R.id.fabPost)

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_home)
        }

        fabPost.setOnClickListener {
            (activity as? MainActivity)?.showPostNotice()
        }

        loadRole()
        return view
    }

    private fun loadRole() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "Student"
                if (role == "Teacher") showTeacherView() else showStudentView()
            }
    }

    private fun showTeacherView() {
        tvTitle.text = "My Notices"
        fabPost.visibility = View.VISIBLE

        val uid = auth.currentUser?.uid ?: return

        listener?.remove()
        listener = db.collection("notices")
            .whereEqualTo("postedByUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val notices = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Notice.fromDocument(
                        doc.id,
                        data,
                        doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.timestampMillis }

                renderNotices(notices)
            }
    }

    private fun showStudentView() {
        tvTitle.text = "Notice Board"
        fabPost.visibility = View.GONE

        listener?.remove()
        listener = db.collection("users")
            .whereEqualTo("role", "Teacher")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                contentContainer.removeAllViews()

                if (snapshot.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No teachers yet"
                    return@addSnapshotListener
                }

                tvEmpty.visibility = View.GONE

                for (doc in snapshot.documents) {
                    val name = doc.getString("fullName") ?: "Teacher"

                    val item = layoutInflater.inflate(
                        R.layout.item_teacher,
                        contentContainer,
                        false
                    )
                    item.findViewById<TextView>(R.id.tvTeacherName).text = name
                    item.findViewById<TextView>(R.id.tvTeacherAvatar).text =
                        name.firstOrNull()?.toString()?.uppercase() ?: "T"

                    item.setOnClickListener {
                        (activity as? MainActivity)?.showTeacherNotices(doc.id, name)
                    }
                    contentContainer.addView(item)
                }
            }
    }

    private fun renderNotices(notices: List<Notice>) {
        contentContainer.removeAllViews()

        if (notices.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No notices yet"
            return
        }

        tvEmpty.visibility = View.GONE

        for (notice in notices) {
            contentContainer.addView(buildNoticeCard(notice))
        }
    }

    private fun buildNoticeCard(notice: Notice): View {
        val item = layoutInflater.inflate(R.layout.item_notice, contentContainer, false)

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

        // Edit / Delete actions (teacher's own notices only)
        item.findViewById<View>(R.id.noticeActions).visibility = View.VISIBLE
        item.findViewById<View>(R.id.btnEdit).setOnClickListener {
            (activity as? MainActivity)?.showEditNotice(notice)
        }
        item.findViewById<View>(R.id.btnDelete).setOnClickListener {
            confirmDelete(notice)
        }

        return item
    }

    private fun confirmDelete(notice: Notice) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete notice")
            .setMessage("Are you sure you want to delete this notice?")
            .setPositiveButton("Delete") { _, _ -> deleteNotice(notice) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteNotice(notice: Notice) {
        db.collection("notices").document(notice.id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Notice deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        listener = null
    }
}
