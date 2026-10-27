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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.R

@Composable
fun ContactsScreen(vm: MainViewModel, modifier: Modifier, onRequestPermission: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val hasPermission by vm.hasContacts.collectAsStateWithLifecycle()
    val selected = settings.selectedSources

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        item {
            InfoText(
                stringResource(R.string.contacts_intro),
                Modifier.padding(0.dp),
            )
        }
        if (!hasPermission) {
            item {
                SectionCard(stringResource(R.string.contacts_no_permission_title)) {
                    InfoText(stringResource(R.string.contacts_no_permission_text))
                    Button(onClick = onRequestPermission, Modifier.padding(horizontal = 16.dp)) { Text(stringResource(R.string.action_allow_access)) }
                }
            }
        } else if (sources.isEmpty()) {
            item { InfoText(stringResource(R.string.contacts_none_found), Modifier.padding(0.dp)) }
        }
        items(sources, key = { it.id }) { source ->
            val accountSelected = source.id in selected
            SectionCard(source.label, subtitle = source.accountName) {
                CheckRow(
                    title = stringResource(R.string.contacts_all),
                    subtitle = counts(source.contactCount, source.birthdayCount),
                    checked = accountSelected,
                    onChange = { vm.setSourceSelected(source.id, it) },
                )
                if (source.groups.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    source.groups.forEach { group ->
                        CheckRow(
                            title = group.title,
                            subtitle = counts(group.contactCount, group.birthdayCount),
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

@Composable
private fun counts(contacts: Int, birthdays: Int) = stringResource(
    R.string.contacts_counts,
    pluralStringResource(R.plurals.contacts_count, contacts, contacts),
    pluralStringResource(R.plurals.contacts_with_birthday, birthdays, birthdays),
)
