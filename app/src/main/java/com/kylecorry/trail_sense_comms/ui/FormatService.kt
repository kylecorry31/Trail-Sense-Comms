package com.kylecorry.trail_sense_comms.ui

import android.content.Context
import android.text.format.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormatService @Inject constructor(@ApplicationContext private val context: Context) {
    fun formatDate(
        date: ZonedDateTime,
        includeWeekDay: Boolean = true,
        abbreviateMonth: Boolean = false
    ): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_DATE or (if (includeWeekDay) DateUtils.FORMAT_SHOW_WEEKDAY else 0) or DateUtils.FORMAT_SHOW_YEAR or (if (abbreviateMonth) DateUtils.FORMAT_ABBREV_MONTH else 0)
        )
    }

}