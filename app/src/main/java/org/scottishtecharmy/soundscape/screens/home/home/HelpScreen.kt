package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing


enum class SectionType{
    Title,          // A non-clickable title of a group of other text
    Link,           // A clickable text link
    Paragraph,      // A paragraph of text
    Faq             // A FAQ question with its answer in the section
}
data class Section(
    val textId: Int,                      // There's always text, this is the resource id for it
    val type: SectionType,
    val skipTalkback: Boolean = false,
    val markdown: Boolean = false,
    val faqAnswer: Int = -1             // The resource id of the answer to a FAQ question
)
data class Sections(
    val titleId: Int,
    val sections: List<Section>
)

// This is a list of all the possible Sections that can be displayed as part of the help screen.
// Each one has a unique titleId which is used to identify it in the route.
val helpPages = listOf(
    Sections(
        R.string.beacon_audio_beacon,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_destination_beacons_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_destination_beacons_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_destination_beacons_how_1, SectionType.Paragraph),
            Section(R.string.help_text_destination_beacons_how_2, SectionType.Paragraph),
            Section(R.string.help_text_destination_beacons_how_3, SectionType.Paragraph)
        )
    ),

    Sections(
        R.string.voice_voices,
        listOf(
            Section(R.string.help_config_voices_content, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.help_remote_page_title,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_remote_control_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_remote_control_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_remote_control_how, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.help_explore_page_title,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_ahead_of_me_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_ahead_of_me_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_ahead_of_me_how, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.help_orient_page_title,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_around_me_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_around_me_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_around_me_how, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.callouts_automatic_callouts,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_automatic_callouts_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_automatic_callouts_when_1, SectionType.Paragraph),
            Section(R.string.help_text_automatic_callouts_when_2, SectionType.Paragraph),
            Section(R.string.help_text_automatic_callouts_when_3, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_automatic_callouts_how_1, SectionType.Paragraph),
            Section(R.string.help_text_automatic_callouts_how_2, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.directions_my_location,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_my_location_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_my_location_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_my_location_how, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.routes_title,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_routes_content_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_routes_content_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_routes_content_how_1, SectionType.Paragraph),
            Section(R.string.help_text_routes_content_how_2, SectionType.Paragraph),
            Section(R.string.help_text_routes_content_how_3, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.callouts_nearby_markers,
        listOf(
            Section(R.string.help_text_section_title_what, SectionType.Title),
            Section(R.string.help_text_nearby_markers_what, SectionType.Paragraph),

            Section(R.string.help_text_section_title_when, SectionType.Title),
            Section(R.string.help_text_nearby_markers_when, SectionType.Paragraph),

            Section(R.string.help_text_section_title_how, SectionType.Title),
            Section(R.string.help_text_nearby_markers_how, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.markers_title,
        listOf(
            Section(R.string.help_text_markers_content_1, SectionType.Paragraph),
            Section(R.string.help_text_markers_content_2, SectionType.Paragraph),
            Section(R.string.help_text_markers_content_3, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.help_creating_markers_page_title,
        listOf(
            Section(R.string.help_text_creating_markers_content_1, SectionType.Paragraph),
            Section(R.string.help_text_creating_markers_content_2, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.help_edit_markers_page_title,
        listOf(
            Section(R.string.help_text_customizing_markers_content_1, SectionType.Paragraph),
            Section(R.string.help_text_customizing_markers_content_2, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.faq_tips_title,
        listOf(
            Section(R.string.faq_tip_finding_bus_stops, SectionType.Paragraph),
            Section(R.string.faq_tip_setting_beacon_on_address, SectionType.Paragraph),
            Section(R.string.faq_tip_create_marker_at_bus_stop, SectionType.Paragraph),
            Section(R.string.faq_tip_beacon_quiet, SectionType.Paragraph),
            Section(R.string.faq_tip_hold_phone_flat, SectionType.Paragraph),
            Section(R.string.faq_tip_turning_beacon_off, SectionType.Paragraph),
            Section(R.string.faq_tip_turning_off_auto_callouts, SectionType.Paragraph),
            //Section(R.string.faq_tip_two_finger_double_tap, SectionType.Paragraph), Currently unsupported on Android
        )
    ),

    Sections(
        R.string.help_offline_page_title,
        listOf(
            Section(R.string.help_offline_page_title, SectionType.Title),
            Section(R.string.help_offline_description, SectionType.Paragraph),
            Section(R.string.help_offline_limitations_heading, SectionType.Title),
            Section(R.string.help_offline_limitations_description, SectionType.Paragraph),
            Section(R.string.help_offline_troubleshooting_heading, SectionType.Title),
            Section(R.string.help_offline_troubleshooting_description, SectionType.Paragraph),
        )
    ),

    Sections(
        R.string.menu_help_and_tutorials,
        listOf(
            Section(R.string.help_configuration_section_title, SectionType.Title),
            Section(R.string.voice_voices, SectionType.Link),
            Section(R.string.help_remote_page_title, SectionType.Link),

            Section(R.string.settings_help_section_beacons_and_pois, SectionType.Title),
            Section(R.string.beacon_audio_beacon, SectionType.Link),
            Section(R.string.callouts_automatic_callouts, SectionType.Link),

            Section(R.string.settings_help_section_home_screen_buttons, SectionType.Title),
            Section(R.string.directions_my_location, SectionType.Link),
            Section(R.string.help_orient_page_title, SectionType.Link),
            Section(R.string.help_explore_page_title, SectionType.Link),
            Section(R.string.callouts_nearby_markers, SectionType.Link),

            Section(R.string.search_view_markers, SectionType.Title),
            Section(R.string.markers_title, SectionType.Link),
            Section(R.string.routes_title, SectionType.Link),
            Section(R.string.help_creating_markers_page_title, SectionType.Link),
            Section(R.string.help_edit_markers_page_title, SectionType.Link),

            Section(R.string.faq_title, SectionType.Title),
            Section(R.string.faq_title, SectionType.Link),
            Section(R.string.faq_tips_title, SectionType.Link),
            Section(R.string.help_offline_page_title, SectionType.Link),
        )
    ),

    Sections(
        R.string.faq_title,
        listOf(
            Section(R.string.faq_section_what_is_soundscape, SectionType.Title),
            Section(R.string.faq_when_to_use_soundscape_question, SectionType.Faq, faqAnswer = R.string.faq_when_to_use_soundscape_answer),
            Section(R.string.faq_markers_function_question, SectionType.Faq, faqAnswer = R.string.faq_markers_function_answer),

            Section(R.string.faq_section_getting_the_best_experience, SectionType.Title),
            Section(R.string.faq_what_can_I_set_question, SectionType.Faq, faqAnswer = R.string.faq_what_can_I_set_answer),
            Section(R.string.faq_how_to_use_beacon_question, SectionType.Faq, faqAnswer = R.string.faq_how_to_use_beacon_answer),
            Section(R.string.faq_why_does_beacon_disappear_question, SectionType.Faq, faqAnswer = R.string.faq_why_does_beacon_disappear_answer),
            Section(R.string.faq_beacon_on_address_question, SectionType.Faq, faqAnswer = R.string.faq_beacon_on_address_answer),
            Section(R.string.faq_beacon_on_home_question, SectionType.Faq, faqAnswer = R.string.faq_beacon_on_home_answer),
            Section(R.string.faq_how_close_to_destination_question, SectionType.Faq, faqAnswer = R.string.faq_how_close_to_destination_answer),
            Section(R.string.faq_turn_beacon_back_on_question, SectionType.Faq, faqAnswer = R.string.faq_turn_beacon_back_on_answer),
            Section(R.string.faq_road_names_question, SectionType.Faq, faqAnswer = R.string.faq_road_names_answer),
            Section(R.string.faq_why_not_every_business_question, SectionType.Faq, faqAnswer = R.string.faq_why_not_every_business_answer),
            Section(R.string.faq_callouts_stopping_in_vehicle_question, SectionType.Faq, faqAnswer = R.string.faq_callouts_stopping_in_vehicle_answer),
            Section(R.string.faq_miss_a_callout_question, SectionType.Faq, faqAnswer = R.string.faq_miss_a_callout_answer),

            Section(R.string.faq_section_how_soundscape_works, SectionType.Title),
            Section(R.string.faq_supported_phones_question, SectionType.Faq, faqAnswer = R.string.faq_supported_phones_answer),
            Section(R.string.faq_supported_headsets_question, SectionType.Faq, faqAnswer = R.string.faq_supported_headsets_answer),
            Section(R.string.faq_battery_impact_question, SectionType.Faq, faqAnswer = R.string.faq_battery_impact_answer),
            Section(R.string.faq_sleep_mode_battery_question, SectionType.Faq, faqAnswer = R.string.faq_sleep_mode_battery_answer),
            Section(R.string.faq_snooze_mode_battery_question, SectionType.Faq, faqAnswer = R.string.faq_snooze_mode_battery_answer),
            Section(R.string.faq_headset_battery_impact_question, SectionType.Faq, faqAnswer = R.string.faq_headset_battery_impact_answer),
            Section(R.string.faq_background_battery_impact_question, SectionType.Faq, faqAnswer = R.string.faq_background_battery_impact_answer),
            Section(R.string.faq_mobile_data_use_question, SectionType.Faq, faqAnswer = R.string.faq_mobile_data_use_answer),
            Section(R.string.faq_difference_from_map_apps_question, SectionType.Faq, faqAnswer = R.string.faq_difference_from_map_apps_answer),
            Section(R.string.faq_use_with_wayfinding_apps_question, SectionType.Faq, faqAnswer = R.string.faq_use_with_wayfinding_apps_answer),
            Section(R.string.faq_controlling_what_you_hear_question, SectionType.Faq, faqAnswer = R.string.faq_controlling_what_you_hear_answer),
            Section(R.string.faq_holding_phone_flat_question, SectionType.Faq, faqAnswer = R.string.faq_holding_phone_flat_answer),
            Section(R.string.faq_what_is_osm_question, SectionType.Faq, faqAnswer = R.string.faq_what_is_osm_answer),
        )
    ),
    Sections(
        R.string.settings_about_app,
        listOf(
            Section(R.string.about_soundscape, SectionType.Paragraph, skipTalkback = false, markdown = true),
            Section(R.string.copyright_notices, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.osm_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.openmaptiles_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.fmod_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.maplibre_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.junit_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),

            Section(R.string.apache_notices, SectionType.Title, skipTalkback = true),
            Section(R.string.rtree_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.realm_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.moshi_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.retrofit_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.okhttp_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.otto_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.leak_canary_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.gpx_parser_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.preferences_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(R.string.dokka_mermaid_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
        )
    )
)

@Composable
fun HelpScreen(
    topic: String,
    navController: NavHostController,
    modifier: Modifier
) {
    // Find our page
    var sections = Sections(0, emptyList())
    if (topic.startsWith("page")) {
        // Parse page title id from route
        val id = topic.substring(4).toInt()
        for (page in helpPages) {
            if(page.titleId == id) {
                sections = page
            }
        }
    } else if(topic.startsWith("faq")) {
        // Parse faq ids from route
        val ids = topic.substring(3).split(".")
        // We want to display the question and answer
        sections = Sections(R.string.faq_title_abbreviated,
            listOf(
                Section(ids[0].toInt(), SectionType.Title),
                Section(ids[1].toInt(), SectionType.Paragraph)
            )
        )
    } else {
        // Default to home
        for (page in helpPages) {
            if(page.titleId == R.string.menu_help_and_tutorials) {
                sections = page
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = stringResource(sections.titleId),
                navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                onNavigateUp = { navController.popBackStack() },
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
            ) {
                // Help topic page
                LazyColumn(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .mediumPadding(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    items(sections.sections) { section ->
                        when (section.type) {
                            SectionType.Title -> {
                                Text(
                                    text = stringResource(section.textId),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .padding(top = spacing.medium)
                                        .semantics {
                                            heading()
                                            if (section.skipTalkback)
                                                hideFromAccessibility()
                                        },
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            SectionType.Paragraph -> {
                                var htmlText = stringResource(section.textId)
                                if(section.markdown) {
                                    val parser: Parser = Parser.builder().build()
                                    val document: Node? = parser.parse(htmlText)
                                    val renderer = HtmlRenderer.builder().build()
                                    htmlText = renderer.render(document)
                                }
                                Text(
                                    text = AnnotatedString.fromHtml(
                                        htmlString = htmlText,
                                        linkStyles = TextLinkStyles(
                                            style = SpanStyle(
                                                textDecoration = TextDecoration.Underline,
                                            )
                                        )
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .semantics {
                                            if(section.skipTalkback)
                                                hideFromAccessibility()
                                        }
                                )
                            }

                            SectionType.Link, SectionType.Faq -> {
                                Button(
                                    onClick = {
                                        if (section.type == SectionType.Faq) {
                                            navController.navigate("${HomeRoutes.Help.route}/faq${section.textId}.${section.faqAnswer}")
                                        } else {
                                            navController.navigate("${HomeRoutes.Help.route}/page${section.textId}")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(spacing.extraSmall),
                                ) {
                                    Box(
                                        Modifier.weight(6f)
                                    ) {
                                        Text(
                                            text = stringResource(section.textId),
                                            textAlign = TextAlign.Start,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                    Box(
                                        Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ChevronRight,
                                            null,
                                            modifier = Modifier.align(Alignment.CenterEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeHelpPreview() {
    HelpScreen(
        topic = "page${R.string.menu_help_and_tutorials}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun BeaconHelpPreview() {
    HelpScreen(
        topic = "page${R.string.beacon_audio_beacon}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun VoicesHelpPreview() {
    HelpScreen(
        topic = "page${R.string.voice_voices}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun RemoteHelpPreview() {
    HelpScreen(
        topic = "page${R.string.help_remote_page_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AheadOfMeHelpPreview() {
    HelpScreen(
        topic = "page${R.string.help_explore_page_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AroundMeHelpPreview() {
    HelpScreen(
        topic = "page${R.string.help_orient_page_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AutomaticCalloutsHelpPreview() {
    HelpScreen(
        topic = "page${R.string.callouts_automatic_callouts}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MyLocationHelpPreview() {
    HelpScreen(
        topic = "page${R.string.directions_my_location}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesContentHelpPreview() {
    HelpScreen(
        topic = "page${R.string.routes_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersHelpPreview() {
    HelpScreen(
        topic = "page${R.string.markers_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun CreatingMarkersHelpPreview() {
    HelpScreen(
        topic = "page${R.string.help_creating_markers_page_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun NearbyMarkersHelpPreview() {
    HelpScreen(
        topic = "page${R.string.callouts_nearby_markers}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun EditingMarkersHelpPreview() {
    HelpScreen(
        topic = "page${R.string.help_edit_markers_page_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun FaqHelpPreview() {
    HelpScreen(
        topic = "page${R.string.faq_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun FaqAnswerHelpPreview() {
    HelpScreen(
        topic = "faq${R.string.faq_when_to_use_soundscape_answer}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun TipsHelpPreview() {
    HelpScreen(
        topic = "page${R.string.faq_tips_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun OfflineHelpPreview() {
    HelpScreen(
        topic = "page${R.string.help_offline_page_title}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AboutHelpPreview() {
    HelpScreen(
        topic = "page${R.string.settings_about_app}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}
