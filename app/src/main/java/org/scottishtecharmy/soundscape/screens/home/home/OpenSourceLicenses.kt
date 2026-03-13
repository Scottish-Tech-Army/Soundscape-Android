package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.viewmodels.LicenseInfo
import org.scottishtecharmy.soundscape.viewmodels.OpenSourceLicensesViewModel

@Composable
fun OpenSourceLicensesVM(
    navController: NavHostController,
    modifier: Modifier
) {
    val viewModel = hiltViewModel<OpenSourceLicensesViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OpenSourceLicenses(
        licenses = uiState.licenses,
        navController = navController,
        modifier = modifier,
        onLicenseClick = viewModel::toggleLicense)
}

@Composable
fun OpenSourceLicenses(
    licenses: List<LicenseInfo>,
    navController: NavHostController,
    modifier: Modifier,
    onLicenseClick: (LicenseInfo) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FlexibleAppBar(
                title = stringResource(R.string.menu_open_source_licenses),
                leftSide = {
                    IconWithTextButton(
                        text = stringResource(R.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft")
                    ) {
                        navController.navigateUp()
                    }
                }
            )
        },

        content = { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                itemsIndexed(licenses) { _, license ->
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .clickable { onLicenseClick(license) }
                            .defaultMinSize(minHeight = spacing.targetSize)
                            .smallPadding(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!license.project.isNullOrEmpty())
                            Text(text = license.project)

                       if (license.isExpanded) {
                            if (license.licenses.isNotEmpty())
                                Text(
                                    text = buildAnnotatedString {
                                        withLink(LinkAnnotation.Url(url = license.licenses[0].second)) {
                                            withStyle(
                                                style = SpanStyle(
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            ) {
                                                append(license.licenses[0].first)
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            if (!license.description.isNullOrEmpty())
                                Text(
                                    text = license.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            if (license.developers.isNotEmpty())
                                Text(
                                    text = license.developers[0],
                                    style = MaterialTheme.typography.bodySmall
                                )
                            if (license.url != null) {
                                Text(
                                    text = buildAnnotatedString {
                                        withLink(LinkAnnotation.Url(url = license.url)) {
                                            withStyle(
                                                style = SpanStyle(
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            ) {
                                                append(license.url)
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun OpenSourceLicensesPreview() {
    OpenSourceLicenses(
        listOf(
            LicenseInfo(
                "Project 1",
                "Author A",
                "1.1",
                emptyList(),
                "http://github.com/davidmoten/rtree2",
                listOf(Pair("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")),
                false
            ),
            LicenseInfo(
                "Project 2",
                "Author B",
                "1.2",
                emptyList(),
                "https://github.com/google/accompanist/",
                listOf(Pair("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")),
                true
            ),
            LicenseInfo(
                "Project 3",
                "Author C",
                "1.3",
                emptyList(),
                "https://developer.android.com/jetpack/androidx/releases/activity#1.10.1",
                listOf(Pair("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")),
                false
            ),
            LicenseInfo(
                "Project 4",
                "Author D",
                "1.4",
                emptyList(),
                "https://developer.android.com/jetpack/androidx/releases/activity#1.10.1",
                listOf(Pair("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")),
                false
            ),
            LicenseInfo(
                "Project 5",
                "Author E",
                "1.5",
                emptyList(),
                "https://developer.android.com/jetpack/androidx/releases/activity#1.10.1",
                listOf(Pair("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")),
                false
            )
        ),
        rememberNavController(),
        Modifier
    )
}
