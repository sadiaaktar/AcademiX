package com.example.academix

import java.util.Calendar

data class RoutineEntry(
    val courseCode: String,
    val faculty: String,
    val room: String,
    val startTime: String,
    val endTime: String,
    val day: String,
    val section: String = ""
) {
    fun facultyRoomText(): String {
        val base = if (faculty.isBlank()) room else "FC: $faculty  ·  R: $room"
        return if (section.isBlank()) base else "$base  ·  Section $section"
    }
}

/**
 * Hardcoded class routines, separated by role.
 * Later this can be moved to Firestore for dynamic schedules.
 */
object RoutineCatalog {

    val days = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")

    // Student routine (CSE Intake 53, Section 5)
    private val studentEntries = listOf(
        // SUN
        RoutineEntry("CSE 328", "NAT", "2416", "01:15 PM", "02:45 PM", "SUN"),
        RoutineEntry("CSE 403", "NMM", "2910", "02:45 PM", "04:15 PM", "SUN"),
        RoutineEntry("ACT 301", "JFS", "2910", "04:15 PM", "05:45 PM", "SUN"),
        // MON
        RoutineEntry("CSE 327", "NAT", "3905", "08:15 AM", "09:45 AM", "MON"),
        RoutineEntry("MKT 301", "SUM", "3905", "09:45 AM", "11:15 AM", "MON"),
        // TUE
        RoutineEntry("MKT 301", "SUM", "1302", "08:15 AM", "09:45 AM", "TUE"),
        RoutineEntry("CSE 327", "NAT", "1302", "09:45 AM", "11:15 AM", "TUE"),
        // WED
        RoutineEntry("ACT 301", "JFS", "3902", "02:45 PM", "04:15 PM", "WED"),
        RoutineEntry("CSE 403", "NMM", "3902", "04:15 PM", "05:45 PM", "WED"),
        // THR
        RoutineEntry("CSE 404", "NMM", "2218", "09:45 AM", "11:15 AM", "THR"),
        RoutineEntry("CSE 404", "NMM", "2218", "11:15 AM", "12:45 PM", "THR")
    )

    // Teacher routine (the teacher's own teaching schedule)
    private val teacherEntries = listOf(
        // SUN
        RoutineEntry("CSE 327", "", "R-605 (B-3)", "08:15 AM", "09:45 AM", "SUN", section = "6"),
        RoutineEntry("CSE 328", "", "R-417 (B-2)", "01:15 PM", "02:45 PM", "SUN", section = "5"),
        // MON
        RoutineEntry("CSE 327", "", "R-905 (B-3)", "08:15 AM", "09:45 AM", "MON", section = "5"),
        // TUE
        RoutineEntry("CSE 327", "", "R-302 (B-1)", "09:45 AM", "11:15 AM", "TUE", section = "5"),
        // WED
        RoutineEntry("CSE 327", "", "R-605 (B-3)", "02:45 PM", "04:15 PM", "WED", section = "6"),
        // THR
        RoutineEntry("CSE 328", "", "R-417 (B-2)", "01:15 PM", "02:45 PM", "THR", section = "6")
    )

    fun scheduleFor(day: String, role: String = "Student"): List<RoutineEntry> {
        val entries = if (role == "Teacher") teacherEntries else studentEntries
        return entries.filter { it.day == day }
    }

    fun teacherCourseCodes(): List<String> =
        teacherEntries.map { it.courseCode }.distinct()

    fun teacherSections(): List<String> =
        teacherEntries.map { it.section }.filter { it.isNotBlank() }.distinct().sorted()

    fun today(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "SUN"
        Calendar.MONDAY -> "MON"
        Calendar.TUESDAY -> "TUE"
        Calendar.WEDNESDAY -> "WED"
        Calendar.THURSDAY -> "THR"
        Calendar.FRIDAY -> "FRI"
        Calendar.SATURDAY -> "SAT"
        else -> "SUN"
    }

    fun todayName(): String = when (today()) {
        "SAT" -> "Saturday"
        "SUN" -> "Sunday"
        "MON" -> "Monday"
        "TUE" -> "Tuesday"
        "WED" -> "Wednesday"
        "THR" -> "Thursday"
        "FRI" -> "Friday"
        else -> ""
    }
}
