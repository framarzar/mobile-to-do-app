package com.framarzar.todo.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun startOfToday(): Long = LocalDate.now().toEpochMillis()

fun startOfTomorrow(): Long = LocalDate.now().plusDays(1).toEpochMillis()

fun LocalDate.toEpochMillis(time: LocalTime = LocalTime.MIDNIGHT): Long =
    atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

// Material date pickers report the selected day as UTC midnight.
fun datePickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
