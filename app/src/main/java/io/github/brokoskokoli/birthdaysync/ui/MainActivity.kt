package io.github.brokoskokoli.birthdaysync.ui

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.brokoskokoli.birthdaysync.R
import io.github.brokoskokoli.birthdaysync.system.CheckId
import io.github.brokoskokoli.birthdaysync.system.Severity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { MainScreen() } }
    }
}

private enum class Tab(@StringRes val label: Int, val icon: ImageVector) {
    OVERVIEW(R.string.tab_overview, Icons.Filled.Home),
    CONTACTS(R.string.tab_contacts, Icons.Filled.Person),
    CALENDAR(R.string.tab_calendar, Icons.Filled.DateRange),
    NOTIFY(R.string.tab_notify, Icons.Filled.Notifications),
    SYSTEM(R.string.tab_system, Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(vm: MainViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    val checks by vm.checks.collectAsStateWithLifecycle()
    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()
    val perform = rememberCheckAction()
    var resumeTick by remember { mutableIntStateOf(0) }

    // Nach jeder Rückkehr (Berechtigungsdialog, Systemeinstellungen) den Zustand neu einlesen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    LaunchedEffect(resumeTick) { vm.refreshNow() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    if (!onboardingDone) {
        OnboardingScreen(vm, onAction = perform, onFinish = {
            tab = Tab.OVERVIEW
            vm.setOnboardingDone(true)
        })
        return
    }

    val restartOnboarding = { vm.setOnboardingDone(false) }
    val openIssues = checks.count { !it.ok && it.severity != Severity.INFO }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(if (tab == Tab.OVERVIEW) R.string.app_name else tab.label)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            if (t == Tab.SYSTEM && openIssues > 0) {
                                BadgedBox(badge = { Badge { Text("$openIssues") } }) { Icon(t.icon, null) }
                            } else Icon(t.icon, null)
                        },
                        label = { Text(stringResource(t.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.OVERVIEW -> OverviewScreen(vm, modifier, openIssues, onSetup = restartOnboarding, onGoto = { tab = it.toTab() })
            Tab.CONTACTS -> ContactsScreen(vm, modifier, onRequestPermission = { perform(CheckId.CONTACTS) })
            Tab.CALENDAR -> CalendarScreen(vm, modifier, onRequestPermission = { perform(CheckId.CALENDAR) })
            Tab.NOTIFY -> NotifyScreen(vm, modifier, onOpenSystem = { tab = Tab.SYSTEM })
            Tab.SYSTEM -> SystemScreen(vm, modifier, onAction = perform, onSetup = restartOnboarding)
        }
    }
}

/** Ziele, die von der Übersicht aus angesprungen werden können. */
enum class Destination { CONTACTS, CALENDAR }

private fun Destination.toTab() = when (this) {
    Destination.CONTACTS -> Tab.CONTACTS
    Destination.CALENDAR -> Tab.CALENDAR
}
