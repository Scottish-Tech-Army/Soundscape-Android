package org.scottishtecharmy.soundscape.screens.migration

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.migration.LegacyImportResult
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.legacy_migration_complete
import org.scottishtecharmy.soundscape.resources.legacy_migration_continue
import org.scottishtecharmy.soundscape.resources.legacy_migration_description
import org.scottishtecharmy.soundscape.resources.legacy_migration_failed
import org.scottishtecharmy.soundscape.resources.legacy_migration_needs_map_data
import org.scottishtecharmy.soundscape.resources.legacy_migration_not_now
import org.scottishtecharmy.soundscape.resources.legacy_migration_progress
import org.scottishtecharmy.soundscape.resources.legacy_migration_title
import org.scottishtecharmy.soundscape.resources.legacy_migration_try_again
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

/** What the import is doing, as far as the screen is concerned. */
sealed interface LegacyMigrationUiState {
    data class Running(val done: Int, val total: Int) : LegacyMigrationUiState

    data class Finished(val imported: Int) : LegacyMigrationUiState

    /** Nothing was imported for want of map data; offer to try again. */
    data object NeedsMapData : LegacyMigrationUiState

    data object Failed : LegacyMigrationUiState
}

/**
 * Shown once, on the first run after an upgrade from the legacy iOS app, while its markers and
 * routes are imported.
 *
 * The import used to run inside the app's `init()`, before any UI existed. That was fine while it
 * was a local database copy, but naming markers means fetching map tiles for each area the user
 * saved something in, and blocking launch on the network is how an app gets killed by the
 * watchdog. So it happens here instead, where it can take the time it needs and say what it's
 * doing.
 *
 * Deliberately not part of onboarding, which an upgrading user has already completed and will
 * never be shown again.
 *
 * [runImport] reports progress as it goes and says how it finished. Naming a marker needs map
 * data, and when there is none to be had the import writes nothing at all rather than save a pile
 * of markers under the wrong names - a half-import can't be resumed later without duplicating
 * what already landed. That case offers the user a retry once they're connected, and stays on
 * offer the next time they open the app.
 */
@Composable
fun LegacyMigrationScreen(
    runImport: suspend ((done: Int, total: Int) -> Unit) -> LegacyImportResult,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember {
        mutableStateOf<LegacyMigrationUiState>(LegacyMigrationUiState.Running(0, 0))
    }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        state = LegacyMigrationUiState.Running(0, 0)
        state = when (val result = runImport { done, total ->
            state = LegacyMigrationUiState.Running(done, total)
        }) {
            is LegacyImportResult.Imported -> LegacyMigrationUiState.Finished(result.count)
            LegacyImportResult.NeedsMapData -> LegacyMigrationUiState.NeedsMapData
            LegacyImportResult.Unreadable -> LegacyMigrationUiState.Failed
        }
    }

    LegacyMigrationScreenContent(
        state = state,
        onRetry = { attempt++ },
        onContinue = onContinue,
        modifier = modifier,
    )
}

/**
 * The screen itself, separated from running the import so it can be previewed and tested in each
 * of its states.
 */
@Composable
fun LegacyMigrationScreenContent(
    state: LegacyMigrationUiState,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleFocusRequester = remember { FocusRequester() }
    val continueFocusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.legacy_migration_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusRequester(titleFocusRequester).focusable(),
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Text(
                text = stringResource(Res.string.legacy_migration_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable(),
            )

            Spacer(modifier = Modifier.height(spacing.large))

            // One live region covering whichever line of status applies, so a screen reader
            // announces the outcome without the user having to go looking for it. Progress
            // counts up as markers are imported, and the same region then reads the result.
            val status = when (state) {
                is LegacyMigrationUiState.Running -> stringResource(
                    Res.string.legacy_migration_progress,
                    state.done.toString(),
                    state.total.toString(),
                )

                is LegacyMigrationUiState.Finished ->
                    stringResource(Res.string.legacy_migration_complete)

                LegacyMigrationUiState.NeedsMapData ->
                    stringResource(Res.string.legacy_migration_needs_map_data)

                LegacyMigrationUiState.Failed -> stringResource(Res.string.legacy_migration_failed)
            }

            if (state is LegacyMigrationUiState.Running) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(spacing.medium))
            }

            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .focusable()
                    .testTag("legacyMigrationStatus"),
            )

            if (state !is LegacyMigrationUiState.Running) {
                Spacer(modifier = Modifier.height(spacing.large))

                if (state is LegacyMigrationUiState.NeedsMapData) {
                    OnboardButton(
                        text = stringResource(Res.string.legacy_migration_try_again),
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(continueFocusRequester)
                            .focusable()
                            .testTag("legacyMigrationRetryButton"),
                    )

                    Spacer(modifier = Modifier.height(spacing.small))
                }

                // Leaving without importing is always allowed. Being held on this screen with no
                // way past it would lock a user out of the app entirely for as long as they have
                // no signal - and the import is still waiting for them next time either way.
                OnboardButton(
                    text = if (state is LegacyMigrationUiState.NeedsMapData) {
                        stringResource(Res.string.legacy_migration_not_now)
                    } else {
                        stringResource(Res.string.legacy_migration_continue)
                    },
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (state is LegacyMigrationUiState.NeedsMapData) {
                                Modifier
                            } else {
                                Modifier.focusRequester(continueFocusRequester)
                            },
                        )
                        .focusable()
                        .testTag("legacyMigrationContinueButton"),
                )

                // Move focus onto the first button as soon as it appears, so a screen reader user
                // isn't left on a progress line that has stopped changing.
                LaunchedEffect(state) {
                    withFrameNanos { }
                    continueFocusRequester.requestFocus()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        titleFocusRequester.requestFocus()
    }
}
