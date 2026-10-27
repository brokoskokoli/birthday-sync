package io.github.brokoskokoli.birthdaysync.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.R
import io.github.brokoskokoli.birthdaysync.system.CheckId
import io.github.brokoskokoli.birthdaysync.system.Severity
import kotlinx.coroutines.delay

private class StepText(val emoji: String, @StringRes val title: Int, @StringRes val reason: Int, @StringRes val action: Int)

private val STEP_TEXTS = mapOf(
    CheckId.CONTACTS to StepText(
        "👥", R.string.onboarding_contacts_title, R.string.onboarding_contacts_text, R.string.action_allow,
    ),
    CheckId.CALENDAR to StepText(
        "📅", R.string.onboarding_calendar_title, R.string.onboarding_calendar_text, R.string.action_allow,
    ),
    CheckId.NOTIFICATIONS to StepText(
        "🔔", R.string.onboarding_notifications_title, R.string.onboarding_notifications_text, R.string.action_allow,
    ),
    CheckId.EXACT_ALARM to StepText(
        "⏰", R.string.onboarding_exact_alarm_title, R.string.onboarding_exact_alarm_text, R.string.action_open_setting,
    ),
    CheckId.BATTERY_OPTIMIZATION to StepText(
        "🔋", R.string.onboarding_battery_title, R.string.onboarding_battery_text, R.string.action_open_setting,
    ),
    CheckId.BACKGROUND_RESTRICTION to StepText(
        "🚫", R.string.onboarding_background_title, R.string.onboarding_background_text, R.string.action_open_app_info,
    ),
    CheckId.APP_HIBERNATION to StepText(
        "💤", R.string.onboarding_hibernation_title, R.string.onboarding_hibernation_text, R.string.action_open_setting,
    ),
)

private val OkGreen = Color(0xFF2E7D32)

/**
 * Begrüßung beim ersten Start: erklärt die App und fragt jede fehlende Berechtigung bzw. Einstellung
 * in einem eigenen Schritt mit Begründung ab. Erteilte Schritte gehen automatisch weiter.
 */
@Composable
fun OnboardingScreen(vm: MainViewModel, onAction: (CheckId) -> Unit, onFinish: () -> Unit) {
    val checks by vm.checks.collectAsStateWithLifecycle()
    // Schritte werden beim Verlassen der Begrüßung festgelegt: nur, was dann noch fehlt.
    var steps by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var attempted by rememberSaveable { mutableStateOf(false) }

    val stepIds = steps?.map { CheckId.valueOf(it) }
    val total = (stepIds?.size ?: 0) + 2
    val current: CheckId? = stepIds?.getOrNull(index - 1)
    val currentOk = current != null && checks.firstOrNull { it.id == current }?.ok == true

    fun goTo(i: Int) { index = i; attempted = false }

    BackHandler(enabled = index > 0) { goTo(index - 1) }

    // Nach Rückkehr aus Dialog/Einstellungen: erteilt → kurz bestätigen, dann weiter.
    LaunchedEffect(current, currentOk, attempted) {
        if (current != null && currentOk && attempted) {
            delay(700)
            goTo(index + 1)
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            if (steps != null) {
                LinearProgressIndicator(progress = { (index + 1f) / total }, modifier = Modifier.fillMaxWidth())
                Text(
                    pluralStringResource(R.plurals.onboarding_step, total, index + 1, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            AnimatedContent(
                targetState = index, label = "step",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
            ) { i ->
                val stepId = stepIds?.getOrNull(i - 1)
                when {
                    i == 0 -> StepBody(
                        "🎂", stringResource(R.string.onboarding_welcome_title), stringResource(R.string.onboarding_welcome_text),
                    )
                    stepId != null -> STEP_TEXTS.getValue(stepId).let { StepBody(it.emoji, stringResource(it.title), stringResource(it.reason)) }
                    else -> {
                        val open = checks.count { !it.ok && it.severity != Severity.INFO }
                        StepBody(
                            if (open == 0) "🎉" else "👍",
                            stringResource(if (open == 0) R.string.system_all_ready else R.string.onboarding_almost_title),
                            // Zwei eigenständige Absätze
                            (if (open == 0) stringResource(R.string.onboarding_all_granted)
                            else pluralStringResource(R.plurals.onboarding_missing, open, open, stringResource(R.string.tab_system))) +
                                "\n\n" + stringResource(R.string.onboarding_next_steps),
                        )
                    }
                }
            }

            // Statuszeile zum aktuellen Schritt
            if (current != null) {
                Text(
                    when {
                        currentOk -> stringResource(R.string.onboarding_granted)
                        attempted -> stringResource(R.string.onboarding_not_granted)
                        else -> ""
                    },
                    color = if (currentOk) OkGreen else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                when {
                    index == 0 -> {
                        Spacer(Modifier)
                        Button(
                            onClick = {
                                steps = checks.filter { !it.ok && it.severity != Severity.INFO && it.id in STEP_TEXTS }.map { it.id.name }
                                goTo(1)
                            },
                            // Prüfungen werden asynchron geladen; vorher ist nicht bekannt, welche Schritte nötig sind.
                            enabled = checks.isNotEmpty(),
                        ) { Text(stringResource(R.string.onboarding_start)) }
                    }
                    current != null -> {
                        TextButton(onClick = { goTo(index + 1) }) { Text(stringResource(if (currentOk) R.string.onboarding_next else R.string.onboarding_skip)) }
                        if (!currentOk) {
                            Button(onClick = { attempted = true; onAction(current) }) {
                                Text(stringResource(if (attempted) R.string.onboarding_retry else STEP_TEXTS.getValue(current).action))
                            }
                        }
                    }
                    else -> {
                        Spacer(Modifier)
                        Button(onClick = onFinish) { Text(stringResource(R.string.onboarding_finish)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBody(emoji: String, title: String, text: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
