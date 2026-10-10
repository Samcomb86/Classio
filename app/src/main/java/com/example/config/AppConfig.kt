package com.example.config

object AppConfig {
    /**
     * Default Google Sheet or Google Drive timetable link published by Thal University.
     * Can also be configured dynamically in the app's Admin screen (tapped 7 times on version).
     */
    const val DEFAULT_TIMETABLE_SOURCE_URL = ""
    const val AUTO_REFRESH_MINUTES = 30

    const val WHATSAPP_CHANNEL_URL = "https://whatsapp.com/channel/0029VaJwzEZ11ulHnliDcu1e"
    const val UNIVERSITY_WEBSITE_URL = "https://tu.edu.pk"

    /**
     * Converts Google Sheets or Google Drive links to direct downloadable CSV / export endpoints
     */
    fun normalizeSourceUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        // Case 1: Google Sheets URL: https://docs.google.com/spreadsheets/d/<ID>/edit...
        val sheetsMatch = Regex("""docs\.google\.com/spreadsheets/d/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (sheetsMatch != null) {
            val docId = sheetsMatch.groupValues[1]
            // Google visualization CSV endpoint works smoothly with public links
            return "https://docs.google.com/spreadsheets/d/$docId/gviz/tq?tqx=out:csv"
        }

        // Case 2: Google Drive File URL: https://drive.google.com/file/d/<ID>/view...
        val driveMatch = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (driveMatch != null) {
            val fileId = driveMatch.groupValues[1]
            return "https://drive.google.com/uc?export=download&id=$fileId"
        }

        return trimmed
    }
}
