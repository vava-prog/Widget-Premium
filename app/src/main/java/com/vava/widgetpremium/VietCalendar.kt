package com.vava.widgetpremium

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Ngày lễ và sự kiện, tên hiển thị lấy từ chuỗi tài nguyên ở lớp giao diện. */
enum class Direction { N, NE, E, SE, S, SW, W, NW }

enum class Holiday {
    NEW_YEAR, PARTY_FOUNDING, VALENTINE, WOMEN_INTL, LIBERATION, LABOUR, CHILDREN, NATIONAL,
    WOMEN_VN, TEACHERS, CHRISTMAS,
    TET, TET_2, TET_3, FIRST_FULL_MOON, HAN_THUC, HUNG_KINGS, BUDDHA, DOAN_NGO, VU_LAN, MID_AUTUMN, KITCHEN_GODS
}

/** Phần tính toán thuần của tab Lịch Việt: can chi ngày, hoàng đạo, giờ hoàng đạo, ngày lễ, lưới tháng. */
object VietCalendar {
    val CAN = listOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    val CHI = listOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    /** Số ngày Julius (nửa đêm) của ngày dương. 1970-01-01 là 2440588. */
    fun julianDay(date: LocalDate): Long = date.toEpochDay() + 2440588L

    fun dayCanIndex(date: LocalDate): Int = ((julianDay(date) + 9) % 10).toInt()
    fun dayChiIndex(date: LocalDate): Int = ((julianDay(date) + 1) % 12).toInt()

    /** Ví dụ 2000-01-01 là ngày "Mậu Ngọ". */
    fun dayName(date: LocalDate): String = CAN[dayCanIndex(date)] + " " + CHI[dayChiIndex(date)]

    /** Chi của tháng âm lịch (tháng 1 là Dần). */
    private val STAR_START = intArrayOf(0, 2, 4, 6, 8, 10) // Tý, Dần, Thìn, Ngọ, Thân, Tuất

    /**
     * Ngày hoàng đạo theo bảng 12 sao: Thanh Long khởi ở chi phụ thuộc tháng âm lịch,
     * các sao tốt là Thanh Long, Minh Đường, Kim Quỹ, Thiên Đức, Ngọc Đường, Tư Mệnh.
     */
    fun isHoangDao(lunarMonth: Int, dayChi: Int): Boolean {
        val start = STAR_START[(lunarMonth - 1) % 6]
        val star = ((dayChi - start) % 12 + 12) % 12
        return star in GOOD_STARS
    }

    private val GOOD_STARS = setOf(0, 1, 4, 5, 7, 10)

    private val GOOD_HOURS = listOf(
        listOf(0, 1, 3, 6, 8, 9),   // ngày Tý, Ngọ
        listOf(2, 3, 5, 8, 10, 11), // ngày Sửu, Mùi
        listOf(0, 1, 4, 5, 7, 10),  // ngày Dần, Thân
        listOf(0, 2, 3, 6, 7, 9),   // ngày Mão, Dậu
        listOf(2, 4, 5, 8, 9, 11),  // ngày Thìn, Tuất
        listOf(1, 4, 6, 7, 10, 11)  // ngày Tỵ, Hợi
    )

    /** Sáu chi giờ hoàng đạo (0 = Tý ... 11 = Hợi) của một ngày, theo chi của ngày. */
    fun goodHours(dayChi: Int): List<Int> = GOOD_HOURS[dayChi % 6]

    /** Giờ bắt đầu và kết thúc (0 đến 23) của một chi giờ. Tý là 23 giờ đến 1 giờ. */
    fun hourRange(chiIndex: Int): Pair<Int, Int> {
        val start = (chiIndex * 2 + 23) % 24
        return start to (start + 2) % 24
    }

    /** Chi giờ chứa giờ đồng hồ đã cho (23 và 0 là Tý). */
    fun hourChi(hour: Int): Int = ((hour + 1) / 2) % 12

    /** 24 tiết khí, chỉ số 0 là Xuân phân (kinh độ Mặt Trời 0 độ), mỗi tiết cách nhau 15 độ. */
    val TERMS = listOf(
        "Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng", "Hạ chí", "Tiểu thử",
        "Đại thử", "Lập thu", "Xử thử", "Bạch lộ", "Thu phân", "Hàn lộ", "Sương giáng", "Lập đông",
        "Tiểu tuyết", "Đại tuyết", "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ thủy", "Kinh trập"
    )

