package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    searchText: String,
    isSearching: Boolean,
    itemList: List<LocationDescription>,
    searchFunctions: SearchFunctions,
    onItemClick: (LocationDescription) -> Unit,
    userLocation: LngLatAlt?
) {
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
                query = searchText,
                onQueryChange = { searchFunctions.onSearchTextChange(it) },
                onSearch = { searchFunctions.onTriggerSearch() },
                expanded = isSearching,
                onExpandedChange = { searchFunctions.onToggleSearch() },
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
                                onClick = { searchFunctions.onToggleSearch() },
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
        onExpandedChange = { searchFunctions.onToggleSearch() },
    ) {
        Column(
            modifier =
                Modifier
                    .semantics {
                        this.collectionInfo =
                            CollectionInfo(
                                rowCount = itemList.size, // Total number of items
                                columnCount = 1, // Single-column list
                            )
                    }.fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.padding(top = spacing.medium)) {
                itemsIndexed(itemList) { index, item ->
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
                                        searchFunctions.onToggleSearch()
                                    }
                                )
                            ),
                            modifier =
                                Modifier.semantics {
                                    this.collectionItemInfo =
                                        CollectionItemInfo(
                                            rowSpan = 1,
                                            columnSpan = 1,
                                            rowIndex = index,
                                            columnIndex = 0,
                                        )
                                },
                            userLocation = userLocation
                        )
                        HorizontalDivider()
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
        emptyList(),
        SearchFunctions(null),
        {},
        LngLatAlt()
    )
}



