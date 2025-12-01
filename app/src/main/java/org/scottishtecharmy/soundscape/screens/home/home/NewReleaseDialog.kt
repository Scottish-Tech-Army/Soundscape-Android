package org.scottishtecharmy.soundscape.screens.home.home

import android.content.SharedPreferences
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity.Companion.LAST_NEW_RELEASE_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * newReleaseDialog displays text explaining what new features are available within the app
 */
@Composable
fun newReleaseDialog(
    innerPadding: PaddingValues,
    sharedPreferences: SharedPreferences,
    newReleaseDialog: MutableState<Boolean>
) {
    val markdownText = stringResource(R.string.new_version_info_details)
    val markdownSentences = remember(markdownText) {
        markdownText.split(Regex("(?<=[.!?][ \n])\\s*")).filter { it.isNotBlank() }
    }
    val parser: Parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    val htmlStrings = mutableListOf<String>()
    for (sentence in markdownSentences) {
        val document: Node? = parser.parse(sentence)
        htmlStrings.add(renderer.render(document))
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    AlertDialog(
        modifier = Modifier
            .padding(innerPadding),
        title = {
            Text(text = stringResource(R.string.new_version_info_text))
        },
        text = {
            LazyColumn(modifier = Modifier.padding(top = spacing.medium)) {
                itemsIndexed(htmlStrings) { _, sentence ->
                    Text(
                        text = AnnotatedString.fromHtml(
                            htmlString = sentence,
                            linkStyles = TextLinkStyles(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                )
                            )
                        ),
                    )
                }
            }
        },
        onDismissRequest = { },
        confirmButton = { },
        dismissButton = {
            TextButton(
                onClick = { newReleaseDialog.value = false }
            ) {
                // Remember that we've shown the dialog for this version
                sharedPreferences.edit(commit = true) {
                    putString(LAST_NEW_RELEASE_KEY, BuildConfig.VERSION_NAME.substringBeforeLast("."))
                }
                Text(text = stringResource(R.string.new_version_info_completed))
            }
        }
    )
}