package org.scottishtecharmy.soundscape.screens.home.home

import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.faq_title_abbreviated
import org.scottishtecharmy.soundscape.resources.menu_help
import java.util.Locale

sealed class HelpTopic(protected val context: Context) {
    // This class uses Uri.encode/decode because FAQ entries may contain "?", which is otherwise
    // interpreted as a query parameter, breaking the routing.

    abstract fun toRouteParam(): String

    abstract fun getDisplayTitle(): String

    class Home(context: Context) : HelpTopic(context) {
        override fun toRouteParam(): String = "page${Res.string.menu_help}"
        override fun getDisplayTitle(): String = Res.string.menu_help.toString()
    }

    class ResourcePage(context: Context, val titleId: Int) : HelpTopic(context) {
        override fun toRouteParam(): String = "page$titleId"
        override fun getDisplayTitle(): String = context.getString(titleId)
    }

    class ResourceFaq(context: Context, val questionId: Int, val answerId: Int) : HelpTopic(context) {
        override fun toRouteParam(): String = "faq$questionId.$answerId"
        override fun getDisplayTitle(): String = Res.string.faq_title_abbreviated.toString()
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

            val locale = Locale.getDefault()
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

        override fun getDisplayTitle(): String = Res.string.faq_title_abbreviated.toString()
    }

    companion object {
        const val HELP_AND_TUTORIALS_FILENAME = "help-and-tutorials.md"
        const val ABOUT_SOUNDSCAPE_FILENAME = "help-about-soundscape.md"

        @Composable
        fun fromRouteParam(param: String): HelpTopic {
            val context = LocalContext.current
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val useMarkdownHelp = preferences.getBoolean(PreferenceKeys.MARKDOWN_HELP, PreferenceDefaults.MARKDOWN_HELP)

            fun getHome(): HelpTopic {
                return if (useMarkdownHelp) {
                    MarkdownPage(context, HELP_AND_TUTORIALS_FILENAME)
                } else {
                    Home(context)
                }
            }

            if (param == "page:${HELP_AND_TUTORIALS_FILENAME}" || param == "page${Res.string.menu_help}" || param.isEmpty()) {
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
                    val key = Uri.decode(param.substring(4))
                    MarkdownPage(context, key)
                }
                param.endsWith(".md") -> MarkdownPage(context, Uri.decode(param))
                else -> getHome()
            }
        }
    }
}