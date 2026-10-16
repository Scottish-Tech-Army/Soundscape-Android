package org.scottishtecharmy.soundscape.screens.home.home

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

sealed class MarkdownBlock {
    data class Heading(val text: String, val level: Int) : MarkdownBlock()
    data class Paragraphs(val html: String) : MarkdownBlock()
    data class Link(val text: String, val destination: String) : MarkdownBlock()
    data class FaqQuestion(val text: String, val fileName: String) : MarkdownBlock()
}

private val LINK_TAG = Regex("""\{% link (?:[^/]+/)*([^ ]+\.md) %\}""")

fun processMarkdownContent(raw: String): String {
    val withoutFrontMatter = if (raw.startsWith("---")) {
        val endOfFrontMatter = raw.indexOf("---", 3)
        if (endOfFrontMatter != -1) {
            raw.substring(endOfFrontMatter + 3).trimStart()
        } else {
            raw
        }
    } else {
        raw
    }
    return withoutFrontMatter.replace(LINK_TAG, "$1")
}

class MarkdownProcessor(content: String) {

    private val content = content
    private val mediaControlCharacters = listOf('⏯', '⏭', '⏮', '⏩', '⏪', '⏺')

    fun getBlocks(
        isFaqSubPage: Boolean,
        faqQuestion: String? = null,
        fileName: String? = null
    ): List<MarkdownBlock> {
        val flavour = CommonMarkFlavourDescriptor()
        val rootNode = MarkdownParser(flavour, true, CancellationToken.NonCancellable)
            .buildMarkdownTreeFromString(content as CharSequence)

        val nodes = rootNode.children

        val processedNodes = if (isFaqSubPage && faqQuestion != null) {
            filterNodesForFaq(nodes, faqQuestion)
        } else {
            withoutAnyRootHeading(nodes)
        }

        val isFaqListPage = !isFaqSubPage && processedNodes.any { isHeading(it, 3) }

        val grouped = groupNodesByHeadings(processedNodes, isFaqListPage, isFaqSubPage)

        return groupsToBlocks(grouped, isFaqListPage, fileName, flavour)
    }

    private fun filterNodesForFaq(nodes: List<ASTNode>, questionTitle: String): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        var found = false
        val normalizedQuestionTitle = questionTitle.trim().removeSuffix("?").removeSuffix(":")