    val TRUC = listOf("Kiến", "Trừ", "Mãn", "Bình", "Định", "Chấp", "Phá", "Nguy", "Thành", "Thu", "Khai", "Bế")

    /** Tiết khí đang có hiệu lực vào cuối ngày (tiết bắt đầu trong ngày này thì tính là tiết mới). */
    fun solarTermIndex(date: LocalDate): Int =
        Math.floorDiv(LunarCalendar.sunLongitudeDegrees((julianDay(date) + 1).toInt()).toInt(), 15).coerceIn(0, 23)

    private fun termIndexAtStart(date: LocalDate): Int =
        Math.floorDiv(LunarCalendar.sunLongitudeDegrees(julianDay(date).toInt()).toInt(), 15).coerceIn(0, 23)

    /** Chỉ số tiết khí nếu một tiết khí bắt đầu vào đúng ngày này, ngược lại là null. */
    fun solarTermStartingOn(date: LocalDate): Int? {
        val end = solarTermIndex(date)
        return if (end != termIndexAtStart(date)) end else null
    }

    /** Chi của tháng tiết khí (tháng Dần bắt đầu ở Lập xuân), chỉ đổi ở các "tiết" có chỉ số lẻ. */
    fun termMonthChi(termIndex: Int): Int {
        val t = if (termIndex % 2 == 1) termIndex else (termIndex + 23) % 24
        return (4 + (t - 1) / 2) % 12
    }

    /** Chỉ số 12 Trực của ngày: ngày có chi trùng chi của tháng tiết khí là Kiến; ngày đổi tháng lặp lại Trực của hôm trước. */
    fun trucIndex(date: LocalDate): Int =
        ((dayChiIndex(date) - termMonthChi(solarTermIndex(date))) % 12 + 12) % 12

    /** Can chi của tháng âm lịch (tháng 1 là Dần; can tháng tính theo can năm). Ví dụ tháng 10 năm Bính Ngọ là Kỷ Hợi. */
    fun lunarMonthName(lunar: LunarDate): String {
        val yearCan = (lunar.year + 6) % 10
        val can = (yearCan * 2 + lunar.month + 1) % 10
        val chi = (lunar.month + 1) % 12
        return CAN[can] + " " + CHI[chi]
    }

    /** Can chi của một canh giờ trong ngày (can giờ Tý phụ thuộc can ngày). */
    fun hourName(date: LocalDate, chiIndex: Int): String =
        CAN[(dayCanIndex(date) * 2 + chiIndex) % 10] + " " + CHI[chiIndex]

    /** Tiết khí kế tiếp sau ngày đã cho (tối đa 20 ngày tới): chỉ số tiết và ngày bắt đầu. */
    fun nextTerm(date: LocalDate): Pair<Int, LocalDate>? {
        for (i in 1..20) {
            val d = date.plusDays(i.toLong())
            solarTermStartingOn(d)?.let { return it to d }
        }
        return null
    }

    // Hỷ thần và Tài thần theo can của ngày (Giáp, Ất, Bính, Đinh, Mậu, Kỷ, Canh, Tân, Nhâm, Quý).
    // Nguồn: Đặng Xuân Xuyến, "Cách tính phương vị xuất hành" (t-van.net); đối chiếu ngày Giáp Thân và Mậu Tuất trên quantrimang.com.
    private val HY_THAN = listOf(
        Direction.NE, Direction.NW, Direction.SW, Direction.S, Direction.SE,
        Direction.NE, Direction.NW, Direction.SW, Direction.S, Direction.SE
    )
    private val TAI_THAN = listOf(
        Direction.SE, Direction.SE, Direction.E, Direction.E, Direction.N,
        Direction.S, Direction.SW, Direction.SW, Direction.W, Direction.NW
    )

    fun hyThan(date: LocalDate): Direction = HY_THAN[dayCanIndex(date)]
    fun taiThan(date: LocalDate): Direction = TAI_THAN[dayCanIndex(date)]

