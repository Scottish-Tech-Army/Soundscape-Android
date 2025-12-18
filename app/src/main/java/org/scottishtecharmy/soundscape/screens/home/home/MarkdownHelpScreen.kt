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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.NavHostController
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

class MarkdownPage(val title: String, val content: String) {
    val root: Node by lazy {
        val parser: Parser = Parser.builder().build()
        // TODO 2025-12-12 Hugh Greene: proper error handling here (log, and better UI output?)
        parser.parse(content) ?: return@lazy org.commonmark.node.Text("Failed to parse '${title}'")
    }
}


private fun Node.collectChildren(): List<Node> {
    val children = mutableListOf<Node>()
    var node: Node? = this.firstChild
    while (node != null) {
        children.add(node)
        node = node.next
    }
    return children
}

@Composable
fun MarkdownHelpScreen(
    topic: String,
    navController: NavHostController,
    modifier: Modifier,
    structureLog: StructureLog = StructureLog {}
) {
    structureLog.start("MarkdownHelpScreen")
    // NOTE 2025-12-12 Hugh Greene: Annoyingly, we can't use the org.commonmark.node.Visitor
    // approach because its method aren't @Composable.  We could make an entirely @Composable copy
    // of that, but I'm going to just do the minimum necessary to get things working here, and hope
    // that nobody introduces any fancy Markdown usage in future which breaks this rendering without
    // the tests picking it up.

    // TODO 2025-11-27 Hugh Greene: Get localised Markdown page contents as raw text, main page only.

    // TODO 2025-12-12 Hugh Greene: Use https://github.com/commonmark/commonmark-java#yaml-front-matter
    // extension setup to parse out and skip the front-matter.
/*
    val mainPageStubContent = """
        ---
        title: Help and Tutorials
        layout: page
        nav_order: 1
        ---

        etc., etc.
        """
*/
    val mainPageStubContent = """
        # Help and Tutorials
        
        ## Configuring Soundscape
        
        [Voices](help-voices.md)

        [Using Media Controls](help-using-media-controls.md)
        
        ## Beacons and Callouts
        
        [Audio Beacon](help-audio-beacon.md)
        
        [Automatic Callouts](help-automatic-callouts.md)
        
        ## Home Screen Buttons
        
        [My Location](help-my-location.md)
        
        [Around Me](help-around-me.md)
        
        [Ahead of Me](help-ahead-of-me.md)
        
        [Nearby Markers](help-nearby-markers.md)
        
        ## Markers and Routes
        
        [Markers](help-markers.md)
        
        [Routes](help-routes.md)
        
        [Creating Markers](help-creating-markers.md)
        
        [Customising Markers](help-customizing-markers.md)
        
        ## Frequently Asked Questions
        
        [Frequently Asked Questions](help-frequently-asked-questions.md)
        
        [Tips](help-tips.md)
        
        [Why is Soundscape working offline?](help-why-is-soundscape-working-offline-.md)
                        
        """.trimIndent()
    // TODO 2025-11-27 Hugh Greene: Select Markdown page based on topic.
    val page = MarkdownPage("Help and Tutorials", mainPageStubContent)

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

    val textContentRenderer = TextContentRenderer.builder().build()
    val htmlRenderer = HtmlRenderer.builder().build()

    Scaffold(
        modifier = modifier,
        topBar = {
            structureLog.start("Scaffold topBar")
            CustomAppBar(
                title = page.title,
                navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                onNavigateUp = { navController.popBackStack() },
            )
            structureLog.end("Scaffold topBar")
        },
        content = { padding ->
            structureLog.start("Scaffold content")
            Box(
                modifier = Modifier
                    .padding(padding)
            ) {
                structureLog.start("Box")
                // Help topic page
                LazyColumn(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .mediumPadding(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    structureLog.start("LazyColumn")
                    // TODO 2025-12-12 Hugh Greene: Check whether this is a "headings and links
                    // only" page and, if so, render the structure bit-by-bit; otherwise just render
                    // as HTML???
                    val rootChildren = page.root.collectChildren()

                    items(rootChildren) { node ->
                        structureLog.start("LazyColumn item")
                        if (node is Heading) {
                            if (node.level != 2) {
                                // TODO 2025-12-1 Hugh Greene: Handle other levels better!
                                return@items
                            }
                            val text = textContentRenderer.render(node)
                            structureLog.unstructured("Text for Heading: '${text}'")
                            Text(
                                text = text,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(top = spacing.medium)
                                    .semantics {
                                        heading()
//                                        if (node.skipTalkback)
//                                            invisibleToUser()
                                    },
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        else if (node is Link) {
                            Button(
                                onClick = {
                                    navController.navigate("${HomeRoutes.Help.route}/page${node.title}")
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(spacing.extraSmall),
                                colors = currentAppButtonColors
                            ) {
                                structureLog.start("Button")
                                Box(
                                    Modifier.weight(6f)
                                ) {
                                    structureLog.start("Box for text")
                                    Text(
                                        text = node.title,
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    structureLog.end("Box for text")
                                }
                                Box(
                                    Modifier.weight(1f)
                                ) {
                                    structureLog.start("Box for icon")
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        null,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    )
                                    structureLog.end("Box for icon")
                                }
                                structureLog.end("Button")
                            }
                        }
                        else {
                            val text = AnnotatedString.fromHtml(
                                htmlString = htmlRenderer.render(node),
                                linkStyles = TextLinkStyles(
                                    style = SpanStyle(
                                        textDecoration = TextDecoration.Underline,
                                    )
                                )
                                // TODO 2025-11-17 Hugh Greene: Add linkInteractionListener
                            )
                            structureLog.unstructured("Text for HTML section: '${text}'")
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .semantics {
//                                        if(node.skipTalkback)
//                                            invisibleToUser()
                                    }
                            )
                        }
                        structureLog.end("LazyColumn item")
                    }
                    structureLog.end("LazyColumn")
                }
                structureLog.end("Box")
            }
            structureLog.end("Scaffold content")
        }
    )
    structureLog.end("MarkdownHelpScreen")
}