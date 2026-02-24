package org.scottishtecharmy.soundscape.screens.home.home

import org.scottishtecharmy.soundscape.R

sealed class HelpTopic {
    abstract fun toRouteParam(): String

    data object Home : HelpTopic() {
        override fun toRouteParam(): String = "home"
    }

    data class ResourcePage(val titleId: Int) : HelpTopic() {
        override fun toRouteParam(): String = "page$titleId"
    }

    data class ResourceFaq(val questionId: Int, val answerId: Int) : HelpTopic() {
        override fun toRouteParam(): String = "faq$questionId.$answerId"
    }

    data class MarkdownPage(val fileName: String) : HelpTopic() {
        override fun toRouteParam(): String {
            val name = if (fileName.endsWith(".md")) fileName else "$fileName.md"
            return "page$name"
        }
    }

    data class MarkdownFaq(val fileName: String, val question: String) : HelpTopic() {
        override fun toRouteParam(): String = "faq:$fileName:$question"
    }

    companion object {
        fun fromRouteParam(param: String): HelpTopic {
            if (param == "home" || param.isEmpty()) return Home

            return when {
                param.startsWith("faq:") -> {
                    val parts = param.substring(4).split(":", limit = 2)
                    if (parts.size == 2) MarkdownFaq(parts[0], parts[1]) else Home
                }
                param.startsWith("faq") -> {
                    val ids = param.substring(3).split(".")
                    if (ids.size == 2) {
                        val qId = ids[0].toIntOrNull()
                        val aId = ids[1].toIntOrNull()
                        if (qId != null && aId != null) ResourceFaq(qId, aId) else Home
                    } else Home
                }
                param.startsWith("page") -> {
                    val rest = param.substring(4)
                    val id = rest.toIntOrNull()
                    if (id != null) {
                        if (id == R.string.menu_help_and_tutorials) Home else ResourcePage(id)
                    } else {
                        MarkdownPage(rest)
                    }
                }
                param.endsWith(".md") -> MarkdownPage(param)
                else -> Home
            }
        }
    }
}