    /** Một trong 28 sao (Nhị thập bát tú): tên, hành (bảy chiếu) và con vật. */
    data class Mansion(val name: String, val element: String, val animal: String)

    val MANSIONS = listOf(
        Mansion("Giác", "Mộc", "Giao"), Mansion("Cang", "Kim", "Long"), Mansion("Đê", "Thổ", "Lạc"),
        Mansion("Phòng", "Nhật", "Thố"), Mansion("Tâm", "Nguyệt", "Hồ"), Mansion("Vĩ", "Hỏa", "Hổ"),
        Mansion("Cơ", "Thủy", "Báo"), Mansion("Đẩu", "Mộc", "Giải"), Mansion("Ngưu", "Kim", "Ngưu"),
        Mansion("Nữ", "Thổ", "Bức"), Mansion("Hư", "Nhật", "Thử"), Mansion("Nguy", "Nguyệt", "Yến"),
        Mansion("Thất", "Hỏa", "Trư"), Mansion("Bích", "Thủy", "Dũ"), Mansion("Khuê", "Mộc", "Lang"),
        Mansion("Lâu", "Kim", "Cẩu"), Mansion("Vị", "Thổ", "Trĩ"), Mansion("Mão", "Nhật", "Kê"),
        Mansion("Tất", "Nguyệt", "Ô"), Mansion("Chủy", "Hỏa", "Hầu"), Mansion("Sâm", "Thủy", "Viên"),
        Mansion("Tỉnh", "Mộc", "Hãn"), Mansion("Quỷ", "Kim", "Dương"), Mansion("Liễu", "Thổ", "Chương"),
        Mansion("Tinh", "Nhật", "Mã"), Mansion("Trương", "Nguyệt", "Lộc"), Mansion("Dực", "Hỏa", "Xà"),
        Mansion("Chẩn", "Thủy", "Dẫn")
    )

    /**
     * Sao trực ngày theo chu kỳ liên tục 28 ngày (không đổi theo tháng). Mỗi sao gắn với một thứ trong tuần
     * (Giác là thứ Năm, Tất là thứ Hai). Đối chiếu quantrimang.com: 28/7/3307 và 31/12/2708 là Giác, 23/11/3339 là Tất.
     */
    fun mansionIndex(date: LocalDate): Int = Math.floorMod(date.toEpochDay() + 7, 28L).toInt()

    fun mansion(date: LocalDate): Mansion = MANSIONS[mansionIndex(date)]

    /** Chi xung với ngày (lục xung): cách 6 chi, ví dụ ngày Tý xung tuổi Ngọ. */
    fun clashChi(date: LocalDate): Int = (dayChiIndex(date) + 6) % 12

    val LUC_DIEU = listOf("Đại an", "Lưu niên", "Tốc hỷ", "Xích khẩu", "Tiểu cát", "Không vong")

    /**
     * Khổng Minh Lục diệu của ngày: mùng 1 tháng Giêng là Đại an, mỗi ngày và mỗi tháng tiến một cung.
     * Chỉ số = (tháng - 1 + ngày - 1) mod 6. Nguồn: tuvikhoahoc.vn và amlich.net; tháng nhuận tính theo số tháng gốc.
     */
    fun lucDieuIndex(lunar: LunarDate): Int = ((lunar.month - 1) + (lunar.day - 1)) % 6

    /** Nguyệt kỵ: mùng 5, 14, 23 âm lịch. */
    fun isNguyetKy(lunar: LunarDate): Boolean = lunar.day == 5 || lunar.day == 14 || lunar.day == 23

    /** Tam nương: mùng 3, 7, 13, 18, 22, 27 âm lịch. */
    fun isTamNuong(lunar: LunarDate): Boolean = lunar.day in setOf(3, 7, 13, 18, 22, 27)

    private val SOLAR = mapOf(
        (1 to 1) to Holiday.NEW_YEAR, (3 to 2) to Holiday.PARTY_FOUNDING, (14 to 2) to Holiday.VALENTINE,
        (8 to 3) to Holiday.WOMEN_INTL, (30 to 4) to Holiday.LIBERATION, (1 to 5) to Holiday.LABOUR,
        (1 to 6) to Holiday.CHILDREN, (2 to 9) to Holiday.NATIONAL, (20 to 10) to Holiday.WOMEN_VN,
        (20 to 11) to Holiday.TEACHERS, (25 to 12) to Holiday.CHRISTMAS
    )

