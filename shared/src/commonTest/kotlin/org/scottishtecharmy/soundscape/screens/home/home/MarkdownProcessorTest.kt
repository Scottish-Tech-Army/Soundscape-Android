package org.scottishtecharmy.soundscape.screens.home.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

        assertEquals(2, blocks.size)
        assertEquals(MarkdownBlock.Heading("Subtitle", 2), blocks[0])
        assertEquals(MarkdownBlock.Paragraphs("<p>Paragraph text.</p>"), blocks[1])
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
        assertEquals(2, blocks1.size)
        assertEquals(MarkdownBlock.Heading("Question 1?", 3), blocks1[0])
        assertEquals(MarkdownBlock.Paragraphs("<p>Answer 1.</p>"), blocks1[1])

        val blocks2 = processor.getBlocks(true, "Question 2")
        assertEquals(2, blocks2.size)
        assertEquals(MarkdownBlock.Heading("Question 2", 3), blocks2[0])
        assertEquals(MarkdownBlock.Paragraphs("<p>Answer 2.</p>"), blocks2[1])
    }

    @Test
    fun testLinkButtonExtraction() {
        val content = """
            # Title
            [Link Text](destination.md)
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false)

        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Link("Link Text", "destination.md"), blocks[0])
    }

    @Test
    fun testGroupingColonEnding() {
        val content = """
            # Title
            Label:
            value text
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraphs)
        val html = (blocks[0] as MarkdownBlock.Paragraphs).html
        assertTrue(html.contains("Label"))
        assertTrue(html.contains("value text"))
    }

    @Test
    fun testGroupingMediaControls() {
        val content = """
            # Title
            Media controls:
            ⏯ Play/Pause
            ⏭ Next
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false)

        // "Media controls:" ends with colon so groups with following paragraphs
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraphs)
    }

    @Test
    fun testGroupingStrongEmphasisStartsNewGroup() {
        val content = """
            # Title

            Some intro text.
            Some more intro.

            **Bold start**
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false)

        // Two paragraphs in one group (no blank line between them);
        // STRONG paragraph starts a new group
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraphs)
        assertTrue(blocks[1] is MarkdownBlock.Paragraphs)
    }

    @Test
    fun testFaqListPageDetection() {
        val content = """
            # FAQ
            ### Question 1?
            Answer 1.
            ### Question 2?
            Answer 2.
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(false, null, "help-faq.md")

        // H1 stripped, two H3 headings become FaqQuestion blocks
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.FaqQuestion)
        assertEquals("Question 1?", (blocks[0] as MarkdownBlock.FaqQuestion).text)
        assertEquals("help-faq.md", (blocks[0] as MarkdownBlock.FaqQuestion).fileName)
        assertTrue(blocks[1] is MarkdownBlock.FaqQuestion)
        assertEquals("Question 2?", (blocks[1] as MarkdownBlock.FaqQuestion).text)
    }

    @Test
    fun testFaqSubPageGroupsAllContent() {
        val content = """
            # FAQ
            ### Question 1?
            Answer paragraph 1.
            Answer paragraph 2.
        """.trimIndent()
        val processor = MarkdownProcessor(content)
        val blocks = processor.getBlocks(true, "Question 1")

        // H3 heading + all answer paragraphs grouped into one block
        assertEquals(2, blocks.size)
        assertEquals(MarkdownBlock.Heading("Question 1?", 3), blocks[0])
        assertTrue(blocks[1] is MarkdownBlock.Paragraphs)
        val html = (blocks[1] as MarkdownBlock.Paragraphs).html
        assertTrue(html.contains("Answer paragraph 1"))
        assertTrue(html.contains("Answer paragraph 2"))
    }

    @Test
    fun testFrontMatterStripped() {
        val raw = """
            ---
            title: Test Page
            layout: page
            parent: "Using Soundscape"
            has_toc: false
            ---

            # Test Page

            Content here.
        """.trimIndent()
        val processed = processMarkdownContent(raw)
        assertFalse(processed.startsWith("---"))
        assertFalse(processed.contains("layout: page"))
        assertTrue(processed.startsWith("# Test Page"))
        assertTrue(processed.contains("Content here."))
    }

    @Test
    fun testLinkTagsResolved() {
        val raw = "See [Voices]({% link voices/help-voices.md %}) for details."
        val processed = processMarkdownContent(raw)
        assertEquals("See [Voices](help-voices.md) for details.", processed)
    }
}
