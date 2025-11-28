package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.NavHostController
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

class MarkdownPage(val title: String)

/*
@Composable
fun MarkdownHelpScreen(
    topic: String,
    navController: NavHostController,
    modifier: Modifier
) {
    // TODO 2025-11-27 Hugh Greene: Get localised Markdown page contents as raw text, main page only.
    // TODO 2025-11-27 Hugh Greene: Select Markdown page based on topic.
    val page = MarkdownPage("Help and Tutorials")

    // TODO 2025-11-28 Hugh Greene: Render main page sections as "titles" and sub-sections as
    // buttons, using Composables as below.

    // TODO 2025-11-28 Hugh Greene: Render other sections based on help topic navigation, which is
    // set up in HomeScreen.  The markdown files could all be parsed on start-up, or on demand (not
    // cached) to save memory, and sub-sections for the FAQ page can be pulled out into separate
    // commonmark Nodes, so we can select the right one to render.  I think we can have some rule
    // whereby pages with three heading levels, and no text within the first and second levels, can
    // be split into "sub-pages" and linked with buttons.

    // TODO 2025-11-28 Hugh Greene: Also, pull out the route building and parsing into a class, so
    // it's more obvious which bits of code are related in the codebase.

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = page.title,
                navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                onNavigateUp = { navController.popBackStack() },
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
            ) {
                // Help topic page
                LazyColumn(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .mediumPadding(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    items(page.sections) { section ->
                        when (section.type) {
                            SectionType.Title -> {
                                Text(
                                    text = stringResource(section.textId),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .padding(top = spacing.medium)
                                        .semantics {
                                            heading()
                                            if (section.skipTalkback)
                                                invisibleToUser()
                                        },
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            SectionType.Paragraph -> {
                                var htmlText = stringResource(section.textId)
                                if(section.markdown) {
                                    val parser: Parser = Parser.builder().build()
                                    val document: Node? = parser.parse(htmlText)
                                    val renderer = HtmlRenderer.builder().build()
                                    htmlText = renderer.render(document)
                                }
                                Text(
                                    text = AnnotatedString.fromHtml(
                                        htmlString = htmlText,
                                        linkStyles = TextLinkStyles(
                                            style = SpanStyle(
                                                textDecoration = TextDecoration.Underline,
                                            )
                                        )
                                        // TODO 2025-11-17 Hugh Greene: Add linkInteractionListener
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .semantics {
                                            if(section.skipTalkback)
                                                invisibleToUser()
                                        }
                                )
                            }

                            SectionType.Link, SectionType.Faq -> {
                                Button(
                                    onClick = {
                                        if (section.type == SectionType.Faq) {
                                            navController.navigate("${HomeRoutes.Help.route}/faq${section.textId}.${section.faqAnswer}")
                                        } else {
                                            navController.navigate("${HomeRoutes.Help.route}/page${section.textId}")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(spacing.extraSmall),
                                    colors = currentAppButtonColors
                                ) {
                                    Box(
                                        Modifier.weight(6f)
                                    ) {
                                        Text(
                                            text = stringResource(section.textId),
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
                        }
                    }
                }
            }
        }
    )
}
*/
