package com.livelink

import android.text.Html
import android.text.Spanned
import com.google.firebase.Timestamp
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Funktion die basierend auf dem Userstatus (Int) die Bezeichnung des Userstatuses
// als String zurückgibt
fun getUserStatus(statusNumber: Int): String {
        return when (statusNumber) {
            0 -> "Mitglied"
            6 -> "Admin"
            8 -> "Bot"
            in 10..11 -> "Sysadmin"
            else -> "Mitglied"
        }
    }

// Funktion um einen Timestamp in eine lesbare Uhrzeit umzuwandeln
fun convertTimestampToTime(timestamp: Any): String {
    val rawTimestamp = timestamp as Timestamp
    val date = rawTimestamp.toDate()
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Berlin")
    }
    return dateFormat.format(date)
}

// Funktion um einen Timestamp in ein lesbares Datum umzuwandeln
fun convertTimestampToDate(timestamp: Any): String {
    val rawTimestamp = timestamp as Timestamp
    val date = rawTimestamp.toDate()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Berlin")
    }
    return dateFormat.format(date)
}

// Wandelt HTML String um, damit es in Android angezeigt werden kann
fun fromHtml(html: String): Spanned {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
}

// Funktion um nicht erlaubten HTML Code zu entfernen
fun cleanHTMLCode(wildspace: String): String {
    // Erlaubte HTML-Tags
    val allowedTags = Safelist.none()
        .addTags("b", "i", "u", "em", "strong", "br")

    // \n durch <br> ersetzen
    val wildspaceWithBr = wildspace.replace("\n", "<br>")

    // Zählen der <br>-Tags und Entfernen der zusätzlichen <br>-Tags
    var count = 0
    val limitedBrWildspace = wildspaceWithBr.replace(Regex("<br\\s*/?>")) {
        count++
        if (count > 10) "" else it.value
    }

    // Alle unerlaubten Tags entfernen
    val cleanedCode = Jsoup.clean(limitedBrWildspace, allowedTags)

    return cleanedCode
}

