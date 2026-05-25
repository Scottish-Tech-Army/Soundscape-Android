package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.feedback.FeedbackQuestion
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.spacing

const val FEEDBACK_CHOICE_EASY = "easy"
const val FEEDBACK_CHOICE_OK = "ok"
const val FEEDBACK_CHOICE_HARD = "hard"

@Composable
fun FeedbackDialog(
    question: FeedbackQuestion,
    onSubmit: (choice: String, freeText: String?) -> Unit,
    onSkip: () -> Unit,
) {
    val paneTitleText = stringResource(R.string.feedback_dialog_pane_title_with_question, question.text)
    val choiceHint = stringResource(R.string.feedback_choice_hint)
    val skipHint = stringResource(R.string.feedback_skip_hint)

    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSkip,
        modifier = Modifier.semantics { paneTitle = paneTitleText },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    ChoiceButton(
                        text = stringResource(R.string.feedback_choice_easy),
                        hint = choiceHint,
                        modifier = Modifier.weight(1f),
                        onClick = { onSubmit(FEEDBACK_CHOICE_EASY, comment.takeIf { it.isNotBlank() }) },
                    )
                    ChoiceButton(
                        text = stringResource(R.string.feedback_choice_ok),
                        hint = choiceHint,
                        modifier = Modifier.weight(1f),
                        onClick = { onSubmit(FEEDBACK_CHOICE_OK, comment.takeIf { it.isNotBlank() }) },
                    )
                    ChoiceButton(
                        text = stringResource(R.string.feedback_choice_hard),
                        hint = choiceHint,
                        modifier = Modifier.weight(1f),
                        onClick = { onSubmit(FEEDBACK_CHOICE_HARD, comment.takeIf { it.isNotBlank() }) },
                    )
                }
                CustomTextField(
                    fieldName = stringResource(R.string.feedback_comment_label),
                    fieldHint = stringResource(R.string.feedback_comment_hint),
                    value = comment,
                    onValueChange = { comment = it },
                    isSingleLine = false,
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.talkbackHint(skipHint),
            ) {
                Text(text = stringResource(R.string.feedback_skip))
            }
        },
    )
}

@Composable
private fun ChoiceButton(
    text: String,
    hint: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    CustomButton(
        modifier = modifier.talkbackHint(hint),
        onClick = onClick,
        text = text,
        shape = RoundedCornerShape(spacing.small),
        textStyle = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
}

@Preview
@Composable
private fun FeedbackDialogPreview() {
    FeedbackDialog(
        question = FeedbackQuestion(
            id = "route_followability_v1",
            text = "How easy was it to follow your route just now?",
        ),
        onSubmit = { _, _ -> },
        onSkip = {},
    )
}
