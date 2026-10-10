package com.example.parser

import com.example.data.DepartmentsData
import com.example.model.*
import java.util.Locale

object TimetableParser {

    private val SEMESTER_REGEX = Regex("""(\d+)(?:ST|ND|RD|TH)?""", RegexOption.IGNORE_CASE)
    private val CLASS_REGEX = Regex("""([A-Z]+)\s+(\d+)(?:ST|ND|RD|TH)?\s*(R|SS\d*|SS)?""", RegexOption.IGNORE_CASE)
    private val TIME_RANGE_REGEX = Regex("""(\d{1,2}):(\d{2})\s*(am|pm)?\s*-\s*(\d{1,2}):(\d{2})\s*(am|pm)?""", RegexOption.IGNORE_CASE)

    fun parseCsv(csvText: String, sourceName: String = "TT_DCSIT_FALL_2026_V_1_6.csv"): TimetableData {
        val rows = parseCsvRows(csvText)
        if (rows.isEmpty()) {
            throw IllegalArgumentException("Empty CSV data")
        }

        // 1. Find Header Row
        var headerRowIndex = -1
        for (i in rows.indices) {
            val row = rows[i]
            if (row.any { it.contains("Class", ignoreCase = true) } && row.any { it.contains("Batch", ignoreCase = true) || it.contains("Time", ignoreCase = true) }) {
                headerRowIndex = i
                break
            }
        }
        if (headerRowIndex == -1) {
            // Fallback: look for 8:00 am or first non-empty row with time slots
            headerRowIndex = rows.indexOfFirst { r -> r.any { it.contains("8:00", ignoreCase = true) } }
        }
        if (headerRowIndex == -1) {
            throw IllegalArgumentException("Could not locate timetable header row")
        }

        val headerRow = rows[headerRowIndex]
        val timeSlots = mutableListOf<TimeSlot>()
        val slotColIndices = mutableListOf<Int>()
        var breakColIndex = -1

        for (col in 2 until headerRow.size) {
            val colText = headerRow[col].trim()
            if (colText.isEmpty()) continue
            val slot = parseTimeSlot(colText)
            if (slot != null) {
                timeSlots.add(slot)
                slotColIndices.add(col)
                if (slot.start == "12:50" || slot.end == "13:10" || colText.contains("12:50", ignoreCase = true)) {
                    breakColIndex = col
                }
            }
        }

        // 2. Iterate data rows and forward-fill days
        var currentDay = ""
        val daysList = mutableListOf<String>()
        val classesList = mutableListOf<ClassInfo>()
        val classMap = mutableMapOf<String, ClassInfo>()
        val timetableMap = mutableMapOf<String, MutableMap<String, MutableList<Lecture>>>()

        for (r in (headerRowIndex + 1) until rows.size) {
            val row = rows[r]
            if (row.all { it.isBlank() }) continue

            val dayCell = if (row.isNotEmpty()) row[0].trim() else ""
            if (dayCell.isNotBlank() && isDayName(dayCell)) {
                currentDay = normalizeDayName(dayCell)
                if (!daysList.contains(currentDay)) {
                    daysList.add(currentDay)
                }
            }

            if (currentDay.isBlank()) continue

            val classCell = if (row.size > 1) row[1].trim() else ""
            if (classCell.isBlank()) continue

            val classInfo = parseClassInfo(classCell) ?: continue
            if (!classMap.containsKey(classInfo.id)) {
                classMap[classInfo.id] = classInfo
                classesList.add(classInfo)
            }

            val dayMap = timetableMap.getOrPut(classInfo.id) { mutableMapOf() }
            val rawLecturesForDay = mutableListOf<Lecture>()

            // Extract each slot's lecture
            for (i in slotColIndices.indices) {
                val col = slotColIndices[i]
                val slot = timeSlots[i]
                val cellText = if (col < row.size) row[col].trim() else ""

                if (cellText.isBlank() || isBreakCell(cellText) || col == breakColIndex) {
                    continue
                }

                val lecture = parseLectureCell(cellText, slot.start, slot.end)
                if (lecture != null) {
                    rawLecturesForDay.add(lecture)
                }
            }

            // Merge consecutive slots for the same lecture
            val mergedLectures = mergeConsecutiveLectures(rawLecturesForDay)
            val existingList = dayMap.getOrPut(currentDay) { mutableListOf() }
            existingList.addAll(mergedLectures)
        }

        // Sort days logically if needed
        val finalDays = if (daysList.isNotEmpty()) daysList else listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

        // Ensure all classes have an entry for each day
        val finalTimetable = mutableMapOf<String, Map<String, List<Lecture>>>()
        for (c in classesList) {
            val dayMap = timetableMap[c.id] ?: mutableMapOf()
            val finalDayMap = mutableMapOf<String, List<Lecture>>()
            for (d in finalDays) {
                finalDayMap[d] = dayMap[d] ?: emptyList()
            }
            finalTimetable[c.id] = finalDayMap
        }

        val meta = MetaData(
            version = "V1.6",
            sheetName = "Timetable Fall 2026 (V1.6)",
            department = "Department of Computer Science & IT",
            source = sourceName
        )

        return TimetableData(
            meta = meta,
            timeSlots = timeSlots,
            days = finalDays,
            classes = classesList,
            timetable = finalTimetable
        )
    }

