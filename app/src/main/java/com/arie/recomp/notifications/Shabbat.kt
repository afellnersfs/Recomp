package com.arie.recomp.notifications

import com.arie.recomp.data.Settings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Weekly quiet window: Friday evening through Saturday night.
 * Uses local sunset (candle lighting = sunset - 18 min, havdalah = sunset + 42 min)
 * when a location is configured, otherwise the fixed times from settings.
 * No reminder or notification is ever fired inside this window.
 */
object Shabbat {

    fun isInWindow(now: ZonedDateTime, s: Settings): Boolean {
        if (!s.shabbatEnabled) return false
        val (start, end) = windowFor(now, s)
        return !now.isBefore(start) && !now.isAfter(end)
    }

    /** The Shabbat window belonging to the week of [reference] (its most recent Friday). */
    fun windowFor(reference: ZonedDateTime, s: Settings): Pair<ZonedDateTime, ZonedDateTime> {
        val zone = reference.zone
        val friday = reference.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
        val saturday = friday.plusDays(1)

        if (s.shabbatUseSunset && (s.latitude != 0.0 || s.longitude != 0.0)) {
            val friSunset = Sunset.sunset(friday, s.latitude, s.longitude, zone)
            val satSunset = Sunset.sunset(saturday, s.latitude, s.longitude, zone)
            if (friSunset != null && satSunset != null) {
                return friSunset.minusMinutes(18) to satSunset.plusMinutes(42)
            }
        }
        val start = friday.atTime(parseTime(s.shabbatStart, LocalTime.of(17, 45))).atZone(zone)
        val end = saturday.atTime(parseTime(s.shabbatEnd, LocalTime.of(21, 30))).atZone(zone)
        return start to end
    }

    private fun parseTime(hm: String, fallback: LocalTime): LocalTime =
        runCatching { LocalTime.parse(hm) }.getOrDefault(fallback)
}

/** Sunset via the standard "Almanac for Computers" algorithm — good to a couple of minutes. */
object Sunset {

    fun sunset(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): ZonedDateTime? {
        val n = date.dayOfYear
        val lngHour = lon / 15.0
        val t = n + ((18.0 - lngHour) / 24.0)

        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(Math.toRadians(m))) + (0.020 * sin(2 * Math.toRadians(m))) + 282.634
        l = normalize(l, 360.0)

        var ra = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(l))))
        ra = normalize(ra, 360.0)
        ra += (floor(l / 90.0) * 90.0) - (floor(ra / 90.0) * 90.0)
        ra /= 15.0

        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(asin(sinDec))

        val zenith = 90.833  // official sunset
        val cosH = (cos(Math.toRadians(zenith)) - (sinDec * sin(Math.toRadians(lat)))) /
            (cosDec * cos(Math.toRadians(lat)))
        if (cosH < -1 || cosH > 1) return null  // polar day/night

        val h = Math.toDegrees(acos(cosH)) / 15.0
        val localMeanTime = h + ra - (0.06571 * t) - 6.622
        val ut = normalize(localMeanTime - lngHour, 24.0)

        val instant = date.atStartOfDay(ZoneOffset.UTC).plusSeconds((ut * 3600).toLong()).toInstant()
        return instant.atZone(zone)
    }

    private fun normalize(v: Double, range: Double): Double = ((v % range) + range) % range
}
