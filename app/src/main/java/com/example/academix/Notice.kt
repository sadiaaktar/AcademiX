package com.example.academix

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Notice(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = NoticeCategories.GENERAL,
    val postedByUid: String = "",
    val postedByName: String = "",
    val section: String = "All",
    val timestampMillis: Long = 0L
) {
    companion object {
        fun fromDocument(id: String, data: Map<String, Any?>, timestampMillis: Long): Notice =
            Notice(
                id = id,
                title = data["title"] as? String ?: "",
                content = data["content"] as? String ?: "",
                category = data["category"] as? String ?: NoticeCategories.GENERAL,
                postedByUid = data["postedByUid"] as? String ?: "",
                postedByName = data["postedByName"] as? String ?: "",
                section = data["section"] as? String ?: "All",
                timestampMillis = timestampMillis
            )
    }
}

object NoticeCategories {
    const val GENERAL = "General"
    const val RESCHEDULE = "Class Reschedule"
    const val CT_DATE = "CT Date"
    const val CT_MARKS = "CT Marks"

    val all = listOf(GENERAL, RESCHEDULE, CT_DATE, CT_MARKS)

    fun colorOf(category: String): Int = when (category) {
        RESCHEDULE -> R.color.accent_assignment
        CT_DATE -> R.color.accent_routine
        CT_MARKS -> R.color.accent_courses
        else -> R.color.brand_purple
    }

    fun softColorOf(category: String): Int = when (category) {
        RESCHEDULE -> R.color.accent_assignment_soft
        CT_DATE -> R.color.accent_routine_soft
        CT_MARKS -> R.color.accent_courses_soft
        else -> R.color.accent_notice_soft
    }
}

fun sectionLabel(section: String): String =
    if (section.isBlank() || section == "All") "All sections" else "Section $section"

fun formatTimestamp(millis: Long): String {
    if (millis <= 0) return ""
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
}
