package org.scottishtecharmy.soundscape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The help desk address is defined twice: SUPPORT_EMAIL for the apps, and support_email in
 * docs/_config.yml for the website, whose pages use it as {{ site.support_email }}. Unit tests
 * run in app/, so the docs are one level up.
 */
class SupportEmailTest {

    private val docs = File("../docs")

    @Test
    fun docsSiteUsesTheSameAddressAsTheApps() {
        val config = File(docs, "_config.yml").readText()
        val match = Regex("""^support_email:\s*(\S+)\s*$""", RegexOption.MULTILINE).find(config)
        assertTrue("docs/_config.yml has no support_email", match != null)
        assertEquals(
            "docs/_config.yml support_email differs from SUPPORT_EMAIL",
            SUPPORT_EMAIL,
            match!!.groupValues[1]
        )
    }

    @Test
    fun docsPagesDoNotSpellOutTheAddress() {
        val pages = docs.walkTopDown()
            .onEnter { it.name != "_site" && it.name != "vendor" && !it.name.startsWith(".") }
            .filter { it.isFile && (it.extension == "md" || it.extension == "html") }
            .filter { SUPPORT_EMAIL in it.readText() }
            .map { it.relativeTo(docs).path }
            .toList()
        assertTrue("Use {{ site.support_email }} instead of the address in: $pages", pages.isEmpty())
    }
}
