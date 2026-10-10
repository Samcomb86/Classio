package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TimetableData(
    val meta: MetaData,
    val timeSlots: List<TimeSlot>,
    val days: List<String>,
    val classes: List<ClassInfo>,
    val timetable: Map<String, Map<String, List<Lecture>>>
)

@JsonClass(generateAdapter = true)
data class MetaData(
    val version: String = "V1.6",
    val sheetName: String = "Timetable Fall 2026 (V1.6)",
    val department: String = "Department of Computer Science & IT",
    val source: String? = null,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class TimeSlot(
    val start: String,
    val end: String
)

@JsonClass(generateAdapter = true)
data class ClassInfo(
    val id: String,            // e.g., "BSCS-1-R"
    val label: String,         // e.g., "BSCS 1ST R"
    val programCode: String,   // e.g., "BSCS"
    val program: String,       // e.g., "BS Computer Science"
    val semester: Int,         // e.g., 1
    val shift: String,         // "regular" or "second"
    val section: String? = null // "SS1", "SS2", or null
)

@JsonClass(generateAdapter = true)
data class Lecture(
    val start: String,
    val end: String,
    val subject: String,
    val code: String? = null,
    val type: String? = null,    // "Theory" | "Lab" | null
    val teacher: String? = null, // normalized display name
    val teacherKey: String? = null, // grouping key
    val room: String? = null,
    val raw: String? = null
)

enum class Shift(val code: String, val label: String, val sheetCode: String) {
    REGULAR("regular", "Morning (Regular - R)", "R"),
    SECOND("second", "Afternoon (Self Study - SS)", "SS")
}

enum class DeptStatus {
    LIVE,
    COMING_SOON
}

data class Department(
    val name: String,
    val status: DeptStatus = DeptStatus.COMING_SOON,
    val programs: List<DegreeProgram>
) {
    val isLive: Boolean get() = status == DeptStatus.LIVE
}

data class Faculty(
    val id: String,
    val name: String,
    val status: DeptStatus = DeptStatus.COMING_SOON,
    val programs: List<DegreeProgram>
) {
    val isLive: Boolean get() = status == DeptStatus.LIVE
}

data class DegreeProgram(
    val name: String,
    val sheetCode: String? = null,
    val code: String = sheetCode ?: name,
    val isFifthSemesterEntry: Boolean = false
) {
    val defaultSemesters: List<Int>
        get() = if (isFifthSemesterEntry) listOf(5, 6, 7, 8) else listOf(1, 2, 3, 4, 5, 6, 7, 8)
}

data class SelectedClassFilter(
    val departmentName: String,
    val programCode: String,
    val programName: String,
    val semester: Int,
    val shift: String, // "regular" or "second"
    val section: String? = null
) {
    val classId: String
        get() = if (shift == "second" && section != null) {
            "$programCode-$semester-$section"
        } else if (shift == "second") {
            "$programCode-$semester-SS"
        } else {
            "$programCode-$semester-R"
        }
}

// Teacher Index & Models
data class TeacherInfo(
    val key: String,
    val name: String,
    val department: String,
    val lectureCount: Int
)

data class TeacherLecture(
    val start: String,
    val end: String,
    val subject: String,
    val code: String? = null,
    val type: String? = null,
    val room: String? = null,
    val classLabels: List<String>,
    val clash: Boolean = false
)

data class TeacherIndex(
    val byTeacher: Map<String, Map<String, List<TeacherLecture>>>,
    val teachers: List<TeacherInfo>
)

// User Profile (persisted across sessions)
sealed class UserProfile {
    data class Student(
        val department: String,
        val programCode: String,
        val programName: String,
        val semester: Int,
        val shift: String, // "regular" or "second"
        val section: String? = null
    ) : UserProfile()

    data class Teacher(
        val department: String,
        val teacherName: String,
        val teacherKey: String
    ) : UserProfile()
}

fun UserProfile.Student.toFilter(): SelectedClassFilter = SelectedClassFilter(
    departmentName = department,
    programCode = programCode,
    programName = programName,
    semester = semester,
    shift = shift,
    section = section
)

@JsonClass(generateAdapter = true)
data class AlertItem(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val read: Boolean = false
)

