package io.github.brokoskokoli.birthdaysync.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.R
import io.github.brokoskokoli.birthdaysync.system.Severity

@Composable
fun NotifyScreen(vm: MainViewModel, modifier: Modifier, onOpenSystem: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val checks by vm.checks.collectAsStateWithLifecycle()
    val problems = checks.filter { !it.ok && it.severity != Severity.INFO }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        item {
            SectionCard(stringResource(R.string.notify_daily_title), subtitle = stringResource(R.string.notify_daily_subtitle)) {
                SwitchRow(
                    stringResource(R.string.notify_today_switch), s.dailyNotificationEnabled,
                    { on -> vm.update { it.copy(dailyNotificationEnabled = on) } },
                    subtitle = stringResource(R.string.notify_today_switch_sub),
                )
                TimeRow(
                    stringResource(R.string.notify_time), s.dailyNotificationMinuteOfDay,
                    { m -> vm.update { it.copy(dailyNotificationMinuteOfDay = m) } },
                    enabled = s.dailyNotificationEnabled,
                )
                OutlinedButton(onClick = { vm.testNotification() }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.notify_test))
                }
            }
        }
        if (problems.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.notify_hint_title)) {
                    InfoText(stringResource(R.string.notify_hint_text, formatList(LocalContext.current, problems.map { it.title })))
                    TextButton(onClick = onOpenSystem, Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.notify_go_system)) }
                }
            }
        }
    }
}
