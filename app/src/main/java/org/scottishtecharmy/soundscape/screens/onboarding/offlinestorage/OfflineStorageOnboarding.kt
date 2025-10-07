package org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils

@Composable
fun OfflineStorageOnboardingScreenVM(
    onNavigate: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: OffscreenStorageOnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OfflineStorageOnboardingScreen(
        onNavigate = onNavigate,
        uiState = uiState,
        onStorageSelected = { viewModel.selectStorage(it, viewModel.appContext)},
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
    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ){
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
                text = stringResource(R.string.offline_map_storage_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Spacer(modifier = Modifier.height(spacing.large))

            Text(
                text = stringResource(R.string.offline_map_storage_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.smallPadding()
            )

            Spacer(modifier = Modifier.height(spacing.large))

            StorageDropDownMenu(
                storages = uiState.storages,
                onStorageSelected = onStorageSelected,
                selectedStorageIndex = uiState.selectedStorageIndex,
                modifier = Modifier.smallPadding()
            )

            Spacer(modifier = Modifier.height(spacing.large))

            if(onNavigate != null) {
                OnboardButton(
                    text = stringResource(R.string.ui_continue),
                    onClick = { onNavigate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offlineStorageOnboardingScreenContinueButton"),
                )
            }
        }
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
        64*1024*1024*1024L,
        22*1024*1024*1024L,
        "22000 MB",
        23*1024*1024*1024L
    )
    val externalStorage1 = StorageUtils.StorageSpace(
        "/path/to/external1",
        description = "External",
        isExternal = true,
        isPrimary = false,
        128*1024*1024*1024L,
        88*1024*1024*1024L,
        "88000 MB",
        90*1024*1024*1024L
    )
    val externalStorage2 = StorageUtils.StorageSpace(
        "/path/to/external2",
        description = "SD",
        isExternal = true,
        isPrimary = true,
        128*1024*1024*1024L,
        20*1024*1024*1024L,
        "20000 MB",
        30*1024*1024*1024L
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