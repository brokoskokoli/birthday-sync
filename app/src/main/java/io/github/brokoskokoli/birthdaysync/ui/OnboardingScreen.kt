package io.github.brokoskokoli.birthdaysync.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.system.CheckId
import io.github.brokoskokoli.birthdaysync.system.Severity
import kotlinx.coroutines.delay

private class StepText(val emoji: String, val title: String, val reason: String, val action: String)

private val STEP_TEXTS = mapOf(
    CheckId.CONTACTS to StepText(
        "👥", "Zugriff auf deine Kontakte",
        "Die Geburtstage stehen in deinen Kontakten. Damit die App sie findet, braucht sie Lesezugriff.\n\n" +
            "Gelesen werden nur Namen, Geburtstage und die Zuordnung zu Konten und Gruppen, auch im Hintergrund " +
            "beim regelmäßigen Abgleich. Die Daten bleiben auf dem Gerät: Die App ändert keine Kontakte, " +
            "hat keinen Internetzugriff und gibt nichts weiter.",
        "Erlauben",
    ),
    CheckId.CALENDAR to StepText(
        "📅", "Zugriff auf deinen Kalender",
        "Für jeden Geburtstag legt die App einen Termin in einem Kalender deiner Wahl an und hält ihn aktuell, " +
            "zum Beispiel wenn sich ein Geburtsdatum ändert.\n\n" +
            "Dafür muss sie Kalender lesen und schreiben. Verändert werden ausschließlich die eigenen Geburtstagstermine.",
        "Erlauben",
    ),
    CheckId.NOTIFICATIONS to StepText(
        "🔔", "Benachrichtigungen",
        "Auf Wunsch meldet sich die App zu einer festen Uhrzeit, wenn heute jemand Geburtstag hat.\n\n" +
            "Ohne diese Berechtigung bleibt die Meldung stumm.",
        "Erlauben",
    ),
    CheckId.EXACT_ALARM to StepText(
        "⏰", "Pünktliche Erinnerung",
        "Damit die tägliche Benachrichtigung genau zur eingestellten Uhrzeit kommt und nicht irgendwann später, " +
            "braucht die App die Erlaubnis für Wecker und Erinnerungen.\n\n" +
            "Aktiviere auf der folgenden Seite den Schalter für Birthday Sync und kehre dann mit \"Zurück\" hierher zurück.",
        "Einstellung öffnen",
    ),
    CheckId.BATTERY_OPTIMIZATION to StepText(
        "🔋", "Im Hintergrund arbeiten",
        "Android stellt Apps im Hintergrund gern ruhig, um Akku zu sparen. Dann kann der regelmäßige Abgleich ausfallen.\n\n" +
            "Der Abgleich dauert nur wenige Sekunden am Tag, der Mehrverbrauch ist kaum messbar. " +
            "Wähle in der folgenden Liste \"Alle Apps\", dann Birthday Sync und stelle \"Nicht optimieren\" ein.",
        "Einstellung öffnen",
    ),
    CheckId.BACKGROUND_RESTRICTION to StepText(
        "🚫", "Hintergrundbeschränkung aufheben",
        "Für diese App ist die Hintergrundaktivität eingeschränkt, dadurch fallen Abgleich und Benachrichtigung aus.\n\n" +
            "Öffne in der App-Info den Punkt \"Akku\" und wähle \"Nicht eingeschränkt\". Kehre dann mit \"Zurück\" hierher zurück.",
        "App-Info öffnen",
    ),
    CheckId.APP_HIBERNATION to StepText(
        "💤", "Nicht automatisch pausieren",
        "Öffnest du die App einige Monate nicht, pausiert Android sie und entzieht ihr die Berechtigungen. " +
            "Der Abgleich würde dann unbemerkt aufhören – obwohl die App ja gerade im Hintergrund arbeiten soll.\n\n" +
            "Schalte auf der folgenden Seite \"App-Aktivität pausieren, wenn sie nicht verwendet wird\" aus " +
            "und kehre dann mit \"Zurück\" hierher zurück.",
        "Einstellung öffnen",
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
                    "Schritt ${index + 1} von $total",
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
                        "🎂", "Willkommen bei Birthday Sync",
                        "Die App überträgt die Geburtstage aus deinen Kontakten automatisch in einen Kalender " +
                            "deiner Wahl und erinnert dich am Tag selbst.\n\n" +
                            "Damit das zuverlässig im Hintergrund klappt, braucht sie ein paar Berechtigungen. " +
                            "Wir gehen sie Schritt für Schritt durch und erklären jeweils, wofür sie gebraucht werden.\n\n" +
                            "Alles bleibt auf deinem Gerät – die App hat keinen Internetzugriff.",
                    )
                    stepId != null -> STEP_TEXTS.getValue(stepId).let { StepBody(it.emoji, it.title, it.reason) }
                    else -> {
                        val open = checks.count { !it.ok && it.severity != Severity.INFO }
                        StepBody(
                            if (open == 0) "🎉" else "👍",
                            if (open == 0) "Alles bereit" else "Fast geschafft",
                            (if (open == 0) "Alle Berechtigungen sind erteilt.\n\n"
                            else "$open Punkt(e) fehlen noch. Du kannst sie jederzeit im Tab \"System\" nachholen.\n\n") +
                                "So geht es weiter:\n" +
                                "1. Kontaktlisten auswählen\n" +
                                "2. Zielkalender wählen\n" +
                                "3. Fertig – den Rest erledigt die App automatisch.",
                        )
                    }
                }
            }

            // Statuszeile zum aktuellen Schritt
            if (current != null) {
                Text(
                    when {
                        currentOk -> "✓ Erteilt"
                        attempted -> "Noch nicht erteilt. Du kannst es erneut versuchen oder den Schritt überspringen."
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
                        ) { Text("Los geht's") }
                    }
                    current != null -> {
                        TextButton(onClick = { goTo(index + 1) }) { Text(if (currentOk) "Weiter" else "Überspringen") }
                        if (!currentOk) {
                            Button(onClick = { attempted = true; onAction(current) }) {
                                Text(if (attempted) "Erneut versuchen" else STEP_TEXTS.getValue(current).action)
                            }
                        }
                    }
                    else -> {
                        Spacer(Modifier)
                        Button(onClick = onFinish) { Text("Zur App") }
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
