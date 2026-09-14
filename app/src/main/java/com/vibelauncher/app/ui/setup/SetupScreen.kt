package com.vibelauncher.app.ui.setup

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography

private val StepTitleStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)

/**
 * First-run setup: one step at a time, Back/Next along the bottom. Everything is skippable -
 * a permission step's own button asks for access, and the footer moves on whether or not it
 * was granted.
 */
@Composable
fun SetupScreen(viewModel: SetupViewModel, onFinished: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    // Home role, notification access and usage access are all granted in system Settings,
    // with no result callback - re-read everything whenever the user comes back here.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel by rememberUpdatedState(viewModel)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentViewModel.refreshAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Back steps backwards through setup; on the first step there's nowhere to go, and
    // leaving setup that way would just bring it back on the next launch.
    BackHandler { viewModel.back() }

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LauncherBlack)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            StepDots(count = uiState.steps.size, current = uiState.index)

            AnimatedContent(
                targetState = uiState.index,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "setup step"
            ) { index ->
                val step = uiState.steps[index]
                val label = "${index + 1} / ${uiState.steps.size}"
                when (step) {
                    SetupStep.WELCOME -> WelcomeStep(label)
                    SetupStep.DONE -> DoneStep(label, uiState.mode)
                    SetupStep.VIBE_BAR,
                    SetupStep.TRY_IT,
                    SetupStep.HOME_SCREEN,
                    SetupStep.TILES_AND_SHORTCUTS,
                    SetupStep.SETTINGS -> TourStep(
                        step = step,
                        label = label,
                        triedTodo = uiState.triedTodo,
                        onSaveTodo = viewModel::saveTryItTodo
                    )
                    else -> PermissionStep(
                        step = step,
                        label = label,
                        access = uiState.access,
                        zipCode = uiState.zipCode,
                        onAccessChanged = viewModel::refreshAccess,
                        onSaveZipCode = viewModel::saveZipCode
                    )
                }
            }

            SetupFooter(
                showBack = !uiState.isFirst,
                nextLabel = nextLabelFor(uiState),
                onBack = { viewModel.back() },
                onNext = {
                    if (uiState.step == SetupStep.DONE) viewModel.finish(onFinished) else viewModel.next()
                }
            )
        }
    }
}

/** "not now" on a step whose access is still off, so it's clear moving on skips it. */
private fun nextLabelFor(state: SetupUiState): String {
    val access = state.access
    return when (state.step) {
        SetupStep.WELCOME -> "start"
        SetupStep.DONE -> "finish"
        SetupStep.MAKE_HOME -> if (access.isDefaultHome) "next" else "not now"
        SetupStep.CALENDAR -> if (access.calendar) "next" else "not now"
        SetupStep.CONTACTS -> if (access.contacts) "next" else "not now"
        SetupStep.TEXT_AND_CALL -> if (access.textAndCall) "next" else "not now"
        SetupStep.NOTIFICATIONS -> if (access.notifications) "next" else "not now"
        SetupStep.USAGE -> if (access.usage) "next" else "not now"
        SetupStep.WEATHER -> if (state.zipCode.isNotBlank()) "next" else "skip"
        SetupStep.LETTER_KEYS -> "next"
        SetupStep.TRY_IT -> if (state.triedTodo != null) "next" else "skip"
        SetupStep.VIBE_BAR, SetupStep.HOME_SCREEN, SetupStep.TILES_AND_SHORTCUTS, SetupStep.SETTINGS -> "next"
    }
}

@Composable
private fun WelcomeStep(label: String) {
    StepLayout(label = label, title = "welcome to vibe") {
        StepText("vibe is a home screen you type into. there's no grid of apps here - start typing to text someone, call, add a to-do or an event, or swipe up to see every app.")
        StepText("the next few steps turn on what vibe needs to work. every one can be skipped now and changed later.")
    }
}

@Composable
private fun DoneStep(label: String, mode: SetupMode) {
    if (mode == SetupMode.TOUR) {
        StepLayout(label = label, title = "that's the tour") {
            StepText("it's here whenever you want it again, in settings.")
        }
        return
    }
    StepLayout(label = label, title = "you're all set") {
        StepText("anything you skipped can be turned on later, and this whole walkthrough can be run again.")
        StepText("settings: swipe up on home, then long-press any app and tap settings.")
    }
}

@Composable
private fun StepDots(count: Int, current: Int) {
    val accent = LocalAccentColor.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val color = when {
                index == current -> accent
                index < current -> LauncherWhite
                else -> LauncherMutedGray.copy(alpha = 0.4f)
            }
            Box(Modifier.size(6.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun SetupFooter(showBack: Boolean, nextLabel: String, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kept in the layout on the first step, just invisible, so "start" doesn't jump
        // sideways when Back appears.
        TextButton(onClick = onBack, enabled = showBack, modifier = Modifier.alpha(if (showBack) 1f else 0f)) {
            Text("back", color = LauncherMutedGray)
        }
        PrimaryButton(text = nextLabel, onClick = onNext)
    }
}

/** The shared shape of every step: position, big title, then the step's own content. */
@Composable
internal fun StepLayout(label: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(label, color = LocalAccentColor.current, style = MaterialTheme.typography.labelSmall)
        Text(title, color = LauncherWhite, style = StepTitleStyle)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
internal fun StepText(text: String, muted: Boolean = false) {
    Text(
        text = text,
        color = if (muted) LauncherMutedGray else LauncherWhite.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodyMedium
    )
}

/** "on" with a filled check once granted, "not yet" with an empty ring before. */
@Composable
internal fun StatusPill(granted: Boolean, grantedText: String = "on", missingText: String = "not yet") {
    val accent = LocalAccentColor.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (granted) {
            Box(Modifier.size(18.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = LauncherWhite, modifier = Modifier.size(12.dp))
            }
        } else {
            Box(Modifier.size(18.dp).border(1.5.dp, LauncherMutedGray, CircleShape))
        }
        Text(
            text = if (granted) grantedText else missingText,
            color = if (granted) LauncherWhite else LauncherMutedGray,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current, contentColor = LauncherWhite)
    ) {
        Text(text)
    }
}

@Composable
internal fun SecondaryButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = LauncherMutedGray)
    }
}

/** Starts the first of [intents] that something on this phone can open - several Settings
 *  deep links only exist on newer Android versions, so each comes with a wider fallback. */
internal fun Context.startFirstAvailable(vararg intents: Intent) {
    val intent = intents.firstOrNull { it.resolveActivity(packageManager) != null } ?: return
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