        for (node in nodes) {
            if (node.type == MarkdownElementTypes.ATX_3) {
                val title = node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                    ?.getTextInNode(content)?.toString()?.trim() ?: ""
                val normalizedTitle = title.removeSuffix("?").removeSuffix(":")
                if (normalizedTitle == normalizedQuestionTitle) {
                    found = true
                } else if (found) {
                    break
                }
            } else if ((node.type == MarkdownElementTypes.ATX_1 || node.type == MarkdownElementTypes.ATX_2) && found) {
                break
            }
            if (found) {
                result.add(node)
            }
        }
        return result
    }

    private fun withoutAnyRootHeading(nodes: List<ASTNode>): List<ASTNode> {
        val firstNode = nodes.getOrNull(0) ?: return nodes
        if (firstNode.type == MarkdownElementTypes.ATX_1) {
            return nodes.drop(1)
        }
        return nodes
    }

    private fun groupNodesByHeadings(
        nodes: List<ASTNode>,
        isFaqListPage: Boolean,
        isFaqSubPage: Boolean
    ): MutableList<MutableList<ASTNode>> {
        val groupedNodes = mutableListOf<MutableList<ASTNode>>()

        for (node in nodes) {
            if (nodeStartsNewItem(node, isFaqListPage, isFaqSubPage, groupedNodes)) {
                groupedNodes.add(mutableListOf(node))
            } else {
                groupedNodes.last().add(node)
            }
        }
        return groupedNodes
    }

    private fun nodeStartsNewItem(
        node: ASTNode,
        isFaqListPage: Boolean,
        isFaqSubPage: Boolean,
        groupedNodes: MutableList<MutableList<ASTNode>>
    ): Boolean {
        return when {
            isHeading(node) -> !isFaqListPage || headingLevel(node) <= 3
            isParagraphWithOnlyLink(node) -> true
            isParagraphStartingWithStrong(node) -> true
            else -> {
                if (groupedNodes.isEmpty()) {
                    true
                } else {
                    val lastGroup = groupedNodes.last()
                    val firstInLastGroup = lastGroup.first()
                    val lastNode = lastGroup.last()

                    if (isFaqListPage && isHeading(firstInLastGroup, 3)) {
                        false
                    } else if (isHeading(lastNode)) {
                        true
                    } else if (isParagraphWithOnlyLink(lastNode)) {
                        true
                    } else if (isFaqSubPage) {
                        false
                    } else {
                        val lastText = nodeToText(lastNode).trim()
                        val currentText = nodeToText(node).trim()
                        val lastEndsInColon = lastText.endsWith(":")
                        val currentStartsInMediaControls =
                            currentText.isNotEmpty() && mediaControlCharacters.contains(currentText[0])

                        !lastEndsInColon && !currentStartsInMediaControls
                    }
                }
            }
        }
    }

    private fun groupsToBlocks(
        groups: MutableList<MutableList<ASTNode>>,
        isFaqListPage: Boolean,
        fileName: String?,
        flavour: CommonMarkFlavourDescriptor
    ): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()

        for (group in groups) {
            val firstNode = group.first()

            if (isHeading(firstNode)) {
                val level = headingLevel(firstNode)
                if (level < 2) continue

                val text = headingText(firstNode)

                if (isFaqListPage && level == 3) {
                    val mdFileName = fileName ?: ""
                    blocks.add(MarkdownBlock.FaqQuestion(text, mdFileName))
                } else {
                    blocks.add(MarkdownBlock.Heading(text, level))
                }
            } else if (group.size == 1 && isParagraphWithOnlyLink(firstNode)) {
                val linkNode = findLinkNode(firstNode)!!
                val text = linkNode.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                    ?.getTextInNode(content)?.toString()
                    ?.removePrefix("[")?.removeSuffix("]")?.trim() ?: ""
                val destination = linkNode.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?.getTextInNode(content)?.toString() ?: ""
                blocks.add(MarkdownBlock.Link(text, destination))
            } else {
                val html = group.joinToString("") { node ->
                    HtmlGenerator(content, node, flavour).generateHtml()
                }.trim()
                if (html.isNotEmpty() && html != "<p></p>") {
                    blocks.add(MarkdownBlock.Paragraphs(html))
                }
            }
        }
        return blocks
    }

    // --- AST inspection helpers ---

    private fun isHeading(node: ASTNode): Boolean =
        node.type == MarkdownElementTypes.ATX_1 ||
        node.type == MarkdownElementTypes.ATX_2 ||
        node.type == MarkdownElementTypes.ATX_3

    private fun isHeading(node: ASTNode, level: Int): Boolean =
        when (level) {
            1 -> node.type == MarkdownElementTypes.ATX_1
            2 -> node.type == MarkdownElementTypes.ATX_2
            3 -> node.type == MarkdownElementTypes.ATX_3
            else -> false
        }

    private fun headingLevel(node: ASTNode): Int =
        when (node.type) {
            MarkdownElementTypes.ATX_1 -> 1
            MarkdownElementTypes.ATX_2 -> 2
            MarkdownElementTypes.ATX_3 -> 3
            else -> 0
        }

    private fun headingText(node: ASTNode): String =
        node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
            ?.getTextInNode(content)?.toString()?.trim() ?: ""

    private fun isParagraphWithOnlyLink(node: ASTNode): Boolean =
        node.type == MarkdownElementTypes.PARAGRAPH && isOnlyLink(node)

    private fun isParagraphStartingWithStrong(node: ASTNode): Boolean {
        if (node.type != MarkdownElementTypes.PARAGRAPH) return false
        val children = node.children.filter { !isWhitespaceOrEol(it) }
        return children.isNotEmpty() && children[0].type == MarkdownElementTypes.STRONG
    }

    private fun isOnlyLink(node: ASTNode): Boolean {
        val children = node.children.filter { !isWhitespaceOrEol(it) }
        return children.size == 1 && children[0].type == MarkdownElementTypes.INLINE_LINK
    }

    private fun findLinkNode(node: ASTNode): ASTNode? =
        node.children.find { it.type == MarkdownElementTypes.INLINE_LINK }

    private fun isWhitespaceOrEol(node: ASTNode): Boolean {
        val type = node.type.toString()
        return type == "WHITE_SPACE" || type == "EOL"
    }

    private fun nodeToText(node: ASTNode): String {
        val flavour = CommonMarkFlavourDescriptor()
        return HtmlGenerator(content, node, flavour).generateHtml()
            .replace(Regex("<[^>]+>"), "")
    }
}