    private val LUNAR = mapOf(
        (1 to 1) to Holiday.TET, (2 to 1) to Holiday.TET_2, (3 to 1) to Holiday.TET_3,
        (15 to 1) to Holiday.FIRST_FULL_MOON, (3 to 3) to Holiday.HAN_THUC, (10 to 3) to Holiday.HUNG_KINGS,
        (15 to 4) to Holiday.BUDDHA, (5 to 5) to Holiday.DOAN_NGO, (15 to 7) to Holiday.VU_LAN,
        (15 to 8) to Holiday.MID_AUTUMN, (23 to 12) to Holiday.KITCHEN_GODS
    )

    fun holidays(date: LocalDate, lunar: LunarDate): List<Holiday> {
        val list = ArrayList<Holiday>(2)
        SOLAR[date.dayOfMonth to date.monthValue]?.let { list.add(it) }
        if (!lunar.isLeap) LUNAR[lunar.day to lunar.month]?.let { list.add(it) }
        return list
    }

    fun lunarOf(date: LocalDate): LunarDate = LunarCalendar.fromSolar(date.dayOfMonth, date.monthValue, date.year)

    /** Bảy ngày của tuần chứa `date`, từ thứ Hai đến Chủ nhật. */
    fun weekDays(date: LocalDate): List<LocalDate> {
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return List(7) { monday.plusDays(it.toLong()) }
    }

    /** Nhãn ngày âm trong dải tuần: mùng 1 ghi kèm tháng ("1/10"), ngày khác chỉ ghi số ngày. */
    fun weekLunarLabel(date: LocalDate): String {
        val l = lunarOf(date)
        return if (l.day == 1) "${l.day}/${l.month}" else l.day.toString()
    }

    /** Lưới tháng, tuần bắt đầu từ thứ Hai; ô trống là null. Số ô luôn chia hết cho 7. */
    fun monthCells(month: YearMonth): List<LocalDate?> {
        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = ArrayList<LocalDate?>()
        repeat(offset) { cells.add(null) }
        for (d in 1..month.lengthOfMonth()) cells.add(month.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    /** Các ngày lễ trong một tháng dương, theo thứ tự ngày. */
    fun monthEvents(month: YearMonth): List<Pair<LocalDate, Holiday>> {
        val out = ArrayList<Pair<LocalDate, Holiday>>()
        for (d in 1..month.lengthOfMonth()) {
            val date = month.atDay(d)
            holidays(date, lunarOf(date)).forEach { out.add(date to it) }
        }
        return out
    }
}

/** Quy tắc cho ghi chú và giờ nhắc (thuần, có test). */
object ReminderRules {
    /** Giờ nhắc hợp lệ khi nằm sau thời điểm hiện tại. */
    fun isValid(remindAtMs: Long, nowMs: Long): Boolean = remindAtMs > nowMs

    /** Giờ nhắc gợi ý: hôm nay thì sau 1 giờ (làm tròn lên phút), ngày khác thì 08:00. */
    fun defaultTime(day: LocalDate, now: LocalDateTime): LocalDateTime =
        if (day == now.toLocalDate()) now.plusHours(1).withSecond(0).withNano(0) else day.atTime(8, 0)

    fun dateKey(day: LocalDate): String = day.toString()
}

/** Nội dung ô thời tiết của dải tuần: biểu tượng và "thấp° cao°"; thiếu dữ liệu thì rỗng. */
object WeekStrip {
    private fun day(date: java.time.LocalDate, days: List<DayForecast>): DayForecast? =
        days.firstOrNull { it.date == date.toString() }

    fun icon(date: java.time.LocalDate, days: List<DayForecast>): String =
        day(date, days)?.let { Condition.fromWmo(it.code).emoji } ?: ""

    fun range(date: java.time.LocalDate, days: List<DayForecast>): String {
        val d = day(date, days) ?: return ""
        if (d.minC.isNaN() || d.maxC.isNaN()) return ""
        return "${Math.round(d.minC)}° ${Math.round(d.maxC)}°"
    }
}
