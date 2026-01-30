package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.talkbackDescription
import org.scottishtecharmy.soundscape.screens.talkbackLive
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun MainSearchBar(
    results: List<LocationDescription>,
    searchFunctions: SearchFunctions,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.settings_section_search),
    onItemClick: (LocationDescription) -> Unit,
    userLocation: LngLatAlt?,
    beaconLocation: LngLatAlt? = null,
    isSearching: Boolean = false)
{
    val shape = RoundedCornerShape(spacing.small)
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val searchLocation = remember { mutableStateOf(userLocation) }

    // Collapsed search bar
    Surface(
        modifier = modifier.clickable { expanded = true },
        shape = shape,
        color = colors.surface,
        tonalElevation = spacing.tiny,
        shadowElevation = spacing.small
    ) {
        Row(
            modifier = Modifier
                .height(spacing.targetSize)
                .fillMaxWidth()
                .padding(horizontal = spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.settings_section_search),
                tint = colors.onSurfaceVariant
            )
            Spacer(Modifier.width(spacing.small))
            Text(
                text = query.ifEmpty { stringResource(R.string.settings_section_search) },
                style = textStyle.copy(
                    color = if (query.isEmpty()) colors.onSurfaceVariant else colors.onSurface
                ),
                modifier = Modifier.talkbackDescription(hint)
            )
        }
    }

    // Fullscreen search overlay
    if (expanded) {
        Popup(
            onDismissRequest = {
                expanded = false
            },
            properties = PopupProperties(focusable = true)
        ) {
            // Get keyboard controller inside Popup since it has its own window
            val keyboardController = LocalSoftwareKeyboardController.current

            // Request focus on the text field when the search overlay opens
            LaunchedEffect(expanded) {
                if (expanded) {
                    focusRequester.requestFocus()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search header
                    Surface(
                        color = colors.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.tiny, vertical = spacing.tiny),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    expanded = false
                                    query = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.ui_back_button_title),
                                    tint = colors.onSurface
                                )
                            }

                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                cursorBrush = SolidColor(colors.primary),
                                textStyle = textStyle,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        val trimmed = query.trim()
                                        if (trimmed.isNotEmpty()) {
                                            searchLocation.value = userLocation
                                            keyboardController?.hide()
                                            searchFunctions.onTriggerSearch(trimmed)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth()) {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.settings_section_search),
                                                style = textStyle.copy(color = colors.onSurfaceVariant)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.text_field_clear_text),
                                        tint = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Results section with loading indicator and TalkBack announcement
                    if (isSearching) {
                        // Visual loading indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.small),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(spacing.medium),
                                strokeWidth = spacing.tiny
                            )
                            Spacer(Modifier.width(spacing.small))
                            Text(
                                text = stringResource(R.string.search_searching),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.talkbackLive()
                            )
                        }
                    } else if (results.isEmpty()) {
                        Text(
                            text = if (query.isBlank())
                                    stringResource(R.string.search_choose_destination)
                                else
                                    stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(spacing.small)
                        )
                    } else {
                        val imePadding = WindowInsets.ime.asPaddingValues()
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = spacing.medium),
                            contentPadding = PaddingValues(
                                bottom = imePadding.calculateBottomPadding() + (spacing.targetSize * 2)
                            )
                        ) {
                            itemsIndexed(results) { index, item ->
                                val modifier =
                                    Modifier.semantics {
                                        this.collectionItemInfo =
                                            CollectionItemInfo(
                                                rowSpan = 1,
                                                columnSpan = 1,
                                                rowIndex = index,
                                                columnIndex = 0,
                                            )
                                    }

                                Column {
                                    if(index == 0) {
                                        HorizontalDivider(
                                            thickness = spacing.tiny,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }

                                    LocationItem(
                                        item = item,
                                        decoration = LocationItemDecoration(
                                            location = true,
                                            source = item.source,
                                            details = EnabledFunction(
                                                true,
                                                {
                                                    expanded = false
                                                    onItemClick(item)
                                                }
                                            )
                                        ),
                                        modifier = modifier,
                                        userLocation = searchLocation.value
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MainSearchBarPreview() {
    MainSearchBar(
        listOf(),
        SearchFunctions(null),
        onItemClick = {},
        userLocation = null)
}

@Preview
@Composable
fun MainSearchBarWithResultsPreview() {
    MainSearchBar(
        listOf(
            LocationDescription("Greggs", LngLatAlt(0.0, 0.0)),
            LocationDescription("Another Greggs", LngLatAlt(0.0, 0.0)),
            LocationDescription("Yet another Greggs", LngLatAlt(0.0, 0.0)),
        ),
        SearchFunctions(null),
        onItemClick = {},
        userLocation = LngLatAlt()
    )
}