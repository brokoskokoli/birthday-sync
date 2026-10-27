package io.github.brokoskokoli.birthdaysync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.R
import io.github.brokoskokoli.birthdaysync.sync.Occurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun OverviewScreen(vm: MainViewModel, modifier: Modifier, openIssues: Int, onSetup: () -> Unit, onGoto: (Destination) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val running by vm.syncRunning.collectAsStateWithLifecycle()
    val nextSync by vm.nextPeriodicSync.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val calendars by vm.calendars.collectAsStateWithLifecycle()
    val target = calendars.firstOrNull { it.id == settings.calendarId }
    val context = LocalContext.current

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        if (openIssues > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.overview_setup_incomplete_title), style = MaterialTheme.typography.titleSmall)
                            Text(pluralStringResource(R.plurals.overview_setup_incomplete_text, openIssues, openIssues), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Button(onClick = onSetup, modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)) { Text(stringResource(R.string.overview_setup_now)) }
                }
            }
        }
        if (settings.selectedSources.isEmpty() || target == null) {
            item {
                SectionCard(stringResource(R.string.overview_first_steps)) {
                    if (settings.selectedSources.isEmpty()) {
                        InfoText(stringResource(R.string.overview_step_contacts))
                        OutlinedButton(onClick = { onGoto(Destination.CONTACTS) }, Modifier.padding(horizontal = 16.dp)) { Text(stringResource(R.string.overview_choose_contacts)) }
                    }
                    if (target == null) {
                        InfoText(stringResource(R.string.overview_step_calendar))
                        OutlinedButton(onClick = { onGoto(Destination.CALENDAR) }, Modifier.padding(horizontal = 16.dp)) { Text(stringResource(R.string.overview_choose_calendar)) }
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.section_sync)) {
                InfoText(
                    if (target != null) stringResource(R.string.overview_target_calendar, target.name)
                    else stringResource(R.string.overview_target_calendar_none),
                )
                val daily = settings.syncIntervalDays == 1
                InfoText(
                    when {
                        !settings.autoSyncEnabled -> stringResource(R.string.overview_auto_off)
                        nextSync > 0 -> stringResource(
                            if (daily) R.string.overview_auto_daily_next else R.string.overview_auto_weekly_next,
                            formatInstant(context, nextSync),
                        )
                        else -> stringResource(if (daily) R.string.overview_auto_daily else R.string.overview_auto_weekly)
                    },
                )
                if (status.lastSyncAt > 0) {
                    Text(
                        stringResource(R.string.overview_last_sync, formatInstant(context, status.lastSyncAt), status.lastSyncMessage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.lastSyncOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else InfoText(stringResource(R.string.overview_no_sync_yet))
                Button(onClick = vm::syncNow, enabled = !running, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (running) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (running) R.string.overview_syncing else R.string.action_sync_now))
                }
            }
        }
        item {
            Text(stringResource(R.string.overview_upcoming), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
        if (upcoming.isEmpty()) {
            item { InfoText(stringResource(R.string.overview_no_birthdays), Modifier.padding(0.dp)) }
        }
        items(upcoming.take(30), key = { it.key }) { UpcomingRow(it) }
    }
}

@Composable
private fun UpcomingRow(o: Occurrence) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), o.date)
    val whenText = when (days) {
        0L -> stringResource(R.string.upcoming_today)
        1L -> stringResource(R.string.upcoming_tomorrow)
        else -> pluralStringResource(R.plurals.upcoming_in_days, days.toInt(), days.toInt())
    }
    val day = formatDay(LocalContext.current, o.date)
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(o.birthday.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    o.age?.let { pluralStringResource(R.plurals.upcoming_date_turns, it, day, it) } ?: day,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                whenText,
                style = MaterialTheme.typography.labelLarge,
                color = if (days == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
    }
}
