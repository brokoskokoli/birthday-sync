package io.github.brokoskokoli.birthdaysync.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import io.github.brokoskokoli.birthdaysync.BuildConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.system.CheckId
import io.github.brokoskokoli.birthdaysync.system.Severity
import io.github.brokoskokoli.birthdaysync.system.SystemCheck

private val OkGreen = Color(0xFF2E7D32)

@Composable
fun SystemScreen(vm: MainViewModel, modifier: Modifier, onAction: (CheckId) -> Unit, onSetup: () -> Unit) {
    val checks by vm.checks.collectAsStateWithLifecycle()
    val open = checks.count { !it.ok && it.severity != Severity.INFO }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        item {
            SectionCard(
                if (open == 0) "Alles bereit" else "$open Punkt(e) offen",
                subtitle = "Für zuverlässige Syncs im Hintergrund und pünktliche Benachrichtigungen",
            ) {
                InfoText("Die Einrichtung führt Schritt für Schritt durch alle fehlenden Punkte, jeweils mit Erklärung.")
                Button(onClick = onSetup, enabled = open > 0, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Einrichtung durchlaufen")
                }
            }
        }
        items(checks, key = { it.id }) { CheckCard(it, onAction) }
        item { AboutCard() }
    }
}

private const val REPO_URL = "https://github.com/brokoskokoli/birthday-sync"
private const val PRIVACY_URL = "https://brokoskokoli.github.io/birthday-sync/privacy.html"

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val open = { url: String ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        Unit
    }
    SectionCard("Über die App", subtitle = "Birthday Sync ${BuildConfig.VERSION_NAME}") {
        InfoText("Alle Daten bleiben auf dem Gerät. Die App hat keinen Internetzugriff und sammelt nichts.")
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { open(PRIVACY_URL) }) { Text("Datenschutz") }
            OutlinedButton(onClick = { open(REPO_URL) }) { Text("Quellcode") }
        }
    }
}

@Composable
private fun CheckCard(check: SystemCheck, onAction: (CheckId) -> Unit) {
    val (icon, tint) = when {
        check.severity == Severity.INFO && check.ok -> Icons.Filled.Info to MaterialTheme.colorScheme.onSurfaceVariant
        check.ok -> Icons.Filled.CheckCircle to OkGreen
        check.severity == Severity.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.tertiary
        else -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(check.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        check.severity == Severity.INFO -> check.description
                        check.ok -> "Erledigt. " + check.description
                        check.severity == Severity.REQUIRED -> "Erforderlich. " + check.description
                        else -> "Empfohlen. " + check.description
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!check.ok || check.severity == Severity.INFO) {
                    OutlinedButton(onClick = { onAction(check.id) }, Modifier.padding(top = 8.dp)) { Text(check.actionLabel) }
                }
            }
        }
    }
}
