package com.vava.widgetpremium

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private const val PAGES = 2400
private const val BASE_PAGE = PAGES / 2
private val CELL_HEIGHT = 54.dp

private fun holidayRes(h: Holiday): Int = when (h) {
    Holiday.NEW_YEAR -> R.string.holiday_new_year
    Holiday.PARTY_FOUNDING -> R.string.holiday_party_founding
    Holiday.VALENTINE -> R.string.holiday_valentine
    Holiday.WOMEN_INTL -> R.string.holiday_women_intl
    Holiday.LIBERATION -> R.string.holiday_liberation
    Holiday.LABOUR -> R.string.holiday_labour
    Holiday.CHILDREN -> R.string.holiday_children
    Holiday.NATIONAL -> R.string.holiday_national
    Holiday.WOMEN_VN -> R.string.holiday_women_vn
    Holiday.TEACHERS -> R.string.holiday_teachers
    Holiday.CHRISTMAS -> R.string.holiday_christmas
    Holiday.TET -> R.string.holiday_tet
    Holiday.TET_2 -> R.string.holiday_tet_2
    Holiday.TET_3 -> R.string.holiday_tet_3
    Holiday.FIRST_FULL_MOON -> R.string.holiday_first_full_moon
    Holiday.HAN_THUC -> R.string.holiday_han_thuc
    Holiday.HUNG_KINGS -> R.string.holiday_hung_kings
    Holiday.BUDDHA -> R.string.holiday_buddha
    Holiday.DOAN_NGO -> R.string.holiday_doan_ngo
    Holiday.VU_LAN -> R.string.holiday_vu_lan
    Holiday.MID_AUTUMN -> R.string.holiday_mid_autumn
    Holiday.KITCHEN_GODS -> R.string.holiday_kitchen_gods
}

private fun directionRes(d: Direction): Int = when (d) {
    Direction.N -> R.string.dir_n
    Direction.NE -> R.string.dir_ne
    Direction.E -> R.string.dir_e
    Direction.SE -> R.string.dir_se
    Direction.S -> R.string.dir_s
    Direction.SW -> R.string.dir_sw
    Direction.W -> R.string.dir_w
    Direction.NW -> R.string.dir_nw
}

