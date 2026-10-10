package com.example

import com.example.data.DepartmentsData
import com.example.model.DeptStatus
import com.example.parser.TeacherNormalizer
import com.example.parser.TimetableParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TimetableParserUnitTest {

    @Test
    fun testParseLectureCellTheory() {
        val raw = "Programming Fundamentals\nCOMP-5101 (T) Mr. Jahanzaib Ali Khan R110"
        val lecture = TimetableParser.parseLectureCell(raw, "08:00", "09:00")

        assertNotNull(lecture)
        assertEquals("Programming Fundamentals", lecture?.subject)
        assertEquals("COMP-5101", lecture?.code)
        assertEquals("Theory", lecture?.type)
        assertEquals("Mr. Jahanzaib Ali Khan", lecture?.teacher)
        assertEquals("R110", lecture?.room)
    }

    @Test
    fun testParseLectureCellLabAndCL() {
        val raw = "Programming Fundamentals\nCOMP-5101 (L) Dr. Muhammad Iqbal CL"
        val lecture = TimetableParser.parseLectureCell(raw, "08:00", "09:00")

        assertNotNull(lecture)
        assertEquals("Programming Fundamentals", lecture?.subject)
        assertEquals("COMP-5101", lecture?.code)
        assertEquals("Lab", lecture?.type)
        assertEquals("Dr. Muhammad Iqbal", lecture?.teacher)
        assertEquals("CL", lecture?.room)
    }

    @Test
    fun testParseDoubleDashCode() {
        val raw = "Theory of Automata\nCSCC--301 (T) (Mr. Asad Ullah) R215"
        val lecture = TimetableParser.parseLectureCell(raw, "10:00", "11:00")

        assertNotNull(lecture)
        assertEquals("Theory of Automata", lecture?.subject)
        assertEquals("CSCC-301", lecture?.code)
        assertEquals("Theory", lecture?.type)
        assertEquals("Mr. Asad Ullah", lecture?.teacher)
        assertEquals("R215", lecture?.room)
    }

    @Test
    fun testTeacherNormalization() {
        // Acceptance Test 6: normalizeTeacher("(Mr.Muhammad Sheraz)") returns "Mr. Muhammad Sheraz", and normalizeTeacher("(?)") returns null
        val normSheraz = TeacherNormalizer.normalizeTeacher("(Mr.Muhammad Sheraz)")
        assertEquals("Mr. Muhammad Sheraz", normSheraz)

        val normQuestion = TeacherNormalizer.normalizeTeacher("(?)")
        assertNull(normQuestion)
    }

    @Test
    fun testParseFullCsvAssetAndAcceptanceCriteria() {
        val csvFile = File("src/main/assets/TT_DCSIT_FALL_2026_V_1_6.csv")
        assertTrue(csvFile.exists())
        val csvText = csvFile.readText()
        val data = TimetableParser.parseCsv(csvText)

        // 1. Total distinct classes parsed from the attached file: 34
        assertEquals(34, data.classes.size)

        // 2. BSCS 1ST R, Monday: Programming Fundamentals (COMP-5101, Theory, Mr. Jahanzaib Ali Khan, R110) 08:00 to 10:00
        val bscs1Mon = data.timetable["BSCS-1-R"]?.get("Monday")
        assertNotNull(bscs1Mon)
        val pf = bscs1Mon!!.firstOrNull { it.subject.contains("Programming Fundamentals") }
        assertNotNull(pf)
        assertEquals("08:00", pf?.start)
        assertEquals("10:00", pf?.end)
        assertEquals("COMP-5101", pf?.code)
        assertEquals("Theory", pf?.type)
        assertEquals("Mr. Jahanzaib Ali Khan", pf?.teacher)
        assertEquals("R110", pf?.room)

        val qr = bscs1Mon.firstOrNull { it.subject.contains("Quantitative Reasoning-I") }
        assertNotNull(qr)
        assertEquals("10:00", qr?.start)
        assertEquals("12:00", qr?.end)
        assertEquals("GENR-5105", qr?.code)
        assertEquals("R110", qr?.room)

        // 3. BSIT 7TH R, Monday: IT Infrastructure (ITCC-407, Ms. Seemal Niazi, R221) from 10:00 to 12:00
        val bsit7Mon = data.timetable["BSIT-7-R"]?.get("Monday")
        assertNotNull(bsit7Mon)
        val itInfra = bsit7Mon!!.firstOrNull { it.subject.contains("IT Infrastructure") }
        assertNotNull(itInfra)
        assertEquals("10:00", itInfra?.start)
        assertEquals("12:00", itInfra?.end)
        assertEquals("ITCC-407", itInfra?.code)
        assertEquals("Ms. Seemal Niazi", itInfra?.teacher)
        assertEquals("R221", itInfra?.room)

        // 4. BSCS 5TH SS1, Wednesday: first lecture is Operating Systems (CMPC-301, Lab, Mr. Najaf Ali Khan, R18) from 09:00 to 10:00
        val bscs5ss1Wed = data.timetable["BSCS-5-SS1"]?.get("Wednesday")
        assertNotNull(bscs5ss1Wed)
        val os = bscs5ss1Wed!!.firstOrNull()
        assertNotNull(os)
        assertEquals("Operating Systems", os?.subject)
        assertEquals("09:00", os?.start)
        assertEquals("10:00", os?.end)
        assertEquals("CMPC-301", os?.code)
        assertEquals("Lab", os?.type)
        assertEquals("Mr. Najaf Ali Khan", os?.teacher)
        assertEquals("R18", os?.room)

        // 5. 5 departments scope: 1 Live (Faculty of Computing), 4 Coming Soon
        val allDepts = DepartmentsData.ALL_DEPARTMENTS
        assertEquals(5, allDepts.size)
        val liveDepts = allDepts.filter { it.isLive }
        assertEquals(1, liveDepts.size)
        assertEquals("Faculty of Computing", liveDepts[0].name)
        val comingSoonDepts = allDepts.filter { it.status == DeptStatus.COMING_SOON }
        assertEquals(4, comingSoonDepts.size)

        // 7. Teacher index building & verification
        val teacherIndex = TeacherNormalizer.buildTeacherIndex(data, "Faculty of Computing")
        val sherazList = teacherIndex.teachers.filter { it.name == "Mr. Muhammad Sheraz" }
        assertEquals(1, sherazList.size)

        // 8. For teacher "Mr. Jahanzaib Ali Khan", Monday schedule contains BSCS 1ST R, Programming Fundamentals, 08:00 to 10:00, room R110
        val jahanzaibKey = TeacherNormalizer.getTeacherKey("Mr. Jahanzaib Ali Khan") ?: ""
        val jahanzaibMon = teacherIndex.byTeacher[jahanzaibKey]?.get("Monday")
        assertNotNull(jahanzaibMon)
        val jahanzaibLec = jahanzaibMon?.firstOrNull { it.subject.contains("Programming Fundamentals") }
        assertNotNull(jahanzaibLec)
        assertEquals("08:00", jahanzaibLec?.start)
        assertEquals("10:00", jahanzaibLec?.end)
        assertEquals("R110", jahanzaibLec?.room)
        assertTrue(jahanzaibLec?.classLabels?.contains("BSCS 1ST R") == true)
    }
}
