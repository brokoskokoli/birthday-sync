package io.github.brokoskokoli.birthdaysync.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ContactsScreen(vm: MainViewModel, modifier: Modifier, onRequestPermission: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val hasPermission by vm.hasContacts.collectAsStateWithLifecycle()
    val selected = settings.selectedSources

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        item {
            InfoText(
                "Wähle ganze Konten oder einzelne Gruppen/Labels. Von allen Kontakten darin werden die Geburtstage übernommen.",
                Modifier.padding(0.dp),
            )
        }
        if (!hasPermission) {
            item {
                SectionCard("Zugriff auf Kontakte fehlt") {
                    InfoText("Ohne diese Berechtigung können keine Kontaktlisten angezeigt werden.")
                    Button(onClick = onRequestPermission, Modifier.padding(horizontal = 16.dp)) { Text("Zugriff erlauben") }
                }
            }
        } else if (sources.isEmpty()) {
            item { InfoText("Keine Kontakte auf dem Gerät gefunden.", Modifier.padding(0.dp)) }
        }
        items(sources, key = { it.id }) { source ->
            val accountSelected = source.id in selected
            SectionCard(source.label, subtitle = source.accountName) {
                CheckRow(
                    title = "Alle Kontakte",
                    subtitle = "${source.contactCount} Kontakte · ${source.birthdayCount} mit Geburtstag",
                    checked = accountSelected,
                    onChange = { vm.setSourceSelected(source.id, it) },
                )
                if (source.groups.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    source.groups.forEach { group ->
                        CheckRow(
                            title = group.title,
                            subtitle = "${group.contactCount} Kontakte · ${group.birthdayCount} mit Geburtstag",
                            checked = accountSelected || group.id in selected,
                            enabled = !accountSelected,
                            indent = true,
                            onChange = { vm.setSourceSelected(group.id, it) },
                        )
                    }
                }
            }
        }
    }
}
