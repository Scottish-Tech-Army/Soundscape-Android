package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.menu_open_source_licenses
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.viewmodels.LicenseInfo

@Composable
fun SharedOpenSourceLicensesScreen(
    licenses: List<LicenseInfo>,
    onNavigateUp: () -> Unit,
    onLicenseClick: (LicenseInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FlexibleAppBar(
                title = stringResource(Res.string.menu_open_source_licenses),
                leftSide = {
                    IconWithTextButton(
                        text = stringResource(Res.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft"),
                    ) {
                        onNavigateUp()
                    }
                },
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
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (!license.project.isNullOrEmpty()) {
                            Text(text = license.project)
                        }

                        if (license.isExpanded) {
                            if (license.licenses.isNotEmpty()) {
                                Text(
                                    text = buildAnnotatedString {
                                        withLink(LinkAnnotation.Url(url = license.licenses[0].second)) {
                                            withStyle(
                                                style = SpanStyle(
                                                    textDecoration = TextDecoration.Underline,
                                                ),
                                            ) {
                                                append(license.licenses[0].first)
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (!license.description.isNullOrEmpty()) {
                                Text(
                                    text = license.description,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (license.developers.isNotEmpty()) {
                                Text(
                                    text = license.developers[0],
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (license.url != null) {
                                Text(
                                    text = buildAnnotatedString {
                                        withLink(LinkAnnotation.Url(url = license.url)) {
                                            withStyle(
                                                style = SpanStyle(
                                                    textDecoration = TextDecoration.Underline,
                                                ),
                                            ) {
                                                append(license.url)
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
