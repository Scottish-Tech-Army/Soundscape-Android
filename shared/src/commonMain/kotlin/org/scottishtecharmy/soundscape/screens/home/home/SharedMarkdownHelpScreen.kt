package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.menu_open_source_licenses
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

import org.scottishtecharmy.soundscape.preferences.PreferencesProvider

// TODO 2026-10-14 HughG: The Markdown help pages don't yet allow for the "siri" boolean and the GPS
// accuracy value.
@Composable
fun SharedMarkdownHelpScreen(
    topic: String,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenSourceLicenses: (() -> Unit)? = null,
    preferencesProvider: PreferencesProvider? = null,
    modifier: Modifier = Modifier
) {
    val helpTopic = HelpTopic.fromRouteParam(topic, preferencesProvider) as? HelpTopic.MarkdownHelpTopic
        ?: return
    val isFaqSubPage = helpTopic is HelpTopic.MarkdownFaq

    val blocks = remember(helpTopic.content) {
        val processor = MarkdownProcessor(helpTopic.content)
        processor.getBlocks(
            isFaqSubPage = isFaqSubPage,
            faqQuestion = if (helpTopic is HelpTopic.MarkdownFaq) helpTopic.question else null,
            fileName = helpTopic.getMarkdownFileName()
        )
    }
    val displayTitle = helpTopic.getDisplayTitle()

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = displayTitle,
                navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
                onNavigateUp = onNavigateUp,
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier.padding(padding)
            ) {
                // Help topic page
                LazyColumn(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .mediumPadding(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    items(blocks) { block ->
                        when (block) {
                            is MarkdownBlock.Heading -> {
                                Text(
                                    text = block.text,
                                    style = when (block.level) {
                                        2 -> MaterialTheme.typography.titleMedium
                                        else -> MaterialTheme.typography.titleSmall
                                    },
                                    modifier = Modifier
                                        .padding(top = if (block.level == 2) spacing.medium else spacing.small)
                                        .semantics {
                                            heading()
                                        },
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            is MarkdownBlock.Paragraphs -> {
                                val text = parseHtmlToAnnotatedString(block.html)
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is MarkdownBlock.Link -> {
                                LinkButton(
                                    text = block.text,
                                    route = if (block.destination.endsWith(".md")) "${SharedRoutes.HELP}/${block.destination}" else block.destination,
                                    onNavigate = onNavigate
                                )
                            }
                            is MarkdownBlock.FaqQuestion -> {
                                val route = "${SharedRoutes.HELP}/${HelpTopic.MarkdownFaq(block.fileName, block.text).toRouteParam()}"
                                LinkButton(
                                    text = block.text,
                                    route = route,
                                    onNavigate = onNavigate
                                )
                            }
                        }
                    }
                    item {
                        if (helpTopic.getMarkdownFileName() == HelpTopic.ABOUT_SOUNDSCAPE_FILENAME
                            && onOpenSourceLicenses != null
                        ) {
                            CustomButton(
                                onClick = onOpenSourceLicenses,
                                text = stringResource(Res.string.menu_open_source_licenses),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .smallPadding()
                                    .testTag("helpScreenOpenSourceLicenses"),
                                shape = RoundedCornerShape(spacing.extraSmall),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun LinkButton(
    text: String,
    route: String,
    onNavigate: (String) -> Unit
) {
    Button(
        onClick = { onNavigate(route) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.extraSmall),
        colors = currentAppButtonColors
    ) {
        Box(
            Modifier.weight(6f)
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Box(
            Modifier.weight(1f)
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
