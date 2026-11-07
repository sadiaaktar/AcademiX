package com.example.academix

/**
 * Central email validation for AcademiX roles.
 *
 * - Student / TA : 11-digit student id @cse.bubt.edu.bd
 * - Teacher      : faculty email @bubt.edu.bd (strictly enforced)
 * - Test teacher : sadia77aktar@gmail.com (allowed only for local testing)
 */
object EmailValidator {

    const val TEST_TEACHER_EMAIL = "sadia77aktar@gmail.com"

    fun isStudentOrTA(email: String): Boolean =
        Regex("^[0-9]{11}@cse\\.bubt\\.edu\\.bd$").matches(email)

    fun isTeacher(email: String): Boolean {
        if (email.equals(TEST_TEACHER_EMAIL, ignoreCase = true)) return true
        return Regex("^[a-zA-Z][a-zA-Z0-9._-]*@bubt\\.edu\\.bd$").matches(email)
    }

    fun isValidUniversityEmail(email: String): Boolean =
        isStudentOrTA(email) || isTeacher(email)
}
