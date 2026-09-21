package org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.offline_map_storage_description
import org.scottishtecharmy.soundscape.resources.offline_map_storage_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils

@Composable
fun OfflineStorageOnboardingScreenVM(
    onNavigate: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: OffscreenStorageOnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OfflineStorageOnboardingScreen(
        onNavigate = onNavigate,
        uiState = uiState,
        onStorageSelected = { viewModel.selectStorage(it) },
        modifier = modifier
    )
}

@Composable
fun OfflineStorageOnboardingScreen(
    onNavigate: (() -> Unit)?,
    uiState: OfflineStorageOnboardingUiState,
    onStorageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large)
                .padding(top = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(Res.string.offline_map_storage_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        heading()
                    }
                    .focusRequester(focusRequester)
                    .focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))

            Text(
                text = stringResource(Res.string.offline_map_storage_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .smallPadding()
                    .focusable()
            )

            Spacer(modifier = Modifier.height(spacing.large))

            StorageDropDownMenu(
                storages = uiState.storages,
                onStorageSelected = onStorageSelected,
                selectedStorageIndex = uiState.selectedStorageIndex,
                modifier = Modifier
                    .smallPadding()
                    .focusable(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer
            )

            Spacer(modifier = Modifier.height(spacing.large))

            if (onNavigate != null) {
                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = { onNavigate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .testTag("offlineStorageOnboardingScreenContinueButton"),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
fun OfflineStorageOnboardingScreenPreview() {
    val internalStorage = StorageUtils.StorageSpace(
        "/path/to/internal",
        description = "Internal",
        isExternal = false,
        isPrimary = false,
        64 * 1024 * 1024 * 1024L,
        22 * 1024 * 1024 * 1024L,
        "22000 MB",
        23 * 1024 * 1024 * 1024L
    )
    val externalStorage1 = StorageUtils.StorageSpace(
        "/path/to/external1",
        description = "External",
        isExternal = true,
        isPrimary = false,
        128 * 1024 * 1024 * 1024L,
        88 * 1024 * 1024 * 1024L,
        "88000 MB",
        90 * 1024 * 1024 * 1024L
    )
    val externalStorage2 = StorageUtils.StorageSpace(
        "/path/to/external2",
        description = "SD",
        isExternal = true,
        isPrimary = true,
        128 * 1024 * 1024 * 1024L,
        20 * 1024 * 1024 * 1024L,
        "20000 MB",
        30 * 1024 * 1024 * 1024L
    )
    OfflineStorageOnboardingScreen(
        onNavigate = null,
        OfflineStorageOnboardingUiState(
            storages = listOf(internalStorage, externalStorage1, externalStorage2),
            currentPath = "/path/to/external2"
        ),
        onStorageSelected = { _ -> },
    )
}
