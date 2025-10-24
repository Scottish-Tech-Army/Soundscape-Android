package org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils

@Composable
fun StorageItem(
    name: String,
    freeSpace: String,
    isSelected: Boolean,
    foregroundColor: Color,
    modifier: Modifier
) {

    Row(modifier = modifier) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = freeSpace,
                style = MaterialTheme.typography.bodySmall,
                color = foregroundColor,
                modifier = Modifier
                    .padding(horizontal = spacing.small)
            )
        }
        if (isSelected) {
            Icon(
                Icons.Rounded.Done,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(spacing.icon)
            )
        } else {
            Spacer(modifier = Modifier.width(spacing.medium))
        }
    }
}

@Composable
fun StorageDropDownMenu(
    storages: List<StorageUtils.StorageSpace>,
    onStorageSelected : (String) -> Unit,
    selectedStorageIndex: Int,
    modifier : Modifier = Modifier
){
    var expanded by rememberSaveable { mutableStateOf(false) }
    val storageSelected by remember(selectedStorageIndex) {
        derivedStateOf {
            selectedStorageIndex >= 0
        }
    }
    Box(modifier = modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.CenterStart)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(spacing.extraSmall)
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    expanded = !expanded
                },
                modifier = Modifier.semantics {
                    if (storageSelected) {
                        selected = true
                    }
                }
            ) {
                Text(
                    text = if (storageSelected) {
                        storages[selectedStorageIndex].description +
                        ", " +
                        stringResource(R.string.offline_maps_free_space).format(storages[selectedStorageIndex].availableString)
                    } else {
                        stringResource(R.string.no_language_selected)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

            }

        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .selectableGroup()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)
        ) {
            storages.forEachIndexed { index, storage ->
                val isSelected = remember { index == selectedStorageIndex }
                DropdownMenuItem(
                    text = {
                        StorageItem(
                            storage.description,
                            storage.availableString,
                            isSelected,
                            MaterialTheme.colorScheme.onSurface,
                            Modifier
                        )
                    },
                    modifier = Modifier
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                expanded = false
                                onStorageSelected(storage.path)
                            }
                        )
                    ,
                    onClick = {
                        expanded = false
                        onStorageSelected(storage.path)
                    },
                )
            }
        }
    }
}

data object MockStoragePreviewData {
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
    val storages = listOf(internalStorage, externalStorage1, externalStorage2)
}

@Preview
@Composable
fun StorageDropDownMenuPreview(){
    StorageDropDownMenu(
        storages = MockStoragePreviewData.storages,
        onStorageSelected = {},
        selectedStorageIndex = 1
    )
}


@Preview(fontScale = 1.5f)
@Composable
fun StorageItemPreview() {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        StorageItem(
            "Internal shared storage",
            "22.35 GB free",
            true,
            MaterialTheme.colorScheme.onSurface,
            Modifier
        )
    }
}

@Preview
@Composable
fun StorageItemPreviewNotSelected() {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        StorageItem(
            "Internal",
            "22 GB",
            false,
            MaterialTheme.colorScheme.onSurface,
            Modifier
        )
    }
}