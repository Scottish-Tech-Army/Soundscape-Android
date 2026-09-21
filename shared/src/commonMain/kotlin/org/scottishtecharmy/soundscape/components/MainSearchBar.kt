package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.search_choose_destination
import org.scottishtecharmy.soundscape.resources.search_no_results
import org.scottishtecharmy.soundscape.resources.search_searching
import org.scottishtecharmy.soundscape.resources.settings_section_search
import org.scottishtecharmy.soundscape.resources.text_field_clear_text
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.talkbackDescription
import org.scottishtecharmy.soundscape.screens.talkbackLive
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun MainSearchBar(
    results: List<LocationDescription>,
    onTriggerSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(Res.string.settings_section_search),
    onItemClick: (LocationDescription) -> Unit,
    userLocation: LngLatAlt?,
    isSearching: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val shape = RoundedCornerShape(spacing.small)
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val searchLocation = remember { mutableStateOf(userLocation) }

    LaunchedEffect(expanded) { onExpandedChange(expanded) }

    // Collapsed search bar
    Surface(
        modifier = modifier
            .clickable { expanded = true }
            .testTag("mainSearchBarCollapsed"),
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
                contentDescription = stringResource(Res.string.settings_section_search),
                tint = colors.onSurfaceVariant
            )
            Spacer(Modifier.width(spacing.small))
            Text(
                text = query.ifEmpty { stringResource(Res.string.settings_section_search) },
                style = textStyle.copy(
                    color = if (query.isEmpty()) colors.onSurfaceVariant else colors.onSurface
                ),
                modifier = Modifier.talkbackDescription(hint)
            )
        }
    }

    // Fullscreen search overlay. Pin to (0, 0) in window coordinates so it covers
    // the top bar on Android instead of docking at the collapsed search bar's anchor.
    val fullscreenPositionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = IntOffset.Zero
        }
    }
    if (expanded) {
        Popup(
            popupPositionProvider = fullscreenPositionProvider,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current

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
                    Surface(color = colors.surface) {
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
                                },
                                modifier = Modifier.testTag("mainSearchBarBackButton")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.ui_back_button_title),
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
                                            onTriggerSearch(trimmed)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("mainSearchBarTextField"),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth()) {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = stringResource(Res.string.settings_section_search),
                                                style = textStyle.copy(color = colors.onSurfaceVariant)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.testTag("mainSearchBarClearButton")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.text_field_clear_text),
                                        tint = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Results
                    if (isSearching) {
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
                                text = stringResource(Res.string.search_searching),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.talkbackLive()
                            )
                        }
                    } else if (results.isEmpty()) {
                        Text(
                            text = if (query.isBlank())
                                stringResource(Res.string.search_choose_destination)
                            else
                                stringResource(Res.string.search_no_results),
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
                                Column(
                                    modifier = Modifier.semantics {
                                        collectionItemInfo = CollectionItemInfo(
                                            rowSpan = 1, columnSpan = 1,
                                            rowIndex = index, columnIndex = 0,
                                        )
                                    }
                                ) {
                                    if (index == 0) {
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
