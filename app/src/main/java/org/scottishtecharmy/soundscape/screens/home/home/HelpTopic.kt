package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.MainActivity.Companion.MARKDOWN_HELP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.MARKDOWN_HELP_KEY
import org.scottishtecharmy.soundscape.R
import java.io.IOException

sealed class HelpTopic(protected val context: Context) {
    // This class uses Uri.encode/decode because FAQ entries may contain "?", which is otherwise
    // interpreted as a query parameter, breaking the routing.

    abstract fun toRouteParam(): String

    abstract fun getDisplayTitle(): String

    class Home(context: Context) : HelpTopic(context) {
        override fun toRouteParam(): String = "page${R.string.menu_help}"
        override fun getDisplayTitle(): String = context.getString(R.string.menu_help)
    }

    class ResourcePage(context: Context, val titleId: Int) : HelpTopic(context) {
        override fun toRouteParam(): String = "page$titleId"
        override fun getDisplayTitle(): String = context.getString(titleId)
    }

    class ResourceFaq(context: Context, val questionId: Int, val answerId: Int) : HelpTopic(context) {
        override fun toRouteParam(): String = "faq$questionId.$answerId"
        override fun getDisplayTitle(): String = context.getString(R.string.faq_title_abbreviated)
    }

    abstract class MarkdownHelpTopic(
        context: Context,
        val fileName: String
    )
        : HelpTopic(context) {

        private val content: String by lazy {
            loadMarkdownAsset(context) ?: "# Error\n\nFailed to load help content" }

        val root: Node by lazy {
            val processedContent = content.processMarkdownContent()
            val parser: Parser = Parser.builder().build()
            parser.parse(processedContent) ?: org.commonmark.node.Text("Failed to parse content")
        }

        fun getDisplayTitle(textContentRenderer: TextContentRenderer): String {
            val rootNode = root
            val firstNode = rootNode.firstChild
            if (firstNode is Heading && firstNode.level == 1) {
                return textContentRenderer.render(firstNode).trim()
            }
            return getDisplayTitle()
        }

        fun getMarkdownFileName(): String? =
            if (fileName.endsWith(".md")) fileName else "$fileName.md"

        protected open fun loadMarkdownAsset(context: Context): String? {
            val fileName = getMarkdownFileName() ?: return null

            val locale = java.util.Locale.getDefault()
            val localeTag = locale.toLanguageTag() // e.g., en-GB
            val lang = locale.language // e.g., en

            val candidatePaths = listOf(
                "help/$localeTag/$fileName",
                "help/$lang/$fileName",
                "help/en-GB/$fileName",
                "help/en/$fileName",
            ).distinct() // in case $localTag and $lang are the same, or are "en[-GB]".

            for (path in candidatePaths) {
                try {
                    return context.assets.open(path).bufferedReader().use { it.readText() }
                } catch (_: IOException) {
                    // Try next path
                }
            }

            return null
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

    }


    class MarkdownPage(context: Context, fileName: String) : MarkdownHelpTopic(context, fileName) {
        override fun toRouteParam(): String {
            val name = if (fileName.endsWith(".md")) fileName else "$fileName.md"
            return "page:${Uri.encode(name)}"
        }

        override fun getDisplayTitle(): String = fileName.removeSuffix(".md")
    }

    class MarkdownFaq(context: Context, fileName: String, val question: String)
        : MarkdownHelpTopic(context, fileName)
    {
        override fun toRouteParam(): String = "page:faq:${Uri.encode(fileName)}:${Uri.encode(question)}"

        override fun getDisplayTitle(): String = context.getString(R.string.faq_title_abbreviated)
    }

    companion object {
        const val HELP_AND_TUTORIALS_FILENAME = "help-and-tutorials.md"
        const val ABOUT_SOUNDSCAPE_FILENAME = "help-about-soundscape.md"

        @Composable
        fun fromRouteParam(param: String): HelpTopic {
            val context = LocalContext.current
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val useMarkdownHelp = preferences.getBoolean(MARKDOWN_HELP_KEY, MARKDOWN_HELP_DEFAULT)

            fun getHome(): HelpTopic {
                return if (useMarkdownHelp) {
                    MarkdownPage(context, HELP_AND_TUTORIALS_FILENAME)
                } else {
                    Home(context)
                }
            }

            if (param == "page:${HELP_AND_TUTORIALS_FILENAME}" || param == "page${R.string.menu_help}" || param.isEmpty()) {
                return getHome()
            }

            return when {
                param.startsWith("page:") -> {
                    val rest = Uri.decode(param.substring(5))
                    if (rest.startsWith("faq:")) {
                        val parts = rest.substring(4).split(":", limit = 2)
                        if (parts.size == 2) {
                            MarkdownFaq(context, Uri.decode(parts[0]), Uri.decode(parts[1]))
                        } else {
                            Home(context)
                        }
                    } else {
                        MarkdownPage(context, rest)
                    }
                }
                param.startsWith("faq") -> {
                    val ids = param.substring(3).split(".")
                    if (ids.size == 2) {
                        val qId = ids[0].toIntOrNull()
                        val aId = ids[1].toIntOrNull()
                        if (qId != null && aId != null) {
                            ResourceFaq(context, qId, aId)
                        } else {
                            Home(context)
                        }
                    } else {
                        Home(context)
                    }
                }
                param.startsWith("page") -> {
                    val rest = Uri.decode(param.substring(4))
                    val id = rest.toIntOrNull()
                    if (id != null) {
                        if (id == R.string.menu_help) {
                            Home(context)
                        } else {
                            ResourcePage(context, id)
                        }
                    } else {
                        MarkdownPage(context, rest)
                    }
                }
                param.endsWith(".md") -> MarkdownPage(context, Uri.decode(param))
                else -> getHome()
            }
        }
    }
}
