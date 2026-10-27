package io.github.brokoskokoli.birthdaysync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.R
import io.github.brokoskokoli.birthdaysync.sync.BirthdayPlanner

// Erinnerung bei ganztägigen Terminen: Minuten vor Mitternacht des Termintags (900 = Vortag 09:00).
private val ALL_DAY_REMINDERS = listOf(0, 360, 540, 900, 2340, 9540)
private val TIMED_REMINDERS = listOf(0, 10, 15, 30, 60, 120, 1440, 2880, 10080)
private val DURATIONS = listOf(15, 30, 60, 120, 240, 480)
private val RANGES = listOf(1, 2, 3, 6, 12)

@Composable
fun CalendarScreen(vm: MainViewModel, modifier: Modifier, onRequestPermission: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val calendars by vm.calendars.collectAsStateWithLifecycle()
    val hasPermission by vm.hasCalendar.collectAsStateWithLifecycle()
    val running by vm.syncRunning.collectAsStateWithLifecycle()
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(s.titleTemplate) }
    val context = LocalContext.current
    val intervals = listOf(1 to stringResource(R.string.interval_daily), 7 to stringResource(R.string.interval_weekly))
    val ranges = RANGES.map { it to pluralStringResource(R.plurals.range_months, it, it) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        item {
            SectionCard(stringResource(R.string.calendar_target_title), subtitle = stringResource(R.string.calendar_target_subtitle)) {
                if (!hasPermission) {
                    InfoText(stringResource(R.string.calendar_no_permission))
                    Button(onClick = onRequestPermission, Modifier.padding(horizontal = 16.dp)) { Text(stringResource(R.string.action_allow_access)) }
                } else {
                    calendars.forEach { cal ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(cal.id == s.calendarId, role = Role.RadioButton) { vm.update { it.copy(calendarId = cal.id) } }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = cal.id == s.calendarId, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Spacer(Modifier.size(12.dp).clip(CircleShape).background(Color(cal.color)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cal.name, style = MaterialTheme.typography.bodyLarge)
                                Text(cal.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    OutlinedButton(onClick = { vm.createLocalCalendar() }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.calendar_create_local, stringResource(R.string.calendar_local_name)))
                    }
                    InfoText(stringResource(R.string.calendar_local_info))
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.calendar_events_title)) {
                SwitchRow(stringResource(R.string.calendar_all_day), s.allDay, { allDay ->
                    vm.update { it.copy(allDay = allDay, eventReminderMinutes = if (allDay) 900 else 60) }
                })
                if (!s.allDay) {
                    TimeRow(stringResource(R.string.calendar_start), s.eventStartMinuteOfDay, { m -> vm.update { it.copy(eventStartMinuteOfDay = m) } })
                    ChoiceRow(stringResource(R.string.calendar_duration), DURATIONS.map { it to formatDuration(context, it) }, s.eventDurationMinutes, { d -> vm.update { it.copy(eventDurationMinutes = d) } })
                }
                SwitchRow(stringResource(R.string.calendar_reminder), s.eventReminderEnabled, { on -> vm.update { it.copy(eventReminderEnabled = on) } })
                if (s.eventReminderEnabled) {
                    ChoiceRow(
                        stringResource(R.string.calendar_remind),
                        if (s.allDay) ALL_DAY_REMINDERS.map { it to formatAllDayReminder(context, it) }
                        else TIMED_REMINDERS.map { it to formatTimedReminder(context, it) },
                        s.eventReminderMinutes,
                        { m -> vm.update { it.copy(eventReminderMinutes = m) } },
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { t -> title = t; vm.update { it.copy(titleTemplate = t) } },
                    label = { Text(stringResource(R.string.calendar_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                InfoText(
                    stringResource(
                        R.string.calendar_title_help,
                        BirthdayPlanner.renderTitle(title, stringResource(R.string.calendar_example_name), 40),
                    ),
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.section_sync)) {
                SwitchRow(stringResource(R.string.calendar_auto_sync), s.autoSyncEnabled, { on -> vm.update { it.copy(autoSyncEnabled = on) } })
                ChoiceRow(stringResource(R.string.calendar_frequency), intervals, s.syncIntervalDays, { d -> vm.update { it.copy(syncIntervalDays = d) } }, enabled = s.autoSyncEnabled)
                ChoiceRow(stringResource(R.string.calendar_range), ranges, s.syncRangeMonths, { m -> vm.update { it.copy(syncRangeMonths = m) } })
                InfoText(
                    stringResource(R.string.calendar_sync_info),
                )
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Button(onClick = vm::syncNow, enabled = !running) { Text(stringResource(if (running) R.string.calendar_running else R.string.action_sync_now)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { confirmRemove = true }) { Text(stringResource(R.string.calendar_remove_events)) }
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.calendar_remove_confirm_title)) },
            text = { Text(stringResource(R.string.calendar_remove_confirm_text)) },
            confirmButton = { TextButton(onClick = { confirmRemove = false; vm.removeAllEvents() }) { Text(stringResource(R.string.action_remove)) } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}
