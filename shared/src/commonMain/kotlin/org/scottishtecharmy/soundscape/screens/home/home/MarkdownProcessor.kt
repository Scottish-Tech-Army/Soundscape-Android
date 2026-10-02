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
    data class Paragraph(val html: String) : MarkdownBlock()
    data class Link(val text: String, val destination: String) : MarkdownBlock()
}

class MarkdownProcessor(private val content: String) {

    fun getBlocks(isFaqSubPage: Boolean, faqQuestion: String? = null): List<MarkdownBlock> {
        val flavour = CommonMarkFlavourDescriptor()
        val rootNode = MarkdownParser(flavour, true, CancellationToken.NonCancellable)
            .buildMarkdownTreeFromString(content as CharSequence)
        
        val nodes = rootNode.children
        
        val processedNodes = if (isFaqSubPage && faqQuestion != null) {
            filterNodesForFaq(nodes, faqQuestion)
        } else {
            withoutAnyRootHeading(nodes)
        }
        
        return groupNodesToBlocks(processedNodes, flavour)
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

    // TODO 2026-10-01 HughG: This isn't following the grouping logic, it's producing a flat list.
    private fun groupNodesToBlocks(nodes: List<ASTNode>, flavour: CommonMarkFlavourDescriptor): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        
        for (node in nodes) {
            when (node.type) {
                MarkdownElementTypes.ATX_1 -> {
                    val text = node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                        ?.getTextInNode(content)?.toString()?.trim() ?: ""
                    blocks.add(MarkdownBlock.Heading(text, 1))
                }
                MarkdownElementTypes.ATX_2 -> {
                    val text = node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                        ?.getTextInNode(content)?.toString()?.trim() ?: ""
                    blocks.add(MarkdownBlock.Heading(text, 2))
                }
                MarkdownElementTypes.ATX_3 -> {
                    val text = node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }
                        ?.getTextInNode(content)?.toString()?.trim() ?: ""
                    blocks.add(MarkdownBlock.Heading(text, 3))
                }
                MarkdownElementTypes.PARAGRAPH if isOnlyLink(node) -> {
                    val linkNode = findLinkNode(node)!!
                    val text = linkNode.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                        ?.getTextInNode(content)?.toString()?.removePrefix("[")?.removeSuffix("]")?.trim() ?: ""
                    val destination = linkNode.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                        ?.getTextInNode(content)?.toString() ?: ""
                    blocks.add(MarkdownBlock.Link(text, destination))
                }
                else -> {
                    val html = HtmlGenerator(content, node, flavour).generateHtml()
                    val trimmedHtml = html.trim()
                    if (trimmedHtml.isNotEmpty() && trimmedHtml != "<p></p>") {
                        blocks.add(MarkdownBlock.Paragraph(trimmedHtml))
                    }
                }
            }
        }
        return blocks
    }

    private fun isOnlyLink(node: ASTNode): Boolean {
        val children = node.children.filter { 
            val type = it.type.toString()
            type != "WHITE_SPACE" && type != "EOL"
        }
        return children.size == 1 && children[0].type == MarkdownElementTypes.INLINE_LINK
    }
    
    private fun findLinkNode(node: ASTNode): ASTNode? {
        return node.children.find { it.type == MarkdownElementTypes.INLINE_LINK }
    }
}
