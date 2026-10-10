package com.example.academix

data class Course(
    val courseCode: String = "",
    val title: String = "",
    val department: String = "CSE",
    val shift: String = "",
    val intake: String = "",
    val section: String = "",
    val teacherId: String = "",
    val taId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "courseCode" to courseCode,
        "title" to title,
        "department" to department,
        "shift" to shift,
        "intake" to intake,
        "section" to section,
        "teacherId" to teacherId,
        "taId" to taId
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): Course = Course(
            courseCode = data["courseCode"] as? String ?: "",
            title = data["title"] as? String ?: "",
            department = data["department"] as? String ?: "CSE",
            shift = data["shift"] as? String ?: "",
            intake = data["intake"] as? String ?: "",
            section = data["section"] as? String ?: "",
            teacherId = data["teacherId"] as? String ?: "",
            taId = data["taId"] as? String ?: ""
        )
    }
}
