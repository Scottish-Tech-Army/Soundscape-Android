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
import androidx.compose.ui.platform.LocalContext
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
import org.commonmark.node.BulletList
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import java.io.IOException

class MarkdownPage(val title: String, val content: String) {
    val root: Node by lazy {
        val parser: Parser = Parser.builder().build()
        // TODO 2025-12-12 Hugh Greene: proper error handling here (log, and better UI output?)
        parser.parse(content) ?: org.commonmark.node.Text("Failed to parse '${title}'")
    }
}

private fun Node.toLogText(): String {
    val sb = StringBuilder()
    accept(object : org.commonmark.node.AbstractVisitor() {
        override fun visit(text: Text) {
            sb.append(text.literal)
        }

        override fun visit(emphasis: Emphasis) {
            sb.append("*")
            visitChildren(emphasis)
            sb.append("*")
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            sb.append("**")
            visitChildren(strongEmphasis)
            sb.append("**")
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            sb.append(" ")
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            sb.appendLine()
        }

        override fun visit(paragraph: Paragraph) {
            visitChildren(paragraph)
            sb.appendLine()
        }

        override fun visit(heading: Heading) {
            visitChildren(heading)
            sb.appendLine()
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(bulletList: BulletList) {
            visitChildren(bulletList)
        }

        override fun visit(orderedList: OrderedList) {
            visitChildren(orderedList)
        }

        override fun visit(listItem: ListItem) {
            val parent = listItem.parent
            if (parent is BulletList) {
                sb.append("- ")
            } else if (parent is OrderedList) {
                var count = parent.startNumber
                var prev = listItem.previous
                while (prev != null) {
                    count++
                    prev = prev.previous
                }
                sb.append("$count. ")
            }
            visitChildren(listItem)
        }
    })
    return sb.toString().trim()
}

private fun List<Node>.toLogText(): String {
    return joinToString("") { it.toLogText() + "\n" }.trim()
}

private fun String.processMarkdownContent(): String {
    // Strip YAML front matter
    val content = if (this.startsWith("---")) {
        val endOfFrontMatter = this.indexOf("---", 3)
        if (endOfFrontMatter != -1) {
            this.substring(endOfFrontMatter + 3).trimStart()
        } else {
            this
        }
    } else {
        this
    }

    // Resolve {% link ... %} tags
    // Replace {% link path/to/file.md %}, {% link file.md %}, etc. with just the filename file.md
    return content.replace(Regex("""\{% link (?:[^/]+/)*([^ ]+\.md) %\}"""), "$1")
}

private fun getMarkdownFileName(context: android.content.Context, topic: String): String? {
    val helpAndTutorialsTitle = context.getString(R.string.menu_help_and_tutorials)

    return when {
        topic == helpAndTutorialsTitle || topic == "page$helpAndTutorialsTitle" -> "help-and-tutorials.md"
        topic.startsWith("page") -> {
            val idStr = topic.substring(4)
            val id = idStr.toIntOrNull()
            if (id != null) {
                when (id) {
                    R.string.voice_voices -> "help-voices.md"
                    R.string.help_remote_page_title -> "help-using-media-controls.md"
                    R.string.beacon_audio_beacon -> "help-audio-beacon.md"
                    R.string.callouts_automatic_callouts -> "help-automatic-callouts.md"
                    R.string.directions_my_location -> "help-my-location.md"
                    R.string.help_orient_page_title -> "help-around-me.md"
                    R.string.help_explore_page_title -> "help-ahead-of-me.md"
                    R.string.callouts_nearby_markers -> "help-nearby-markers.md"
                    R.string.markers_title -> "help-markers.md"
                    R.string.routes_title -> "help-routes.md"
                    R.string.help_creating_markers_page_title -> "help-creating-markers.md"
                    R.string.help_edit_markers_page_title -> "help-customizing-markers.md"
                    R.string.faq_title -> "help-frequently-asked-questions.md"
                    R.string.faq_tips_title -> "help-tips.md"
                    R.string.help_offline_page_title -> "help-why-is-soundscape-working-offline-.md"
                    R.string.settings_about_app -> "help-about-soundscape.md"
                    else -> "$idStr.md"
                }
            } else {
                if (idStr.endsWith(".md")) idStr else "$idStr.md"
            }
        }
        else -> {
            val stripped = topic.removePrefix("page")
            if (stripped.endsWith(".md")) stripped else "$stripped.md"
        }
    }
}

private fun loadMarkdownAsset(context: android.content.Context, topic: String): String? {
    if (topic.startsWith("faq") && topic.contains(".")) {
        val ids = topic.substring(3).split(".")
        try {
            val question = context.getString(ids[0].toInt()).trim()
            val answer = context.getString(ids[1].toInt()).trim()
            return "## $question\n\n$answer"
        } catch (e: Exception) {
            // Fall through
        }
    }

    val fileName = getMarkdownFileName(context, topic) ?: return null

    val locale = java.util.Locale.getDefault()
    val localeTag = locale.toLanguageTag() // e.g., en-GB
    val lang = locale.language // e.g., en

    val candidatePaths = listOf(
        "help/$localeTag/$fileName",
        "help/$lang/$fileName",
        "help/$fileName"
    ).distinct() // in case $localTag and $lang are the same

    for (path in candidatePaths) {
        try {
            return context.assets.open(path).bufferedReader().use { it.readText() }.processMarkdownContent()
        } catch (e: IOException) {
            // Try next path
        }
    }

    return null
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

private fun Node.tryGetOnlyChild(): Node? {
    val firstChild = this.firstChild
    if (firstChild == null || firstChild.next != null) {
        return null
    }
    return firstChild
}

class LinksPage {
    class Section(val title: String) {
        val links: MutableList<Link> = mutableListOf()
    }

    val sections: MutableList<Section> = mutableListOf()
}

fun List<Node>.withoutAnyRootHeading(): List<Node> {
    val firstNode = this[0]
    if (firstNode is Heading && firstNode.level == 1) {
        return this.drop(1)
    }
    return this
}

@Composable
fun MarkdownHelpScreen(
    topic: String,
    navController: NavHostController,
    modifier: Modifier,
    structureLog: StructureLog = StructureLog {}
) {
    structureLog.start("HelpScreen")

    val context = LocalContext.current
    val textContentRenderer = remember { TextContentRenderer.builder().build() }
    val htmlRenderer = remember { HtmlRenderer.builder().build() }

    val isFaqSubPage = topic.startsWith("faq:")
    val faqSubPageInfo = if (isFaqSubPage) {
        val parts = topic.substring(4).split(":", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    } else {
        null
    }

    val helpAndTutorialsTitle = stringResource(R.string.menu_help_and_tutorials)
    val displayTitle = when {
        isFaqSubPage -> stringResource(R.string.faq_title_abbreviated)
        topic == helpAndTutorialsTitle || topic == "page$helpAndTutorialsTitle" -> helpAndTutorialsTitle
        topic.startsWith("page") -> {
            val id = topic.substring(4).toIntOrNull()
            if (id != null) stringResource(id) else topic.removePrefix("page").removeSuffix(".md")
        }
        topic.startsWith("faq") -> stringResource(R.string.faq_title_abbreviated)
        else -> topic.removePrefix("page").removeSuffix(".md")
    }

    val rawContent = if (faqSubPageInfo != null) {
        loadMarkdownAsset(context, "page${faqSubPageInfo.first}")
    } else {
        loadMarkdownAsset(context, topic)
    } ?: "# Error\n\nFailed to load help content for '$topic'"

    val page = MarkdownPage(displayTitle, rawContent)
    val rootNodes = page.root.collectChildren()
    val isFaqListPage = !isFaqSubPage && rootNodes.any { it is Heading && it.level == 3 }

    val nodesToRender = if (faqSubPageInfo != null) {
        filterNodesForFaq(rootNodes, faqSubPageInfo.second, textContentRenderer)
    } else {
        rootNodes.withoutAnyRootHeading()
    }

    // TODO 2025-11-28 Hugh Greene: Also, pull out the route building and parsing into a class, so
    // it's more obvious which bits of code are related in the codebase.

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
                    val groupedNodes = groupNodesByHeadings(
                        nodesToRender, topic, isFaqListPage, textContentRenderer)

                    items(groupedNodes) { nodes ->
                        structureLog.start("LazyColumn item")
                        val firstNode = nodes.first()
                        if (firstNode is Heading) {
                            if (firstNode.level < 2) {
                                // TODO 2025-12-18 Hugh Greene: Handle other levels better!
                                structureLog.unstructured("Skipping Heading level ${firstNode.level}")
                                structureLog.end("LazyColumn item")
                                return@items
                            }

                            if (isFaqListPage && firstNode.level == 3) {
                                val questionText = textContentRenderer.render(firstNode).trim()
                                val fileName = getMarkdownFileName(context, topic) ?: ""
                                LinkButton(
                                    text = questionText,
                                    route = "${HomeRoutes.Help.route}/faq:$fileName:$questionText",
                                    navController = navController,
                                    structureLog = structureLog,
                                    logText = firstNode.toLogText()
                                )
                            } else {
                                val text = textContentRenderer.render(firstNode).trim()
                                structureLog.unstructured("Text for Title: '${firstNode.toLogText()}'")
                                Text(
                                    text = text,
                                    style = if (firstNode.level == 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(top = if (firstNode.level == 2) spacing.medium else spacing.small)
                                        .semantics {
                                            heading()
                                        },
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else if (nodes.size == 1 && firstNode is Paragraph && firstNode.tryGetOnlyChild() is Link) {
                            val link = firstNode.tryGetOnlyChild() as Link
                            val text = link.collectChildren().joinToString("") {
                                textContentRenderer.render(it)
                            }.trim()
                            LinkButton(
                                text = text,
                                route = "${HomeRoutes.Help.route}/${link.destination}",
                                navController = navController,
                                structureLog = structureLog,
                                logText = link.toLogText()
                            )
                        } else {
                            val htmlContent = nodes.joinToString("") { htmlRenderer.render(it) }.trim()
                            val text = AnnotatedString.fromHtml(
                                htmlString = htmlContent,
                                linkStyles = TextLinkStyles(
                                    style = SpanStyle(
                                        textDecoration = TextDecoration.Underline,
                                    )
                                )
                                // TODO 2025-11-17 Hugh Greene: Add linkInteractionListener
                            )
                            structureLog.unstructured("Text for HTML section: '${nodes.toLogText()}'")
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
    structureLog.end("HelpScreen")
}

@Composable
private fun LinkButton(
    text: String,
    route: String,
    navController: NavHostController,
    structureLog: StructureLog,
    logText: String
) {
    Button(
        onClick = {
            navController.navigate(route)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.extraSmall),
        colors = currentAppButtonColors
    ) {
        structureLog.start("Button")
        Box(
            Modifier.weight(6f)
        ) {
            structureLog.start("Box for text")
            structureLog.unstructured("Text for Button: '$logText'")
            Text(
                text = text,
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

private fun filterNodesForFaq(
    nodes: List<Node>,
    questionTitle: String,
    textContentRenderer: TextContentRenderer
): List<Node> {
    val result = mutableListOf<Node>()
    var found = false
    for (node in nodes) {
        if (node is Heading && node.level == 3) {
            val title = textContentRenderer.render(node).trim()
            if (title == questionTitle) {
                found = true
            } else if (found) {
                // Next H3 heading, stop
                break
            }
        } else if (node is Heading && node.level < 3 && found) {
            // Next H1 or H2, stop
            break
        }
        if (found) {
            result.add(node)
        }
    }
    return if (result.isEmpty()) {
        listOf(org.commonmark.node.Text("Question not found: $questionTitle"))
    } else {
        result
    }
}

private fun groupNodesByHeadings(
    nodes: List<Node>,
    topic: String,
    isFaqListPage: Boolean,
    textContentRenderer: TextContentRenderer
): MutableList<MutableList<Node>> {
    val groupedNodes = mutableListOf<MutableList<Node>>()
    val emojiMarkers = listOf('⏯', '⏭', '⏮', '⏩', '⏪', '⏺')

    for (node in nodes) {
        val startsNewItem = when {
            node is Heading -> !isFaqListPage || node.level <= 3
            node is Paragraph && node.tryGetOnlyChild() is Link -> true
            node is Paragraph && (node.firstChild is StrongEmphasis) -> true
            else -> {
                if (groupedNodes.isEmpty()) true
                else {
                    val lastGroup = groupedNodes.last()
                    val firstInLastGroup = lastGroup.first()
                    val lastNode = lastGroup.last()
                    val isFaq = topic.startsWith("faq")

                    if (isFaqListPage && firstInLastGroup is Heading && firstInLastGroup.level == 3) {
                        false
                    } else if (lastNode is Heading) {
                        true
                    } else if (lastNode is Paragraph && lastNode.tryGetOnlyChild() is Link) {
                        true
                    } else if (isFaq) {
                        false // Group everything in FAQ answer
                    } else {
                        // Heuristic for Markers vs Media Controls vs Audio Beacon
                        val lastText = textContentRenderer.render(lastNode).trim()
                        val currentText = textContentRenderer.render(node).trim()
                        val lastEndsInColon = lastText.endsWith(":")
                        val currentStartsInEmoji =
                            currentText.isNotEmpty() && emojiMarkers.contains(currentText[0])

                        !lastEndsInColon && !currentStartsInEmoji
                    }
                }
            }
        }

        if (startsNewItem) {
            groupedNodes.add(mutableListOf(node))
        } else {
            groupedNodes.last().add(node)
        }
    }
    return groupedNodes
}
