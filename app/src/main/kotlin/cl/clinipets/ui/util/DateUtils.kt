package cl.clinipets.ui.util

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))
private val fancyDateFormatter = DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM", Locale("es", "ES"))

fun OffsetDateTime.toLocalHour(): String =
    this.atZoneSameInstant(ZoneId.systemDefault()).format(hourFormatter)

fun OffsetDateTime.toLocalDateStr(): String =
    this.atZoneSameInstant(ZoneId.systemDefault()).format(dateFormatter)

fun OffsetDateTime.toLocalFancyDate(): String =
    this.atZoneSameInstant(ZoneId.systemDefault())
        .format(fancyDateFormatter)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
