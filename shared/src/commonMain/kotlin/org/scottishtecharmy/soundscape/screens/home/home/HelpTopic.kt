package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.platform.readResourceText
import org.scottishtecharmy.soundscape.platform.getDefaultLanguage
import org.scottishtecharmy.soundscape.platform.getDefaultLanguageTag
import org.scottishtecharmy.soundscape.platform.urlEncode
import org.scottishtecharmy.soundscape.platform.urlDecode
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.faq_title_abbreviated
import org.scottishtecharmy.soundscape.resources.menu_help
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.CancellationToken

sealed class HelpTopic {
    // This class uses urlEncode/decode because FAQ entries may contain "?", which is otherwise
    // interpreted as a query parameter, breaking the routing.

    abstract fun toRouteParam(): String

    @Composable
    abstract fun getDisplayTitle(): String

    class Home : HelpTopic() {
        override fun toRouteParam(): String = "page${Res.string.menu_help.key}"
        @Composable
        override fun getDisplayTitle(): String = stringResource(Res.string.menu_help)
    }

    class ResourcePage(val titleId: StringResource) : HelpTopic() {
        override fun toRouteParam(): String = "page${titleId.key}"
        @Composable
        override fun getDisplayTitle(): String = stringResource(titleId)
    }

    class ResourceFaq(val questionId: StringResource, val answerId: StringResource) : HelpTopic() {
        override fun toRouteParam(): String = "faq${questionId.key}.${answerId.key}"
        @Composable
        override fun getDisplayTitle(): String = stringResource(Res.string.faq_title_abbreviated)
    }

    abstract class MarkdownHelpTopic(
        val fileName: String
    ) : HelpTopic() {

        val content: String by lazy {
            processMarkdownContent(loadMarkdownAsset() ?: "# Error\n\nFailed to load help content")
        }

        val title: String by lazy {
            val flavour = CommonMarkFlavourDescriptor()
            val tree = MarkdownParser(flavour, true, CancellationToken.NonCancellable)
                .buildMarkdownTreeFromString(content as CharSequence)
            
            // Look for the first level 1 heading
            val h1 = tree.children.find { it.type == MarkdownElementTypes.ATX_1 }
            if (h1 != null) {
                h1.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                    ?.getTextInNode(content)?.toString()?.trim() ?: getFallbackTitle()
            } else {
                getFallbackTitle()
            }
        }

        abstract fun getFallbackTitle(): String

        @Composable
        override fun getDisplayTitle(): String = title

        fun getMarkdownFileName(): String =
            if (fileName.endsWith(".md")) fileName else "$fileName.md"

        protected open fun loadMarkdownAsset(): String? {
            val fileName = getMarkdownFileName()

            val localeTag = getDefaultLanguageTag()
            val lang = getDefaultLanguage()

            val candidatePaths = listOf(
                "help/$fileName.$localeTag",
                "help/$fileName.$lang",
                "help/$fileName",
            ).distinct()

            for (path in candidatePaths) {
                try {
                    return readResourceText(path)
                } catch (_: Exception) {
                    // Try next path
                }
            }

            return null
        }

    }

    class MarkdownPage(fileName: String) : MarkdownHelpTopic(fileName) {
        override fun toRouteParam(): String {
            val name = if (fileName.endsWith(".md")) fileName else "$fileName.md"
            return "page:${urlEncode(name)}"
        }

        override fun getFallbackTitle(): String = fileName.removeSuffix(".md")
    }

    class MarkdownFaq(fileName: String, val question: String)
        : MarkdownHelpTopic(fileName)
    {
        override fun toRouteParam(): String = "page:faq:${urlEncode(fileName)}:${urlEncode(question)}"

        @Composable
        override fun getDisplayTitle(): String = stringResource(Res.string.faq_title_abbreviated)

        override fun getFallbackTitle(): String = "FAQ"
    }

    companion object {
        const val HELP_AND_TUTORIALS_FILENAME = "help-and-tutorials.md"
        const val ABOUT_SOUNDSCAPE_FILENAME = "help-about-soundscape.md"

        @Composable
        fun fromRouteParam(param: String, preferences: PreferencesProvider? = null): HelpTopic {
            val useMarkdownHelp = preferences?.getBoolean(PreferenceKeys.MARKDOWN_HELP, PreferenceDefaults.MARKDOWN_HELP) ?: false

            fun getHome(): HelpTopic {
                return if (useMarkdownHelp) {
                    MarkdownPage(HELP_AND_TUTORIALS_FILENAME)
                } else {
                    Home()
                }
            }

            if (param == "page:${HELP_AND_TUTORIALS_FILENAME}" || param == "page${Res.string.menu_help.key}" || param.isEmpty()) {
                return getHome()
            }

            return when {
                param.startsWith("page:") -> {
                    val rest = urlDecode(param.substring(5))
                    if (rest.startsWith("faq:")) {
                        val parts = rest.substring(4).split(":", limit = 2)
                        if (parts.size == 2) {
                            MarkdownFaq(urlDecode(parts[0]), urlDecode(parts[1]))
                        } else {
                            Home()
                        }
                    } else {
                        MarkdownPage(rest)
                    }
                }
                param.startsWith("faq") -> {
                    val ids = param.substring(3).split(".")
                    if (ids.size == 2) {
                        val qId = findStringResourceByKey(ids[0])
                        val aId = findStringResourceByKey(ids[1])
                        if (qId != null && aId != null) {
                            ResourceFaq(qId, aId)
                        } else {
                            Home()
                        }
                    } else {
                        Home()
                    }
                }
                param.startsWith("page") -> {
                    val key = urlDecode(param.substring(4))
                    val res = findStringResourceByKey(key)
                    if (res != null) {
                        ResourcePage(res)
                    } else {
                        MarkdownPage(key)
                    }
                }
                param.endsWith(".md") -> MarkdownPage(urlDecode(param))
                else -> getHome()
            }
        }
    }
}
