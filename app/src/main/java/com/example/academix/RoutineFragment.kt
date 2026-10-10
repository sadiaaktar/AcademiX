package com.example.academix

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class RoutineFragment : Fragment() {

    private lateinit var dayContainer: LinearLayout
    private lateinit var classesContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    private var selectedDay: String = RoutineCatalog.today()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_routine, container, false)

        dayContainer = view.findViewById(R.id.dayContainer)
        classesContainer = view.findViewById(R.id.classesContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_home)
        }

        buildDaySelector()
        selectDay(selectedDay)

        return view
    }

    private fun buildDaySelector() {
        dayContainer.removeAllViews()

        for (day in RoutineCatalog.days) {
            val chip = TextView(requireContext())
            chip.text = day
            chip.tag = day
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

            chip.setOnClickListener { selectDay(day) }
            dayContainer.addView(chip)
        }
    }

    private fun selectDay(day: String) {
        selectedDay = day

        for (i in 0 until dayContainer.childCount) {
            val chip = dayContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == day
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

        renderSchedule(day)
    }

    private fun renderSchedule(day: String) {
        classesContainer.removeAllViews()

        val schedule = RoutineCatalog.scheduleFor(day)

        if (schedule.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No classes on $day"
            return
        }

        tvEmpty.visibility = View.GONE

        for (entry in schedule) {
            val item = layoutInflater.inflate(R.layout.item_routine, classesContainer, false)
            item.findViewById<TextView>(R.id.tvStartTime).text = entry.startTime
            item.findViewById<TextView>(R.id.tvEndTime).text = entry.endTime
            item.findViewById<TextView>(R.id.tvCourseCode).text = entry.courseCode
            item.findViewById<TextView>(R.id.tvCourseTitle).text =
                CourseCatalog.titleOf(entry.courseCode)
            item.findViewById<TextView>(R.id.tvFacultyRoom).text =
                "FC: ${entry.faculty}  ·  R: ${entry.room}"
            classesContainer.addView(item)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
