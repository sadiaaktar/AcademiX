package com.example.academix

import java.util.Calendar

data class RoutineEntry(
    val courseCode: String,
    val faculty: String,
    val room: String,
    val startTime: String,
    val endTime: String,
    val day: String
)

/**
 * Hardcoded class routine for CSE Intake 53, Section 5.
 * Later this can be moved to Firestore for dynamic schedules.
 */
object RoutineCatalog {

    val days = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")

    private val entries = listOf(
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

    fun scheduleFor(day: String): List<RoutineEntry> =
        entries.filter { it.day == day }

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