private fun toMillis(t: LocalDateTime): Long = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
private fun fromMillis(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
private val REMIND_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val notes by remember { NoteRepository.observe(context) }.collectAsState(initial = emptyList())
    val pagerState = rememberPagerState(initialPage = BASE_PAGE) { PAGES }
    var selected by remember { mutableStateOf(today) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    val month = YearMonth.from(today).plusMonths((pagerState.currentPage - BASE_PAGE).toLong())
    LaunchedEffect(pagerState.currentPage) {
        selected = if (YearMonth.from(selected) == month) selected
        else if (YearMonth.from(today) == month) today else month.atDay(1)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, R.string.notes_perm_denied, Toast.LENGTH_LONG).show()
    }

    val daysWithNotes = remember(notes) { notes.map { it.dateKey }.toSet() }
    val dayNotes = notes.filter { it.dateKey == ReminderRules.dateKey(selected) }

    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                Text("‹", fontSize = 24.sp)
            }
            Text(
                stringResource(R.string.cal_month_fmt, month.monthValue, month.year),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                Text("›", fontSize = 24.sp)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf(
                R.string.cal_week_mon, R.string.cal_week_tue, R.string.cal_week_wed, R.string.cal_week_thu,
                R.string.cal_week_fri, R.string.cal_week_sat, R.string.cal_week_sun
            ).forEach {
                Text(
                    stringResource(it), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(CELL_HEIGHT * 6)) { page ->
            val pageMonth = YearMonth.from(today).plusMonths((page - BASE_PAGE).toLong())
            MonthGrid(pageMonth, today, selected, daysWithNotes) { selected = it }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.cal_swipe_hint), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(BASE_PAGE) }
                selected = today
            }) { Text(stringResource(R.string.cal_today)) }
        }

        DayDetail(selected, today)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (dayNotes.isEmpty()) {
                    Text(stringResource(R.string.notes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                dayNotes.forEach { note ->
                    NoteRow(
                        note,
                        onToggle = { scope.launch { NoteRepository.toggleDone(context, note) } },
                        onEdit = { editing = note; showDialog = true },
                        onDelete = { deleting = note }
                    )
                }
                Button(onClick = { editing = null; showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.notes_add))
                }
                ReminderStatus()
            }
        }

        MonthEvents(month)
    }

    if (showDialog) {
        NoteDialog(
            day = selected,
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { note ->
                showDialog = false
                scope.launch { NoteRepository.save(context, note) }
                if (note.remindAtMs != null) {
                    Toast.makeText(context, R.string.notes_saved_reminder, Toast.LENGTH_SHORT).show()
                    if (!ReminderScheduler.hasNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    Toast.makeText(context, R.string.notes_saved, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.notes_delete_title)) },
            text = { Text(note.text) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { NoteRepository.delete(context, note) }
                }) { Text(stringResource(R.string.notes_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.notes_cancel)) } }
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth, today: LocalDate, selected: LocalDate, daysWithNotes: Set<String>, onPick: (LocalDate) -> Unit
) {
    val cells = remember(month) { VietCalendar.monthCells(month) }
    Column(Modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().height(CELL_HEIGHT)) {
                week.forEach { date ->
                    if (date == null) {
                        Box(Modifier.weight(1f))
                    } else {
                        DayCell(
                            date, date == selected, date == today,
                            ReminderRules.dateKey(date) in daysWithNotes,
                            Modifier.weight(1f)
                        ) { onPick(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, isToday: Boolean, hasNote: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val lunar = remember(date) { VietCalendar.lunarOf(date) }
    val special = remember(date) { VietCalendar.holidays(date, lunar).isNotEmpty() }
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .padding(2.dp)
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .then(if (isToday) Modifier.border(BorderStroke(2.dp, scheme.primary), shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                fontWeight = FontWeight.SemiBold,
                color = if (special) scheme.error else scheme.onSurface
            )
            Text(
                if (lunar.day == 1) "1/${lunar.month}" else lunar.day.toString(),
                fontSize = 10.sp,
                color = scheme.onSurfaceVariant
            )
        }
        if (hasNote) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(6.dp).clip(CircleShape).background(scheme.tertiary)
            )
        }
    }
}

@Composable
private fun DayDetail(date: LocalDate, today: LocalDate) {
    val lunar = remember(date) { VietCalendar.lunarOf(date) }
    val holidays = remember(date) { VietCalendar.holidays(date, lunar) }
    val chi = VietCalendar.dayChiIndex(date)
    val good = VietCalendar.isHoangDao(lunar.month, chi)
    val goodHours = remember(date) { VietCalendar.goodHours(chi) }
    val nowChi = if (date == today) VietCalendar.hourChi(LocalDateTime.now().hour) else -1
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.cal_detail_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$weekday, ${date.dayOfMonth}/${date.monthValue}/${date.year}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(
                    R.string.cal_lunar_fmt, lunar.day, lunar.month,
                    if (lunar.isLeap) stringResource(R.string.cal_leap_suffix) else "",
                    LunarCalendar.yearName(lunar.year)
                )
            )
            Text(stringResource(R.string.cal_day_name, VietCalendar.dayName(date)))
            Text(
                stringResource(if (good) R.string.cal_good_day else R.string.cal_bad_day),
                fontWeight = FontWeight.SemiBold,
                color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            holidays.forEach { Text("★ " + stringResource(holidayRes(it)), color = MaterialTheme.colorScheme.error) }
            val termStart = remember(date) { VietCalendar.solarTermStartingOn(date) }
            Text(
                if (termStart != null) stringResource(R.string.cal_term_today, VietCalendar.TERMS[termStart])
                else stringResource(R.string.cal_term_current, VietCalendar.TERMS[VietCalendar.solarTermIndex(date)])
            )
            Text(stringResource(R.string.cal_truc, VietCalendar.TRUC[VietCalendar.trucIndex(date)]))
            Text(
                stringResource(
                    R.string.cal_directions,
                    stringResource(directionRes(VietCalendar.hyThan(date))),
                    stringResource(directionRes(VietCalendar.taiThan(date)))
                )
            )
            if (VietCalendar.isNguyetKy(lunar)) Text(stringResource(R.string.cal_nguyet_ky), color = MaterialTheme.colorScheme.error)
            if (VietCalendar.isTamNuong(lunar)) Text(stringResource(R.string.cal_tam_nuong), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.cal_good_hours), style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                HourDial(
                    good = goodHours,
                    currentChi = nowChi,
                    centerLines = listOf(
                        stringResource(R.string.cal_year_line, LunarCalendar.yearName(lunar.year)),
                        stringResource(R.string.cal_month_line, VietCalendar.lunarMonthName(lunar)),
                        stringResource(R.string.cal_day_name, VietCalendar.dayName(date))
                    )
                )
            }
            Text(
                stringResource(R.string.cal_dial_legend),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            (0..11).chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { h ->
                        val (from, to) = VietCalendar.hourRange(h)
                        val isGood = h in goodHours
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (h == nowChi) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary), RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .background(if (isGood) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(VietCalendar.hourName(date, h), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            Text("$from–$to", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            Text(
                                stringResource(if (isGood) R.string.cal_hour_good else R.string.cal_hour_bad),
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            val next = remember(date) { VietCalendar.nextTerm(date) }
            if (next != null) {
                Text(
                    stringResource(
                        R.string.cal_next_term, VietCalendar.TERMS[next.first],
                        "%02d/%02d".format(next.second.dayOfMonth, next.second.monthValue),
                        java.time.temporal.ChronoUnit.DAYS.between(date, next.second).toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = note.done, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                note.text,
                textDecoration = if (note.done) TextDecoration.LineThrough else null,
                color = if (note.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            note.remindAtMs?.let {
                Text(
                    "⏰ " + stringResource(R.string.notes_remind_at, REMIND_FORMAT.format(fromMillis(it))),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onEdit) { Text(stringResource(R.string.notes_edit_btn)) }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.notes_delete)) }
    }
}

@Composable
private fun MonthEvents(month: YearMonth) {
    val events = remember(month) { VietCalendar.monthEvents(month) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.cal_events_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (events.isEmpty()) Text(stringResource(R.string.cal_no_events), color = MaterialTheme.colorScheme.onSurfaceVariant)
            events.forEach { (date, holiday) ->
                val lunar = VietCalendar.lunarOf(date)
                Text("%02d/%02d (%d/%d ÂL) · ".format(date.dayOfMonth, date.monthValue, lunar.day, lunar.month) +
                    stringResource(holidayRes(holiday)))
            }
        }
    }
}

@Composable
private fun NoteDialog(day: LocalDate, initial: Note?, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var remind by remember { mutableStateOf(initial?.remindAtMs != null) }
    var time by remember {
        mutableStateOf(
            initial?.remindAtMs?.let { fromMillis(it) } ?: ReminderRules.defaultTime(day, LocalDateTime.now())
        )
    }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.notes_new else R.string.notes_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it; error = null },
                    placeholder = { Text(stringResource(R.string.notes_hint)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    keyboardOptions = KeyboardOptions.Default
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.notes_remind_switch), Modifier.weight(1f))
                    Switch(checked = remind, onCheckedChange = { remind = it; error = null })
                }
                if (remind) {
                    Text(stringResource(R.string.notes_remind_at, REMIND_FORMAT.format(time)))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                time = time.withYear(y).withMonth(m + 1).withDayOfMonth(d); error = null
                            }, time.year, time.monthValue - 1, time.dayOfMonth).show()
                        }) { Text(stringResource(R.string.notes_pick_date)) }
                        OutlinedButton(onClick = {
                            TimePickerDialog(context, { _, h, min ->
                                time = time.withHour(h).withMinute(min); error = null
                            }, time.hour, time.minute, true).show()
                        }) { Text(stringResource(R.string.notes_pick_time)) }
                    }
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = text.trim()
                val at = if (remind) toMillis(time) else null
                when {
                    trimmed.isEmpty() -> error = R.string.notes_err_empty
                    at != null && !ReminderRules.isValid(at, System.currentTimeMillis()) -> error = R.string.notes_err_past
                    else -> onSave(
                        (initial ?: Note(dateKey = ReminderRules.dateKey(day), text = trimmed))
                            .copy(text = trimmed, remindAtMs = at)
                    )
                }
            }) { Text(stringResource(R.string.notes_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.notes_cancel)) } }
    )
}

/** Cho người dùng thấy vì sao nhắc có thể không hiện và cách khắc phục; có nút gửi thông báo thử. */
@Composable
private fun ReminderStatus() {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val notifOk = refresh >= 0 && ReminderScheduler.hasNotificationPermission(context)
    val exactOk = refresh >= 0 && ReminderScheduler.canExact(context)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!notifOk) {
            Text(stringResource(R.string.rs_notif_off), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rs_open_notif)) }
        }
        if (!exactOk) {
            Text(stringResource(R.string.rs_exact_off), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rs_allow_exact)) }
        }
        if (notifOk && exactOk) {
            Text(stringResource(R.string.rs_ok), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.rs_oem_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rs_open_app_settings)) }
        OutlinedButton(onClick = {
            ReminderNotifier.show(context, 0L, context.getString(R.string.rs_test_text))
            refresh++
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rs_test)) }
    }
}