    private fun isDayName(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.startsWith("mon") || lower.startsWith("tue") || lower.startsWith("wed") ||
                lower.startsWith("thu") || lower.startsWith("fri") || lower.startsWith("sat") || lower.startsWith("sun")
    }

    private fun normalizeDayName(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("mon") -> "Monday"
            lower.startsWith("tue") -> "Tuesday"
            lower.startsWith("wed") -> "Wednesday"
            lower.startsWith("thu") -> "Thursday"
            lower.startsWith("fri") -> "Friday"
            lower.startsWith("sat") -> "Saturday"
            lower.startsWith("sun") -> "Sunday"
            else -> text
        }
    }

    private fun parseClassInfo(label: String): ClassInfo? {
        val cleanLabel = label.trim().replace("\\s+".toRegex(), " ")
        val parts = cleanLabel.split(" ")
        if (parts.size < 2) return null

        val programCode = parts[0].uppercase(Locale.ROOT)
        val semMatch = SEMESTER_REGEX.find(parts[1]) ?: return null
        val semester = semMatch.groupValues[1].toIntOrNull() ?: return null

        val shiftPart = if (parts.size >= 3) parts[2].uppercase(Locale.ROOT) else "R"
        val (shift, section) = when {
            shiftPart.startsWith("SS1") -> Pair("second", "SS1")
            shiftPart.startsWith("SS2") -> Pair("second", "SS2")
            shiftPart.startsWith("SS") -> Pair("second", null)
            else -> Pair("regular", null)
        }

        val id = if (shift == "second" && section != null) {
            "$programCode-$semester-$section"
        } else if (shift == "second") {
            "$programCode-$semester-SS"
        } else {
            "$programCode-$semester-R"
        }

        val programName = DepartmentsData.getProgramName(programCode)

        return ClassInfo(
            id = id,
            label = cleanLabel,
            programCode = programCode,
            program = programName,
            semester = semester,
            shift = shift,
            section = section
        )
    }

    fun parseLectureCell(cellText: String, start: String, end: String): Lecture? {
        val clean = cellText.trim()
        if (clean.isEmpty() || isBreakCell(clean)) return null

        val lines = clean.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val subject: String
        val detailLine: String

        if (lines.size >= 2) {
            subject = lines[0].trim()
            detailLine = lines.subList(1, lines.size).joinToString(" ").trim()
        } else {
            val single = lines[0]
            val codeIdx = single.indexOfFirst { it.isLetter() && single.contains("-") }
            if (codeIdx > 0 && single.contains(Regex("""[A-Z]{2,4}-+\d{3,4}"""))) {
                val match = Regex("""[A-Z]{2,4}-+\d{3,4}""").find(single)
                if (match != null && match.range.first > 0) {
                    subject = single.substring(0, match.range.first).trim()
                    detailLine = single.substring(match.range.first).trim()
                } else {
                    subject = single
                    detailLine = ""
                }
            } else {
                subject = single
                detailLine = ""
            }
        }

        var courseCode: String? = null
        var type: String? = null
        var teacher: String? = null
        var room: String? = null

        if (detailLine.isNotEmpty()) {
            var working = detailLine.replace("\\s+".toRegex(), " ")

            // 1. Extract Room at the end (e.g. R110, R16, R219, CL, R)
            val roomMatch = Regex("""\b(R\d{2,3}|CL|R)\b\s*$""", RegexOption.IGNORE_CASE).find(working)
            if (roomMatch != null) {
                val foundRoom = roomMatch.groupValues[1].uppercase(Locale.ROOT)
                room = if (foundRoom == "R") null else foundRoom
                working = working.substring(0, roomMatch.range.first).trim()
            }

            // 2. Extract Course Code (e.g. COMP-5101, GENR-5105, CSCC--301, Math-202, CS-112)
            val codeMatch = Regex("""\b([A-Za-z]{2,4})-+(\d{3,4})\b""").find(working)
            if (codeMatch != null) {
                val prefix = codeMatch.groupValues[1].uppercase(Locale.ROOT)
                val number = codeMatch.groupValues[2]
                courseCode = "$prefix-$number"
                working = working.replaceRange(codeMatch.range, " ").trim()
            }

            // 3. Extract Type (T) / [T] -> Theory, (L) / [L] -> Lab
            val typeMatch = Regex("""(?:\(|\[)\s*([TL])\s*(?:\)|\])|\b([TL])\b""", RegexOption.IGNORE_CASE).find(working)
            if (typeMatch != null) {
                val t = (typeMatch.groupValues[1].ifEmpty { typeMatch.groupValues[2] }).uppercase(Locale.ROOT)
                type = when (t) {
                    "T" -> "Theory"
                    "L" -> "Lab"
                    else -> null
                }
                working = working.replaceRange(typeMatch.range, " ").trim()
            }

            // 4. Remaining text is the Teacher
            val normalizedTeacher = TeacherNormalizer.normalizeTeacher(working)
            teacher = normalizedTeacher
        }

        val teacherKey = TeacherNormalizer.getTeacherKey(teacher)

        return Lecture(
            start = start,
            end = end,
            subject = subject,
            code = courseCode,
            type = type,
            teacher = teacher,
            teacherKey = teacherKey,
            room = room,
            raw = clean
        )
    }

    private fun isBreakCell(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("break") || lower.contains("prayer") || lower.contains("lunch")
    }

    private fun mergeConsecutiveLectures(lectures: List<Lecture>): List<Lecture> {
        if (lectures.isEmpty()) return emptyList()
        val merged = mutableListOf<Lecture>()
        var current = lectures[0]

        for (i in 1 until lectures.size) {
            val next = lectures[i]
            val isSame = current.subject.equals(next.subject, ignoreCase = true) &&
                    current.code == next.code &&
                    current.type == next.type &&
                    current.teacher == next.teacher &&
                    current.room == next.room &&
                    current.end == next.start

            if (isSame) {
                current = current.copy(end = next.end)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun parseTimeSlot(text: String): TimeSlot? {
        val clean = text.trim()
        val match = TIME_RANGE_REGEX.find(clean) ?: return null
        val h1 = match.groupValues[1].toInt()
        val m1 = match.groupValues[2].toInt()
        val ampm1 = match.groupValues[3].lowercase(Locale.ROOT)
        val h2 = match.groupValues[4].toInt()
        val m2 = match.groupValues[5].toInt()
        val ampm2 = match.groupValues[6].lowercase(Locale.ROOT)

        val normalizedAmpm2 = if (ampm2.isNotEmpty()) ampm2 else if (h2 in 1..7) "pm" else "am"
        val normalizedAmpm1 = if (ampm1.isNotEmpty()) ampm1 else if (h1 in 1..7) "pm" else if (h1 in 8..11) "am" else normalizedAmpm2

        val start24 = to24Hour(h1, m1, normalizedAmpm1)
        val end24 = to24Hour(h2, m2, normalizedAmpm2)

        return TimeSlot(start = start24, end = end24)
    }

    private fun to24Hour(hour: Int, min: Int, ampm: String): String {
        var h = hour
        if (ampm == "pm" && h < 12) h += 12
        if (ampm == "am" && h == 12) h = 0
        return String.format(Locale.US, "%02d:%02d", h, min)
    }

    private fun parseCsvRows(csvText: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < csvText.length) {
            val c = csvText[i]
            if (insideQuotes) {
                if (c == '"') {
                    if (i + 1 < csvText.length && csvText[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        insideQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> insideQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < csvText.length && csvText[i + 1] == '\n') i++
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        result.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        result.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            result.add(ArrayList(currentRow))
        }
        return result
    }
}
