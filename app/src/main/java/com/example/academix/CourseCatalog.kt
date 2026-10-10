package com.example.academix

/**
 * Hardcoded catalog of the CSE Intake 53 courses.
 *
 * Used as the source for:
 *  - the registration course checkboxes
 *  - mapping a course code to its title on the dashboard
 *
 * Later this can be replaced by a Firestore "courses" collection.
 */
object CourseCatalog {

    val courses = listOf(
        Course(courseCode = "ACT 301", title = "Accounting and Management"),
        Course(courseCode = "CSE 327", title = "Software Engineering"),
        Course(courseCode = "CSE 328", title = "Software Engineering Lab"),
        Course(courseCode = "CSE 403", title = "Machine Learning"),
        Course(courseCode = "CSE 404", title = "Machine Learning Lab"),
        Course(courseCode = "MKT 301", title = "Digital Marketing")
    )

    fun titleOf(code: String): String =
        courses.firstOrNull { it.courseCode == code }?.title ?: code
}
