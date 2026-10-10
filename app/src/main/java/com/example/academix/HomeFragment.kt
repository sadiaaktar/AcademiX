package com.example.academix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var scrollHome: ScrollView
    private lateinit var tvGreeting: TextView
    private lateinit var tvName: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var coursesContainer: LinearLayout
    private lateinit var tvCoursesEmpty: TextView
    private lateinit var tvCoursesHeader: TextView
    private lateinit var todayClassesContainer: LinearLayout
    private lateinit var tvTodayEmpty: TextView
    private lateinit var tvTodayLabel: TextView

    private var profileListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        scrollHome = view.findViewById(R.id.scrollHome)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvName = view.findViewById(R.id.tvName)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        tvAvatar = view.findViewById(R.id.tvAvatar)
        coursesContainer = view.findViewById(R.id.coursesContainer)
        tvCoursesEmpty = view.findViewById(R.id.tvCoursesEmpty)
        tvCoursesHeader = view.findViewById(R.id.tvCoursesHeader)
        todayClassesContainer = view.findViewById(R.id.todayClassesContainer)
        tvTodayEmpty = view.findViewById(R.id.tvTodayEmpty)
        tvTodayLabel = view.findViewById(R.id.tvTodayLabel)

        renderTodayClasses()

        view.findViewById<View>(R.id.ivNotification).setOnClickListener {
            Toast.makeText(
                requireContext(),
                "No new notifications",
                Toast.LENGTH_SHORT
            ).show()
        }

        setupQuickActions(view)
        loadUser()

        return view
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<View>(R.id.cardNotice).setOnClickListener {
            openTab(R.id.nav_notice)
        }
        view.findViewById<View>(R.id.cardAssignment).setOnClickListener {
            openTab(R.id.nav_assignment)
        }
        view.findViewById<View>(R.id.cardRoutine).setOnClickListener {
            (activity as? MainActivity)?.showRoutine()
        }
        view.findViewById<View>(R.id.cardCourses).setOnClickListener {
            scrollHome.smoothScrollTo(0, tvCoursesHeader.top)
        }
        view.findViewById<View>(R.id.cardHelp).setOnClickListener {
            comingSoon("Help Request")
        }
        view.findViewById<View>(R.id.cardMessages).setOnClickListener {
            comingSoon("Messages")
        }
    }

    private fun openTab(itemId: Int) {
        (activity as? MainActivity)?.selectTab(itemId)
    }

    private fun comingSoon(feature: String) {
        Toast.makeText(requireContext(), "$feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return

        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null) return@addSnapshotListener
                updateHeader(doc)
                updateCourses(doc)
            }
    }

    private fun updateHeader(doc: DocumentSnapshot) {
        val name = doc.getString("fullName") ?: "Student"
        val department = doc.getString("department") ?: "CSE"
        val intake = doc.getString("intake") ?: ""
        val section = doc.getString("section") ?: ""

        val firstName = name.split(" ").firstOrNull() ?: name

        tvName.text = name
        tvAvatar.text = firstName.firstOrNull()?.toString()?.uppercase() ?: "S"

        val parts = mutableListOf(department)
        if (intake.isNotBlank()) parts.add("Intake $intake")
        if (section.isNotBlank()) parts.add("Section $section")
        tvSubtitle.text = parts.joinToString(" · ")
    }

    private fun updateCourses(doc: DocumentSnapshot) {
        val courseCodes = doc.get("courses") as? List<*> ?: emptyList<Any>()

        coursesContainer.removeAllViews()

        if (courseCodes.isEmpty()) {
            tvCoursesEmpty.visibility = View.VISIBLE
            return
        }

        tvCoursesEmpty.visibility = View.GONE

        for (code in courseCodes) {
            val codeStr = code.toString()
            val title = CourseCatalog.titleOf(codeStr)

            val item = layoutInflater.inflate(
                R.layout.item_course,
                coursesContainer,
                false
            )
            item.findViewById<TextView>(R.id.tvCourseCode).text = codeStr
            item.findViewById<TextView>(R.id.tvCourseTitle).text = title
            coursesContainer.addView(item)
        }
    }

    private fun renderTodayClasses() {
        val today = RoutineCatalog.today()
        val schedule = RoutineCatalog.scheduleFor(today)

        tvTodayLabel.text = RoutineCatalog.todayName()

        todayClassesContainer.removeAllViews()

        if (schedule.isEmpty()) {
            tvTodayEmpty.visibility = View.VISIBLE
            return
        }

        tvTodayEmpty.visibility = View.GONE

        for (entry in schedule) {
            val item = layoutInflater.inflate(
                R.layout.item_routine,
                todayClassesContainer,
                false
            )
            item.findViewById<TextView>(R.id.tvStartTime).text = entry.startTime
            item.findViewById<TextView>(R.id.tvEndTime).text = entry.endTime
            item.findViewById<TextView>(R.id.tvCourseCode).text = entry.courseCode
            item.findViewById<TextView>(R.id.tvCourseTitle).text =
                CourseCatalog.titleOf(entry.courseCode)
            item.findViewById<TextView>(R.id.tvFacultyRoom).text =
                "FC: ${entry.faculty}  ·  R: ${entry.room}"
            todayClassesContainer.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileListener?.remove()
        profileListener = null
    }
}
