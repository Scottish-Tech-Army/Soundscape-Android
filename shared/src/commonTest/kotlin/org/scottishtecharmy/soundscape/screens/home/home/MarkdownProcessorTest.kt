package org.scottishtecharmy.soundscape.screens.home.home

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownProcessorTest {

    @Test
    fun testHeadingExtraction() {
        val content = """
            # Title
            ## Subtitle
            Paragraph text.
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false)
        
        println("Blocks: " + blocks.joinToString())
        
        assertEquals(2, blocks.size)
        assertEquals(MarkdownBlock.Heading("Subtitle", 2), blocks[0])
        assertEquals(MarkdownBlock.Paragraph("<p>Paragraph text.</p>"), blocks[1])
    }

    @Test
    fun testFaqFiltering() {
        val content = """
            # FAQ
            ## Section 1
            ### Question 1?
            Answer 1.
            ### Question 2
            Answer 2.
            ## Section 2
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        
        val blocks1 = processor.getBlocks(true, "Question 1")
        println("Blocks1: " + blocks1.joinToString())
        assertEquals(2, blocks1.size)
        assertEquals(MarkdownBlock.Heading("Question 1?", 3), blocks1[0])
        assertEquals(MarkdownBlock.Paragraph("<p>Answer 1.</p>"), blocks1[1])
        
        val blocks2 = processor.getBlocks(true, "Question 2")
        println("Blocks2: " + blocks2.joinToString())
        assertEquals(2, blocks2.size)
        assertEquals(MarkdownBlock.Heading("Question 2", 3), blocks2[0])
        assertEquals(MarkdownBlock.Paragraph("<p>Answer 2.</p>"), blocks2[1])
    }

    @Test
    fun testLinkButtonExtraction() {
        val content = """
            # Title
            [Link Text](destination.md)
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false)
        
        println("Blocks Link: " + blocks.joinToString())
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Link("Link Text", "destination.md"), blocks[0])
    }
}
