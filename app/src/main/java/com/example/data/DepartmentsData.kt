package com.example.data

import com.example.model.DegreeProgram
import com.example.model.Department
import com.example.model.DeptStatus
import com.example.model.Faculty

object DepartmentsData {

    val PROGRAM_NAME_MAPPINGS = mapOf(
        "BSCS" to "BS Computer Science",
        "BSIT" to "BS Information Technology",
        "BSSE" to "BS Software Engineering",
        "BSAI" to "BS Artificial Intelligence",
        "BSCS-5TH" to "BSCS 5th",
        "BSIT-5TH" to "BSIT 5th"
    )

    // ==========================================
    // 1. FACULTY OF COMPUTING (LIVE)
    // ==========================================
    val FACULTY_COMPUTING = Faculty(
        id = "computing",
        name = "Faculty of Computing",
        status = DeptStatus.LIVE,
        programs = listOf(
            DegreeProgram(name = "BS Artificial Intelligence (BSAI)", sheetCode = "BSAI", code = "BSAI"),
            DegreeProgram(name = "BS Computer Science (BSCS)", sheetCode = "BSCS", code = "BSCS"),
            DegreeProgram(name = "BS Information Technology (BSIT)", sheetCode = "BSIT", code = "BSIT"),
            DegreeProgram(name = "BS Software Engineering (BSSE)", sheetCode = "BSSE", code = "BSSE"),
            DegreeProgram(name = "BSCS 5th", sheetCode = "BSCS", code = "BSCS-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BSIT 5th", sheetCode = "BSIT", code = "BSIT-5TH", isFifthSemesterEntry = true)
        )
    )

    // ==========================================
    // 2. FACULTY OF SCIENCES (COMING SOON)
    // ==========================================
    val FACULTY_SCIENCES = Faculty(
        id = "sciences",
        name = "Faculty of Sciences",
        status = DeptStatus.COMING_SOON,
        programs = listOf(
            DegreeProgram(name = "BS Chemistry", sheetCode = "BSCHEM", code = "BSCHEM"),
            DegreeProgram(name = "BS Chemistry 5th", sheetCode = "BSCHEM", code = "BSCHEM-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Botany", sheetCode = "BSBOT", code = "BSBOT"),
            DegreeProgram(name = "BS Botany 5th", sheetCode = "BSBOT", code = "BSBOT-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Mathematics", sheetCode = "BSMATH", code = "BSMATH"),
            DegreeProgram(name = "BS Zoology", sheetCode = "BSZOO", code = "BSZOO"),
            DegreeProgram(name = "BS Zoology 5th", sheetCode = "BSZOO", code = "BSZOO-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Physics", sheetCode = "BSPHYS", code = "BSPHYS"),
            DegreeProgram(name = "BS Physics 5th", sheetCode = "BSPHYS", code = "BSPHYS-5TH", isFifthSemesterEntry = true)
        )
    )

    // ==========================================
    // 3. FACULTY OF MANAGEMENT SCIENCES (COMING SOON)
    // ==========================================
    val FACULTY_MANAGEMENT = Faculty(
        id = "management",
        name = "Faculty of Management Sciences",
        status = DeptStatus.COMING_SOON,
        programs = listOf(
            DegreeProgram(name = "Bachelor of Business Administration (BBA)", sheetCode = "BBA", code = "BBA"),
            DegreeProgram(name = "BBA 5th", sheetCode = "BBA", code = "BBA-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Commerce", sheetCode = "BSCOMM", code = "BSCOMM"),
            DegreeProgram(name = "BS Economics", sheetCode = "BSECON", code = "BSECON"),
            DegreeProgram(name = "BS Economics 5th", sheetCode = "BSECON", code = "BSECON-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "Bachelor of Banking and Finance", sheetCode = "BBF", code = "BBF"),
            DegreeProgram(name = "Bachelor of Accounting and Finance", sheetCode = "BAF", code = "BAF")
        )
    )

    // ==========================================
    // 4. FACULTY OF ARTS & HUMANITIES (COMING SOON)
    // ==========================================
    val FACULTY_ARTS = Faculty(
        id = "arts",
        name = "Faculty of Arts & Humanities",
        status = DeptStatus.COMING_SOON,
        programs = listOf(
            DegreeProgram(name = "BS Communication & Media Studies", sheetCode = "BSCMS", code = "BSCMS"),
            DegreeProgram(name = "BS English", sheetCode = "BSENG", code = "BSENG"),
            DegreeProgram(name = "BS English 5th", sheetCode = "BSENG", code = "BSENG-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS History", sheetCode = "BSHIST", code = "BSHIST"),
            DegreeProgram(name = "BS History 5th", sheetCode = "BSHIST", code = "BSHIST-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Islamic Studies", sheetCode = "BSISLAM", code = "BSISLAM"),
            DegreeProgram(name = "BS Islamic Studies 5th", sheetCode = "BSISLAM", code = "BSISLAM-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Physical Education", sheetCode = "BSPE", code = "BSPE"),
            DegreeProgram(name = "BS Physical Education 5th", sheetCode = "BSPE", code = "BSPE-5TH", isFifthSemesterEntry = true),
            DegreeProgram(name = "BS Urdu", sheetCode = "BSURDU", code = "BSURDU"),
            DegreeProgram(name = "BS Urdu 5th", sheetCode = "BSURDU", code = "BSURDU-5TH", isFifthSemesterEntry = true)
        )
    )

    // ==========================================
    // 5. FACULTY OF SOCIAL SCIENCES (COMING SOON)
    // ==========================================
    val FACULTY_SOCIAL = Faculty(
        id = "social",
        name = "Faculty of Social Sciences",
        status = DeptStatus.COMING_SOON,
        programs = listOf(
            DegreeProgram(name = "BS Education", sheetCode = "BSEDU", code = "BSEDU"),
            DegreeProgram(name = "BS International Relations", sheetCode = "BSIR", code = "BSIR"),
            DegreeProgram(name = "BS Psychology", sheetCode = "BSPSY", code = "BSPSY"),
            DegreeProgram(name = "BS Sociology", sheetCode = "BSSOC", code = "BSSOC"),
            DegreeProgram(name = "BS Social Work", sheetCode = "BSSW", code = "BSSW")
        )
    )

    // Complete list of all 5 Faculties
    val ALL_FACULTIES: List<Faculty> = listOf(
        FACULTY_COMPUTING,
        FACULTY_SCIENCES,
        FACULTY_MANAGEMENT,
        FACULTY_ARTS,
        FACULTY_SOCIAL
    )

    // 5 University Faculties / Departments (1 Live + 4 Coming Soon)
    val ALL_DEPARTMENTS: List<Department> = ALL_FACULTIES.map { fac ->
        Department(
            name = fac.name,
            status = fac.status,
            programs = fac.programs
        )
    }

    val LIVE_DEPARTMENT = ALL_DEPARTMENTS.first { it.isLive }

    fun getProgramName(code: String): String {
        return PROGRAM_NAME_MAPPINGS[code]
            ?: ALL_FACULTIES.flatMap { it.programs }.firstOrNull { it.code.equals(code, ignoreCase = true) || it.sheetCode.equals(code, ignoreCase = true) }?.name
            ?: ALL_DEPARTMENTS.flatMap { it.programs }.firstOrNull { it.code.equals(code, ignoreCase = true) || it.sheetCode.equals(code, ignoreCase = true) }?.name
            ?: code
    }
}
