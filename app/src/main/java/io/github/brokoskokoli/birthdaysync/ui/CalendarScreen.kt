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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.sync.BirthdayPlanner

private val ALL_DAY_REMINDERS = listOf(
    0 to "Am Tag um 00:00 Uhr",
    360 to "Am Vortag um 18:00 Uhr",
    540 to "Am Vortag um 15:00 Uhr",
    900 to "Am Vortag um 09:00 Uhr",
    2340 to "2 Tage vorher um 09:00 Uhr",
    9540 to "1 Woche vorher um 09:00 Uhr",
)
private val TIMED_REMINDERS = listOf(
    0 to "Zum Beginn", 10 to "10 Minuten vorher", 15 to "15 Minuten vorher", 30 to "30 Minuten vorher",
    60 to "1 Stunde vorher", 120 to "2 Stunden vorher", 1440 to "1 Tag vorher", 2880 to "2 Tage vorher",
    10080 to "1 Woche vorher",
)
private val DURATIONS = listOf(15, 30, 60, 120, 240, 480).map { it to formatDuration(it) }
private val RANGES = listOf(1 to "1 Monat", 2 to "2 Monate", 3 to "3 Monate", 6 to "6 Monate", 12 to "12 Monate")
private val INTERVALS = listOf(1 to "Täglich", 7 to "Wöchentlich")

@Composable
fun CalendarScreen(vm: MainViewModel, modifier: Modifier, onRequestPermission: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val calendars by vm.calendars.collectAsStateWithLifecycle()
    val hasPermission by vm.hasCalendar.collectAsStateWithLifecycle()
    val running by vm.syncRunning.collectAsStateWithLifecycle()
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(s.titleTemplate) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        item {
            SectionCard("Zielkalender", subtitle = "Hier werden die Geburtstagstermine angelegt") {
                if (!hasPermission) {
                    InfoText("Zugriff auf den Kalender fehlt.")
                    Button(onClick = onRequestPermission, Modifier.padding(horizontal = 16.dp)) { Text("Zugriff erlauben") }
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
                        Text("Eigenen Kalender \"Geburtstage\" anlegen")
                    }
                    InfoText("Ein eigener Kalender liegt nur auf dem Gerät und lässt sich in der Kalender-App getrennt ein- und ausblenden.")
                }
            }
        }
        item {
            SectionCard("Termine") {
                SwitchRow("Ganztägig", s.allDay, { allDay ->
                    vm.update { it.copy(allDay = allDay, eventReminderMinutes = if (allDay) 900 else 60) }
                })
                if (!s.allDay) {
                    TimeRow("Beginn", s.eventStartMinuteOfDay, { m -> vm.update { it.copy(eventStartMinuteOfDay = m) } })
                    ChoiceRow("Dauer", DURATIONS, s.eventDurationMinutes, { d -> vm.update { it.copy(eventDurationMinutes = d) } })
                }
                SwitchRow("Kalender-Erinnerung", s.eventReminderEnabled, { on -> vm.update { it.copy(eventReminderEnabled = on) } })
                if (s.eventReminderEnabled) {
                    ChoiceRow(
                        "Erinnern", if (s.allDay) ALL_DAY_REMINDERS else TIMED_REMINDERS, s.eventReminderMinutes,
                        { m -> vm.update { it.copy(eventReminderMinutes = m) } },
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { t -> title = t; vm.update { it.copy(titleTemplate = t) } },
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                InfoText(
                    "Platzhalter: {name}, {alter}. Beispiel: " +
                        BirthdayPlanner.renderTitle(title, "Anna Beispiel", 40),
                )
            }
        }
        item {
            SectionCard("Synchronisation") {
                SwitchRow("Automatisch synchronisieren", s.autoSyncEnabled, { on -> vm.update { it.copy(autoSyncEnabled = on) } })
                ChoiceRow("Häufigkeit", INTERVALS, s.syncIntervalDays, { d -> vm.update { it.copy(syncIntervalDays = d) } }, enabled = s.autoSyncEnabled)
                ChoiceRow("Zeitraum im Voraus", RANGES, s.syncRangeMonths, { m -> vm.update { it.copy(syncRangeMonths = m) } })
                InfoText(
                    "Geänderte Namen oder Geburtsdaten werden beim nächsten Sync übernommen; entfernte Kontakte " +
                        "verschwinden aus dem Kalender. Änderungen an diesen Einstellungen werden nach wenigen Sekunden angewendet.",
                )
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Button(onClick = vm::syncNow, enabled = !running) { Text(if (running) "Läuft …" else "Jetzt synchronisieren") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { confirmRemove = true }) { Text("Termine entfernen") }
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Alle Termine entfernen?") },
            text = {
                Text(
                    "Alle von Birthday Sync angelegten Termine werden aus allen Kalendern gelöscht. " +
                        "Ist der automatische Sync aktiv, legt er sie beim nächsten Lauf neu an.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmRemove = false; vm.removeAllEvents() }) { Text("Entfernen") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Abbrechen") } },
        )
    }
}
