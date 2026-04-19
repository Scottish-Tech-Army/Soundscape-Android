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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jetbrains.compose.resources.StringResource
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.resources.*


enum class SectionType{
    Title,          // A non-clickable title of a group of other text
    Link,           // A clickable text link
    Paragraph,      // A paragraph of text
    Faq             // A FAQ question with its answer in the section
}
data class Section(
    val textId: StringResource,           // There's always text, this is the resource id for it
    val type: SectionType,
    val skipTalkback: Boolean = false,
    val markdown: Boolean = false,
    val faqAnswer: StringResource? = null  // The resource id of the answer to a FAQ question
)
data class Sections(
    val titleId: StringResource,
    val sections: List<Section>
)

// Build a lookup map from StringResource key to StringResource for all resources used in helpPages.
// This is populated lazily after helpPages is initialized.
private val stringResourceByKey: Map<String, StringResource> by lazy {
    val map = mutableMapOf<String, StringResource>()
    for (page in helpPages) {
        map[page.titleId.key] = page.titleId
        for (section in page.sections) {
            map[section.textId.key] = section.textId
            section.faqAnswer?.let { map[it.key] = it }
        }
    }
    map
}

fun findStringResourceByKey(key: String): StringResource? = stringResourceByKey[key]

// This is a list of all the possible Sections that can be displayed as part of the help screen.
// Each one has a unique titleId which is used to identify it in the route.
val helpPages = listOf(
    Sections(
        Res.string.beacon_audio_beacon,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_destination_beacons_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_destination_beacons_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_destination_beacons_how_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_destination_beacons_how_2, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_destination_beacons_how_3, SectionType.Paragraph, markdown = true)
        )
    ),

    Sections(
        Res.string.voice_voices,
        listOf(
            Section(Res.string.help_config_voices_content, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.help_remote_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_remote_control_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_remote_control_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_remote_control_how, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.help_explore_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_ahead_of_me_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_ahead_of_me_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_ahead_of_me_how, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.help_orient_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_around_me_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_around_me_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_around_me_how, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.callouts_automatic_callouts,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_automatic_callouts_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_automatic_callouts_when_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_automatic_callouts_when_2, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_automatic_callouts_when_3, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_automatic_callouts_how_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_automatic_callouts_how_2, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.directions_my_location,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_my_location_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_my_location_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_my_location_how, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.routes_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_routes_content_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_routes_content_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_routes_content_how_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_routes_content_how_2, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_routes_content_how_3, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.callouts_nearby_markers,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_nearby_markers_what, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_nearby_markers_when, SectionType.Paragraph, markdown = true),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_nearby_markers_how, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.markers_title,
        listOf(
            Section(Res.string.help_text_markers_content_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_markers_content_2, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_markers_content_3, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.help_creating_markers_page_title,
        listOf(
            Section(Res.string.help_text_creating_markers_content_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_creating_markers_content_2, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.help_edit_markers_page_title,
        listOf(
            Section(Res.string.help_text_customizing_markers_content_1, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_text_customizing_markers_content_2, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.faq_tips_title,
        listOf(
            Section(Res.string.faq_tip_finding_bus_stops, SectionType.Paragraph, markdown = true),
            Section(Res.string.faq_tip_setting_beacon_on_address, SectionType.Paragraph, markdown = true),
            Section(Res.string.faq_tip_create_marker_at_bus_stop, SectionType.Paragraph, markdown = true),
            Section(Res.string.faq_tip_beacon_quiet, SectionType.Paragraph, markdown = true),
            Section(Res.string.faq_tip_hold_phone_flat, SectionType.Paragraph, markdown = true),
            Section(Res.string.faq_tip_turning_beacon_off, SectionType.Paragraph, markdown = true),
            Section(Res.string.faq_tip_turning_off_auto_callouts, SectionType.Paragraph, markdown = true),
            //Section(Res.string.faq_tip_two_finger_double_tap, SectionType.Paragraph), Currently unsupported on Android
        )
    ),

    Sections(
        Res.string.help_offline_page_title,
        listOf(
            Section(Res.string.help_offline_page_title, SectionType.Title),
            Section(Res.string.help_offline_description, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_offline_limitations_heading, SectionType.Title),
            Section(Res.string.help_offline_limitations_description, SectionType.Paragraph, markdown = true),
            Section(Res.string.help_offline_troubleshooting_heading, SectionType.Title),
            Section(Res.string.help_offline_troubleshooting_description, SectionType.Paragraph, markdown = true),
        )
    ),

    Sections(
        Res.string.menu_help,
        listOf(
            Section(Res.string.help_configuration_section_title, SectionType.Title),
            Section(Res.string.voice_voices, SectionType.Link),
            Section(Res.string.help_remote_page_title, SectionType.Link),

            Section(Res.string.settings_help_section_beacons_and_pois, SectionType.Title),
            Section(Res.string.beacon_audio_beacon, SectionType.Link),
            Section(Res.string.callouts_automatic_callouts, SectionType.Link),

            Section(Res.string.settings_help_section_home_screen_buttons, SectionType.Title),
            Section(Res.string.directions_my_location, SectionType.Link),
            Section(Res.string.help_orient_page_title, SectionType.Link),
            Section(Res.string.help_explore_page_title, SectionType.Link),
            Section(Res.string.callouts_nearby_markers, SectionType.Link),

            Section(Res.string.search_view_markers, SectionType.Title),
            Section(Res.string.markers_title, SectionType.Link),
            Section(Res.string.routes_title, SectionType.Link),
            Section(Res.string.help_creating_markers_page_title, SectionType.Link),
            Section(Res.string.help_edit_markers_page_title, SectionType.Link),

            Section(Res.string.faq_title, SectionType.Title),
            Section(Res.string.faq_title, SectionType.Link),
            Section(Res.string.faq_tips_title, SectionType.Link),
            Section(Res.string.help_offline_page_title, SectionType.Link),
        )
    ),

    Sections(
        Res.string.faq_title,
        listOf(
            Section(Res.string.faq_section_what_is_soundscape, SectionType.Title),
            Section(Res.string.faq_when_to_use_soundscape_question, SectionType.Faq, faqAnswer = Res.string.faq_when_to_use_soundscape_answer),
            Section(Res.string.faq_markers_function_question, SectionType.Faq, faqAnswer = Res.string.faq_markers_function_answer),

            Section(Res.string.faq_section_getting_the_best_experience, SectionType.Title),
            Section(Res.string.faq_what_can_I_set_question, SectionType.Faq, faqAnswer = Res.string.faq_what_can_I_set_answer),
            Section(Res.string.faq_how_to_use_beacon_question, SectionType.Faq, faqAnswer = Res.string.faq_how_to_use_beacon_answer),
            Section(Res.string.faq_why_does_beacon_disappear_question, SectionType.Faq, faqAnswer = Res.string.faq_why_does_beacon_disappear_answer),
            Section(Res.string.faq_beacon_on_address_question, SectionType.Faq, faqAnswer = Res.string.faq_beacon_on_address_answer),
            Section(Res.string.faq_beacon_on_home_question, SectionType.Faq, faqAnswer = Res.string.faq_beacon_on_home_answer),
            Section(Res.string.faq_how_close_to_destination_question, SectionType.Faq, faqAnswer = Res.string.faq_how_close_to_destination_answer),
            Section(Res.string.faq_turn_beacon_back_on_question, SectionType.Faq, faqAnswer = Res.string.faq_turn_beacon_back_on_answer),
            Section(Res.string.faq_road_names_question, SectionType.Faq, faqAnswer = Res.string.faq_road_names_answer),
            Section(Res.string.faq_why_not_every_business_question, SectionType.Faq, faqAnswer = Res.string.faq_why_not_every_business_answer),
            Section(Res.string.faq_callouts_stopping_in_vehicle_question, SectionType.Faq, faqAnswer = Res.string.faq_callouts_stopping_in_vehicle_answer),
            Section(Res.string.faq_miss_a_callout_question, SectionType.Faq, faqAnswer = Res.string.faq_miss_a_callout_answer),

            Section(Res.string.faq_section_how_soundscape_works, SectionType.Title),
            Section(Res.string.faq_supported_phones_question, SectionType.Faq, faqAnswer = Res.string.faq_supported_phones_answer),
            Section(Res.string.faq_supported_headsets_question, SectionType.Faq, faqAnswer = Res.string.faq_supported_headsets_answer),
            Section(Res.string.faq_battery_impact_question, SectionType.Faq, faqAnswer = Res.string.faq_battery_impact_answer),
            Section(Res.string.faq_sleep_mode_battery_question, SectionType.Faq, faqAnswer = Res.string.faq_sleep_mode_battery_answer),
            Section(Res.string.faq_snooze_mode_battery_question, SectionType.Faq, faqAnswer = Res.string.faq_snooze_mode_battery_answer),
            Section(Res.string.faq_headset_battery_impact_question, SectionType.Faq, faqAnswer = Res.string.faq_headset_battery_impact_answer),
            Section(Res.string.faq_background_battery_impact_question, SectionType.Faq, faqAnswer = Res.string.faq_background_battery_impact_answer),
            Section(Res.string.faq_mobile_data_use_question, SectionType.Faq, faqAnswer = Res.string.faq_mobile_data_use_answer),
            Section(Res.string.faq_difference_from_map_apps_question, SectionType.Faq, faqAnswer = Res.string.faq_difference_from_map_apps_answer),
            Section(Res.string.faq_use_with_wayfinding_apps_question, SectionType.Faq, faqAnswer = Res.string.faq_use_with_wayfinding_apps_answer),
            Section(Res.string.faq_controlling_what_you_hear_question, SectionType.Faq, faqAnswer = Res.string.faq_controlling_what_you_hear_answer),
            Section(Res.string.faq_holding_phone_flat_question, SectionType.Faq, faqAnswer = Res.string.faq_holding_phone_flat_answer),
            Section(Res.string.faq_what_is_osm_question, SectionType.Faq, faqAnswer = Res.string.faq_what_is_osm_answer),
        )
    ),
    Sections(
        Res.string.settings_about_app,
        listOf(
            Section(Res.string.about_soundscape, SectionType.Paragraph, skipTalkback = false, markdown = true),
            Section(Res.string.copyright_notices, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(Res.string.osm_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
            Section(Res.string.openmaptiles_copyright, SectionType.Paragraph, skipTalkback = true, markdown = true),
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
    var sections = Sections(Res.string.menu_help, emptyList())
    if (topic.startsWith("page")) {
        // Parse page title key from route
        val key = topic.substring(4)
        for (page in helpPages) {
            if(page.titleId.key == key) {
                sections = page
            }
        }
    } else if(topic.startsWith("faq")) {
        // Parse faq keys from route
        val keys = topic.substring(3).split(".")
        // We want to display the question and answer
        val questionRes = findStringResourceByKey(keys[0])
        val answerRes = findStringResourceByKey(keys[1])
        if (questionRes != null && answerRes != null) {
            sections = Sections(Res.string.faq_title_abbreviated,
                listOf(
                    Section(questionRes, SectionType.Title),
                    Section(answerRes, SectionType.Paragraph)
                )
            )
        }
    } else {
        // Default to home
        for (page in helpPages) {
            if(page.titleId == Res.string.menu_help) {
                sections = page
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = stringResource(sections.titleId),
                navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
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
                                                invisibleToUser()
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
                                                invisibleToUser()
                                        }
                                )
                            }

                            SectionType.Link, SectionType.Faq -> {
                                Button(
                                    onClick = {
                                        if (section.type == SectionType.Faq) {
                                            navController.navigate("${HomeRoutes.Help.route}/faq${section.textId.key}.${section.faqAnswer?.key}")
                                        } else {
                                            navController.navigate("${HomeRoutes.Help.route}/page${section.textId.key}")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(spacing.extraSmall),
                                    colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
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
                    item {
                        if (sections.titleId == Res.string.settings_about_app) {
                            CustomButton(
                                onClick = { navController.navigate(HomeRoutes.OpenSourceLicense.route) },
                                text = stringResource(Res.string.menu_open_source_licenses),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .smallPadding(),
                                shape = RoundedCornerShape(spacing.extraSmall),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
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
        topic = "page${Res.string.menu_help.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun BeaconHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.beacon_audio_beacon.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun VoicesHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.voice_voices.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun RemoteHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.help_remote_page_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AheadOfMeHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.help_explore_page_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AroundMeHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.help_orient_page_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AutomaticCalloutsHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.callouts_automatic_callouts.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MyLocationHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.directions_my_location.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesContentHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.routes_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.markers_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun CreatingMarkersHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.help_creating_markers_page_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun NearbyMarkersHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.callouts_nearby_markers.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun EditingMarkersHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.help_edit_markers_page_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun FaqHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.faq_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun FaqAnswerHelpPreview() {
    HelpScreen(
        topic = "faq${Res.string.faq_when_to_use_soundscape_question.key}.${Res.string.faq_when_to_use_soundscape_answer.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun TipsHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.faq_tips_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun OfflineHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.help_offline_page_title.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AboutHelpPreview() {
    HelpScreen(
        topic = "page${Res.string.settings_about_app.key}",
        navController = rememberNavController(),
        modifier = Modifier
    )
}
