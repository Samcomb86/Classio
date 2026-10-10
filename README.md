# TU Timetable - Thal University Bhakkar

A mobile-first, high-performance Android application built with Kotlin and Jetpack Compose for students and teachers of **Thal University Bhakkar** (https://tu.edu.pk).

The app allows students to find their own class timetable, and teachers to find their complete teaching schedule, both extracted from the university-wide timetable in 4 taps or fewer.

---

## 1. Quick Overview & Architecture

- **Student Flow (4 taps or fewer):**
  Role (Student) > Department > Program > Semester > Shift (First Time [Regular] vs Second Time [SS]) > Timetable.
- **Teacher Flow:**
  Role (Teacher) > Department > Teacher (Searchable) > Teaching Schedule across all classes with clash detection and section merging.
- **Launch Scope:**
  `Department of Computer Science & IT` is active and live with real timetable data. 18 other university departments are listed with a "Coming soon" badge and a direct link to get updates on the official WhatsApp channel.
- **Color Palette:**
  Centralized design tokens matching Thal University Bhakkar (deep navy `#0F2A4A` and brand orange `#F97316`).

---

## 2. Theming

All colors in the app are centralized as design tokens defined in `app/src/main/java/com/example/ui/theme/Color.kt` and applied dynamically via `TUTimetableTheme` in `Theme.kt`.

### Design Tokens Summary:

| Token | Light Theme Value | Dark Theme Value | Purpose |
| --- | --- | --- | --- |
| `--bg` | `#F5F7FB` | `#07101F` | Main page background |
| `--surface` | `#FFFFFF` | `#0E1B30` | Cards, dialogs, sheets |
| `--surface-2` | `#EEF2F8` | `#142540` | Chips, inputs, secondary containers |
| `--line` | `#E1E7F0` | `#20324E` | Borders and dividers |
| `--text` | `#0B1B33` | `#EAF0FA` | Main body text |
| `--muted` | `#5B6B82` | `#93A4BD` | Secondary and description text |
| `--primary` | `#0F2A4A` | `#FF9A3D` | Primary action buttons and selected states |
| `--on-primary` | `#FFFFFF` | `#07101F` | Text on primary buttons |
| `--primary-soft` | `#E3EBF7` | `#1B2F4D` | Soft blue highlights |
| `--accent` | `#F97316` | `#FF9A3D` | Brand orange highlights |
| `--on-accent` | `#0B1B33` | `#07101F` | Text on orange elements |
| `--accent-text` | `#C2410C` | `#FFB26B` | Orange text on light backgrounds |
| `--accent-soft` | `#FFF1E6` | `#2A2016` | Soft orange backgrounds |
| `--success` | `#15803D` | `#4ADE80` | Live status and current lecture badge |
| `--success-soft` | `#E6F6EC` | `#112A1D` | Soft green background |
| `--info-lab` | `#0F766E` | `#2DD4BF` | Lab lecture badge |
| `--info-lab-soft` | `#E0F4F1` | `#0F2B2A` | Soft teal lab background |
| `--danger` | `#DC2626` | `#F87171` | Clashes and error states |
| `--danger-soft` | `#FDECEC` | `#2E1618` | Soft error background |
| `--warning` | `#B45309` | `#FBBF24` | Offline/stale warning messages |
| `--warning-soft` | `#FEF3C7` | `#2B2410` | Soft warning background |

To change any color across the entire application, update `LightAppColors` or `DarkAppColors` in `Color.kt`.

---

## 3. Configuration & Automated Timetable Sync

Timetable URL configuration is managed in `AppConfig.kt` (and can be overridden live by tapping the version text in the footer 7 times):

```kotlin
// AppConfig.kt
object AppConfig {
    const val DEFAULT_TIMETABLE_SOURCE_URL = "" // Paste Google Sheet / Drive link here
    const val AUTO_REFRESH_MINUTES = 30
    const val WHATSAPP_CHANNEL_URL = "https://whatsapp.com/channel/0029VaJwzEZ11ulHnliDcu1e"
    const val UNIVERSITY_WEBSITE_URL = "https://tu.edu.pk"
}
```

### Supported Link Types (Auto-converted):
- Google Sheets: `https://docs.google.com/spreadsheets/d/<ID>/edit` -> `https://docs.google.com/spreadsheets/d/<ID>/export?format=xlsx` (or `/gviz/tq?tqx=out:csv`)
- Google Drive: `https://drive.google.com/file/d/<ID>/view` -> `https://drive.google.com/uc?export=download&id=<ID>`
- Direct CSV or XLSX URLs are downloaded directly.

### Proxy & CORS:
In an Android environment, OkHttp performs native HTTP requests with redirects enabled and bypasses browser CORS restrictions. For web deployments, the Google Visualization endpoint (`/gviz/tq?tqx=out:csv`) or a serverless proxy route (e.g. Next.js API route / Vercel function) can be used.

---

## 3. How to Enable a New Department

To switch a department from `coming_soon` to `live`, edit `DepartmentsData.kt`:

1. Change `status = DeptStatus.COMING_SOON` to `status = DeptStatus.LIVE`.
2. Add its degree programs with their respective sheet codes (e.g., `DegreeProgram("BS Chemistry", "BSCHEM")`).
3. That is a one-line change with no other code changes required.

---

## 4. Teacher Normalization & Indexing

The parser runs `TeacherNormalizer.normalizeTeacher(raw)` on every lecture cell:
1. Strips brackets `()`, `[]`, and stray parentheses.
2. Standardizes title spacing: `"Mr.Muhammad Sheraz"` -> `"Mr. Muhammad Sheraz"`. Handles `Mr`, `Mrs`, `Ms`, `Miss`, `Dr`, `Prof`, `Engr`, `Sir`, `Madam`.
3. Maps `"?"`, `"(?)"`, and empty values to `null`.
4. Generates a case-insensitive, dot-stripped `teacherKey` (e.g., `"mr muhammad sheraz"`).
5. Sorts teachers alphabetically by name without title.
6. Merges same-time lectures for multiple sections into one card and flags timetable clashes with an amber badge.

---

## 5. Acceptance Tests (/debug Screen)

Tap the version string in the footer to access acceptance diagnostics or the Admin link configuration:
- Test 1: BSCS 1ST R (Monday) Programming Fundamentals & Quantitative Reasoning-I
- Test 2: BSIT 7TH R (Monday) IT Infrastructure
- Test 3: BSCS 5TH SS1 (Wednesday) Operating Systems
- Test 4: 34 distinct classes parsed from sheet
- Test 5: 19 departments scope (1 Live, 18 Coming Soon)
- Test 6: `normalizeTeacher` function correctness
- Test 7: Clean unique teacher list
- Test 8: Teacher schedule lookup for Mr. Jahanzaib Ali Khan
- Test 9: Lecture schedule consistency across student and teacher views
- Test 10: Profile persistence across launches
