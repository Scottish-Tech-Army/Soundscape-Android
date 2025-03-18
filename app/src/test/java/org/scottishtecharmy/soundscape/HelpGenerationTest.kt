package org.scottishtecharmy.soundscape

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.scottishtecharmy.soundscape.screens.home.home.SectionType
import org.scottishtecharmy.soundscape.screens.home.home.helpPages
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class HelpGenerationTest {

    @Test
    fun getHelp() {

        val context = RuntimeEnvironment.application

        val markdownOutput = StringBuilder()
        markdownOutput.append("This page is auto-generated from the same source as the app Help screen by the `HelpGenerationTest` unit test.\n\n")
        for (page in helpPages) {
            markdownOutput.append("# ")
            markdownOutput.append(context.getString(page.titleId))
            markdownOutput.append("\n")
            for(section in page.sections) {
                if(section.type == SectionType.Faq) {
                    markdownOutput.append("\n")
                    markdownOutput.append("### ")
                    markdownOutput.append(context.getString(section.textId))
                    markdownOutput.append("\n")
                    markdownOutput.append(context.getString(section.faqAnswer))
                } else if(section.type == SectionType.Title) {
                    markdownOutput.append("\n")
                    markdownOutput.append("## ")
                    markdownOutput.append(context.getString(section.textId))
                } else {
                    markdownOutput.append("\n")
                    markdownOutput.append(context.getString(section.textId))
                }
                markdownOutput.append("\n")
            }
            markdownOutput.append("\n")
        }
        val outputFile = FileOutputStream("../docs/help.md")
        outputFile.write(markdownOutput.toString().toByteArray())
        outputFile.close()
    }
}