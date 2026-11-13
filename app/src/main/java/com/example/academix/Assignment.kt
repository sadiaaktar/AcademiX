package com.example.academix

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Assignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDateMillis: Long = 0L,
    val section: String = "All",
    val courseCode: String = "",
    val postedByUid: String = "",
    val postedByName: String = "",
    val attachmentData: String = "",
    val attachmentName: String = "",
    val createdAtMillis: Long = 0L
) {
    fun hasAttachment(): Boolean = attachmentData.isNotEmpty()

    companion object {
        fun fromDocument(id: String, data: Map<String, Any?>): Assignment =
            Assignment(
                id = id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                dueDateMillis = data["dueDateMillis"] as? Long ?: 0L,
                section = data["section"] as? String ?: "All",
                courseCode = data["courseCode"] as? String ?: "",
                postedByUid = data["postedByUid"] as? String ?: "",
                postedByName = data["postedByName"] as? String ?: "",
                attachmentData = data["attachmentData"] as? String ?: "",
                attachmentName = data["attachmentName"] as? String ?: "",
                createdAtMillis = data["createdAtMillis"] as? Long ?: 0L
            )
    }
}

data class Submission(
    val studentUid: String = "",
    val studentName: String = "",
    val pdfData: String = "",
    val fileName: String = "",
    val submittedAtMillis: Long = 0L
) {
    companion object {
        fun fromDocument(data: Map<String, Any?>): Submission =
            Submission(
                studentUid = data["studentUid"] as? String ?: "",
                studentName = data["studentName"] as? String ?: "",
                pdfData = data["pdfData"] as? String ?: "",
                fileName = data["fileName"] as? String ?: "",
                submittedAtMillis = data["submittedAtMillis"] as? Long ?: 0L
            )
    }
}

fun formatDueDate(millis: Long): String {
    if (millis <= 0) return ""
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(millis))
}
