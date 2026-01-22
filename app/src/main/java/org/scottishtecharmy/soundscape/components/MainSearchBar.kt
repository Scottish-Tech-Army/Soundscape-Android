package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.talkbackLive
import org.scottishtecharmy.soundscape.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    searchText: String,
    isSearching: Boolean,
    searchInProgress: Boolean,
    itemList: List<LocationDescription>,
    searchFunctions: SearchFunctions,
    onItemClick: (LocationDescription) -> Unit,
    userLocation: LngLatAlt?
) {
    val searchLocation = remember { userLocation }
    val firstItemFocusRequester = remember { FocusRequester() }
    val searchTriggered = remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(searchText, TextRange(searchText.length)))
    }

    // Track the last search query for which we moved focus to results
    var lastFocusedQuery by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(searchText) {
        if (searchText != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = searchText,
                selection = TextRange(searchText.length)
            )
        }
    }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            // Force cursor to the end when returning/expanding
            textFieldValue = textFieldValue.copy(selection = TextRange(searchText.length))
        } else {
            // Reset the last focused query when search is closed
            lastFocusedQuery = null
        }
    }

    LaunchedEffect(itemList, searchInProgress) {
        // Move focus to the first result when search finishes and there are results,
        // but only if we haven't already moved focus for this specific query.
        if (!searchInProgress && itemList.isNotEmpty() && searchText != lastFocusedQuery) {
            // A small delay ensures the UI has composed the results before we try to focus
            delay(1000)
            firstItemFocusRequester.requestFocus()
            lastFocusedQuery = searchText
        }
    }

    SearchBar(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (!isSearching) {
                        Modifier.padding(horizontal = spacing.small)
                    } else {
                        Modifier.padding(horizontal = spacing.none)
                    },
                ),
        shape = RoundedCornerShape(spacing.small),
        inputField = {
            SearchBarDefaults.InputField(
                // TODO: SearchBarDefaults doesn't currently take a TextFieldValue which means that
                //  we can't control the cursor position within it. Rather than write a new search
                //  bar we'll await this being fixed in Material3.
                query = textFieldValue.text,
                onQueryChange = { newText ->
                    textFieldValue = textFieldValue.copy(text = newText, selection = TextRange(newText.length))
                    searchFunctions.onSearchTextChange(newText)
                },
                onSearch = {
                    searchTriggered.value = true
                    searchFunctions.onTriggerSearch()
                },
                expanded = isSearching,
                onExpandedChange = { expanded ->
                    if (expanded != isSearching) {
                        searchFunctions.onToggleSearch()
                    }
                },
                placeholder = { Text(stringResource(id = R.string.search_choose_destination)) },
                leadingIcon = {
                    when {
                        !isSearching -> {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        }

                        else -> {
                            IconButton(
                                onClick = {
                                    searchFunctions.onToggleSearch()
                                    searchFunctions.onSearchTextChange("")
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription =
                                        stringResource(R.string.cancel_search_contentDescription),
                                )
                            }
                        }
                    }
                },
            )
        },
        expanded = isSearching,
        onExpandedChange = { expanded ->
            if (expanded != isSearching) {
                searchFunctions.onToggleSearch()
            }
        }
    ) {
        if(searchInProgress) {
            Text(
                text = stringResource(R.string.search_searching),
                modifier = Modifier.talkbackLive()
            )
        }
        else if(itemList.isEmpty()) {
            if(searchTriggered.value) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    modifier = Modifier.talkbackLive()
                )
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .semantics {
                            this.collectionInfo =
                                CollectionInfo(
                                    rowCount = itemList.size, // Total number of items
                                    columnCount = 1, // Single-column list
                                )
                        }
                        .fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.padding(top = spacing.medium)) {
                    itemsIndexed(itemList) { index, item ->
                        var modifier =
                            Modifier.semantics {
                                this.collectionItemInfo =
                                    CollectionItemInfo(
                                        rowSpan = 1,
                                        columnSpan = 1,
                                        rowIndex = index,
                                        columnIndex = 0,
                                    )
                            }
                        
                        if(index == 0) {
                            println("Set up focusRequester")
                            modifier = modifier
                                .focusRequester(firstItemFocusRequester)
                                .focusable()
                        }

                        Column {
                            LocationItem(
                                item = item,
                                decoration = LocationItemDecoration(
                                    location = true,
                                    source = item.source,
                                    details = EnabledFunction(
                                        true,
                                        {
                                            onItemClick(item)
                                        }
                                    )
                                ),
                                modifier = modifier,
                                userLocation = searchLocation
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainSearchPreview() {
    MainSearchBar(
        searchText = "",
        isSearching = false,
        searchInProgress = false,
        emptyList(),
        SearchFunctions(null),
        {},
        LngLatAlt()
    )
}
@Preview(showBackground = true)
@Composable
fun MainSearchPreviewSearching() {
    MainSearchBar(
        searchText = "Monaco",
        isSearching =  true,
        searchInProgress = false,
        emptyList(),
        SearchFunctions(null),
        {},
        LngLatAlt()
    )
}

@Preview(showBackground = true)
@Composable
fun MainSearchPreviewSearchInProgress() {
    MainSearchBar(
        searchText = "Monaco",
        isSearching =  true,
        searchInProgress = true,
        emptyList(),
        SearchFunctions(null),
        {},
        LngLatAlt()
    )
}
