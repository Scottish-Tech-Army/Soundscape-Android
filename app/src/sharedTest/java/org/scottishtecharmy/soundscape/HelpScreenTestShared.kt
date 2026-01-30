package org.scottishtecharmy.soundscape

import androidx.media3.common.util.Util.unescapeFileName
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import org.scottishtecharmy.soundscape.screens.home.home.Section
import org.scottishtecharmy.soundscape.screens.home.home.SectionType
import org.scottishtecharmy.soundscape.screens.home.home.Sections
import org.scottishtecharmy.soundscape.screens.home.home.helpPages
import java.util.Locale

object HelpScreenTestShared {
    private val helpPagesByTitleString: Map<String, Sections> by lazy {
        helpPages.associateBy { getDefaultString(it.titleId) }
    }

    private val faqPagesByTitleString: Map<String, Section> by lazy {
        val faqPage = helpPages.find { it.titleId == R.string.faq_title }!!
        val individualFaqTopics = faqPage.sections.filter { it.type == SectionType.Faq }
        individualFaqTopics.associateBy { getDefaultString(it.textId) }
    }

    /**
     * Retrieves a string resource for the default locale (e.g., from `values/strings.xml`).
     *
     * @param id The ID of the string resource to retrieve (e.g., R.string.something).
     * @return The string value from the default resource file.
     */
    private fun getDefaultString(id: Int): String {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val defaultConfig = targetContext.resources.configuration
        // Use Locale.ROOT to be explicit about the base resource
        defaultConfig.setLocale(Locale.ROOT)
        val defaultContext = targetContext.createConfigurationContext(defaultConfig)
        return defaultContext.getString(id)
    }

    fun getTopic(testTopic: String): String {
        return when {
            testTopic.startsWith("page_") -> {
                val title = unescapeFileName(testTopic.substring(5))
                val helpPage = (helpPagesByTitleString[title]
                    ?: (fail("Failed to find page with title '${title}'") as Nothing))
                "page${helpPage.titleId}"
            }
            testTopic.startsWith("faq_") -> {
                val title = unescapeFileName(testTopic.substring(4))
                val section = faqPagesByTitleString[title]
                    ?: (fail("Failed to find FAQ entry with title '${title}'") as Nothing)
                "faq${section.textId}.${section.faqAnswer}"
            }
            else -> testTopic
        }
    }

    /**
     * In this list, we start from the section title, rather than from the topic ID as it
     * would be used in the app, because (a) we want to make sure individual page titles haven't
     * changed accidentally; (b) we want to make sure the set of titles is exactly as before;
     * and (c) the integer IDs might change if help strings are added, removed, or rearranged.
     * The test itself does a lookup from the title to the IDs needed as input to the production
     * code.
     */
    fun data(): Iterable<Array<String>> {
        return arrayListOf(
            // Main help page.
            arrayOf("Help and Tutorials"),

            // Other help pages.
            arrayOf("page_Voices"),
            arrayOf("page_Using Media Controls"),
            arrayOf("page_Audio Beacon"),
            arrayOf("page_Automatic Callouts"),
            arrayOf("page_My Location"),
            arrayOf("page_Around Me"),
            arrayOf("page_Ahead of Me"),
            arrayOf("page_Nearby Markers"),
            arrayOf("page_Markers"),
            arrayOf("page_Routes"),
            arrayOf("page_Creating Markers"),
            arrayOf("page_Customizing Markers"),
            arrayOf("page_Frequently Asked Questions"),
            arrayOf("page_Tips"),
            arrayOf("page_Why is Soundscape working offline%3f"),

            // FAQ items.
            arrayOf("faq_When should I use Soundscape%3f"),
            arrayOf("faq_What are Markers and how do I get the most out of them%3f"),
            arrayOf("faq_What can I set as a beacon%3f"),
            arrayOf("faq_How do I use a beacon like a pro%3f"),
            arrayOf("faq_Why does the audible beacon disappear sometimes%3f"),
            arrayOf("faq_Can I set a beacon on an address%3f"),
            arrayOf("faq_How do I set a beacon on my home%3f"),
            arrayOf("faq_When I set a beacon on a destination, how close will Soundscape get me to the destination%3f"),
            arrayOf("faq_Can I turn the beacon back on when I am close to my destination%3f"),
            arrayOf("faq_Why does Soundscape call out road names twice when I approach an intersection%3f"),
            arrayOf("faq_Why doesn’t Soundscape announce every business that I pass%3f"),
            arrayOf("faq_Why do some callouts stop when I'm in a vehicle%3f"),
            arrayOf("faq_What if I don't understand a callout or miss it because of ambient noise%3f"),
            arrayOf("faq_What phone does Soundscape run on%3f"),
            arrayOf("faq_What headphones should I use with Soundscape%3f"),
            arrayOf("faq_How does Soundscape impact my phone’s battery%3f"),
            arrayOf("faq_How do I use Sleep Mode to minimize Soundscape’s impact on my phone battery%3f"),
            arrayOf("faq_How do I use Snooze mode to minimize Soundscape’s impact on my phone battery%3f"),
            arrayOf("faq_How does my choice of headphones affect the battery life of my phone%3f"),
            arrayOf("faq_How does running Soundscape in the background impact battery life of my phone%3f"),
            arrayOf("faq_How much mobile data does Soundscape use%3f"),
            arrayOf("faq_How is Soundscape different from other map apps%3f"),
            arrayOf("faq_How do I use Soundscape with a wayfinding app%3f"),
            arrayOf("faq_How do I control what I hear and when I hear it in Soundscape%3f"),
            arrayOf("faq_Do I need to hold the phone in my hand all the time%3f"),
            arrayOf("faq_What is Open Street Map and why do we use it for Soundscape%3f"),
        )
    }
}
