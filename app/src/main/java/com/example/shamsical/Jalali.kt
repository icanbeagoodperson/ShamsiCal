package com.example.shamsical
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class J(val y: Int, val m: Int, val d: Int)

/** Jalali <-> Gregorian (algorithm from jalaali-js). */
object Jal {
    private val br = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)
    private class Cal(val leap: Int, val gy: Int, val march: Int)
    private fun div(a: Int, b: Int) = Math.floorDiv(a, b)
    private fun md(a: Int, b: Int) = Math.floorMod(a, b)

    private fun cal(jy: Int): Cal {
        var leapJ = -14; var jp = br[0]; var jump = 0
        for (i in 1 until br.size) {
            val jm = br[i]; jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(md(jump, 33), 4); jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(md(n, 33) + 3, 4)
        if (md(jump, 33) == 4 && jump - n == 4) leapJ++
        val gy = jy + 621
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = md(md(n + 1, 33) - 1, 4); if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    fun toJ(date: LocalDate): J {
        val gy = date.year; var jy = gy - 621; val r = cal(jy)
        var k = (date.toEpochDay() - LocalDate.of(gy, 3, r.march).toEpochDay()).toInt()
        if (k >= 0) { if (k <= 185) return J(jy, 1 + k / 31, k % 31 + 1); k -= 186 }
        else { jy--; k += 179; if (r.leap == 1) k++ }
        return J(jy, 7 + k / 30, k % 30 + 1)
    }

    fun fromJ(jy: Int, jm: Int, jd: Int): LocalDate {
        val r = cal(jy)
        return LocalDate.ofEpochDay(LocalDate.of(r.gy, 3, r.march).toEpochDay() + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1)
    }

    fun len(y: Int, m: Int) = if (m <= 6) 31 else if (m <= 11) 30 else if (cal(y).leap == 0) 30 else 29

    val months = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val days = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val short = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    /** Saturday = 0 ... Friday = 6 */
    fun wd(d: LocalDate) = (d.dayOfWeek.value + 1) % 7

    // Only fixed solar (Shamsi) holidays
    private val hol = mapOf(
        (1 to 1) to "نوروز", (1 to 2) to "نوروز", (1 to 3) to "نوروز", (1 to 4) to "نوروز",
        (1 to 12) to "روز جمهوری اسلامی", (1 to 13) to "سیزده‌بدر",
        (3 to 14) to "رحلت امام خمینی", (3 to 15) to "قیام ۱۵ خرداد",
        (11 to 22) to "پیروزی انقلاب اسلامی", (12 to 29) to "ملی شدن صنعت نفت")
    fun holiday(d: LocalDate): String? { val j = toJ(d); return hol[j.m to j.d] }
}

fun String.fa() = map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")
fun fullJ(d: LocalDate): String { val j = Jal.toJ(d); return "${Jal.days[Jal.wd(d)]} ${j.d} ${Jal.months[j.m - 1]} ${j.y}".fa() }
fun fullG(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)
fun parse(s: String): Int? { val p = s.trim().split(":"); val h = p.getOrNull(0)?.toIntOrNull() ?: return null; val m = p.getOrNull(1)?.toIntOrNull() ?: 0; return h * 60 + m }
