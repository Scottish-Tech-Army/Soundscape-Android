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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.platform.isIos
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.about_soundscape
import org.scottishtecharmy.soundscape.resources.beacon_audio_beacon
import org.scottishtecharmy.soundscape.resources.callouts_automatic_callouts
import org.scottishtecharmy.soundscape.resources.callouts_nearby_markers
import org.scottishtecharmy.soundscape.resources.copyright_notices
import org.scottishtecharmy.soundscape.resources.directions_my_location
import org.scottishtecharmy.soundscape.resources.faq_background_battery_impact_answer
import org.scottishtecharmy.soundscape.resources.faq_background_battery_impact_question
import org.scottishtecharmy.soundscape.resources.faq_battery_impact_answer
import org.scottishtecharmy.soundscape.resources.faq_battery_impact_question
import org.scottishtecharmy.soundscape.resources.faq_beacon_on_address_answer
import org.scottishtecharmy.soundscape.resources.faq_beacon_on_address_question
import org.scottishtecharmy.soundscape.resources.faq_beacon_on_home_answer
import org.scottishtecharmy.soundscape.resources.faq_beacon_on_home_question
import org.scottishtecharmy.soundscape.resources.faq_callouts_stopping_in_vehicle_answer
import org.scottishtecharmy.soundscape.resources.faq_callouts_stopping_in_vehicle_question
import org.scottishtecharmy.soundscape.resources.faq_controlling_what_you_hear_answer
import org.scottishtecharmy.soundscape.resources.faq_controlling_what_you_hear_question
import org.scottishtecharmy.soundscape.resources.faq_difference_from_map_apps_answer
import org.scottishtecharmy.soundscape.resources.faq_difference_from_map_apps_question
import org.scottishtecharmy.soundscape.resources.faq_headset_battery_impact_answer
import org.scottishtecharmy.soundscape.resources.faq_headset_battery_impact_question
import org.scottishtecharmy.soundscape.resources.faq_holding_phone_flat_answer
import org.scottishtecharmy.soundscape.resources.faq_holding_phone_flat_question
import org.scottishtecharmy.soundscape.resources.faq_how_close_to_destination_answer
import org.scottishtecharmy.soundscape.resources.faq_how_close_to_destination_question
import org.scottishtecharmy.soundscape.resources.faq_how_to_use_beacon_answer
import org.scottishtecharmy.soundscape.resources.faq_how_to_use_beacon_question
import org.scottishtecharmy.soundscape.resources.faq_markers_function_answer
import org.scottishtecharmy.soundscape.resources.faq_markers_function_question
import org.scottishtecharmy.soundscape.resources.faq_miss_a_callout_answer
import org.scottishtecharmy.soundscape.resources.faq_miss_a_callout_question
import org.scottishtecharmy.soundscape.resources.faq_mobile_data_use_answer
import org.scottishtecharmy.soundscape.resources.faq_mobile_data_use_question
import org.scottishtecharmy.soundscape.resources.faq_road_names_answer
import org.scottishtecharmy.soundscape.resources.faq_road_names_question
import org.scottishtecharmy.soundscape.resources.faq_section_getting_the_best_experience
import org.scottishtecharmy.soundscape.resources.faq_section_how_soundscape_works
import org.scottishtecharmy.soundscape.resources.faq_section_what_is_soundscape
import org.scottishtecharmy.soundscape.resources.faq_sleep_mode_battery_answer
import org.scottishtecharmy.soundscape.resources.faq_sleep_mode_battery_question
import org.scottishtecharmy.soundscape.resources.faq_snooze_mode_battery_answer
import org.scottishtecharmy.soundscape.resources.faq_snooze_mode_battery_question
import org.scottishtecharmy.soundscape.resources.faq_supported_headsets_answer
import org.scottishtecharmy.soundscape.resources.faq_supported_headsets_question
import org.scottishtecharmy.soundscape.resources.faq_supported_phones_answer
import org.scottishtecharmy.soundscape.resources.faq_supported_phones_question
import org.scottishtecharmy.soundscape.resources.faq_tip_beacon_quiet
import org.scottishtecharmy.soundscape.resources.faq_tip_create_marker_at_bus_stop
import org.scottishtecharmy.soundscape.resources.faq_tip_finding_bus_stops
import org.scottishtecharmy.soundscape.resources.faq_tip_hold_phone_flat
import org.scottishtecharmy.soundscape.resources.faq_tip_setting_beacon_on_address
import org.scottishtecharmy.soundscape.resources.faq_tip_turning_beacon_off
import org.scottishtecharmy.soundscape.resources.faq_tip_turning_off_auto_callouts
import org.scottishtecharmy.soundscape.resources.faq_tips_title
import org.scottishtecharmy.soundscape.resources.faq_title
import org.scottishtecharmy.soundscape.resources.faq_title_abbreviated
import org.scottishtecharmy.soundscape.resources.faq_turn_beacon_back_on_answer
import org.scottishtecharmy.soundscape.resources.faq_turn_beacon_back_on_question
import org.scottishtecharmy.soundscape.resources.faq_use_with_wayfinding_apps_answer
import org.scottishtecharmy.soundscape.resources.faq_use_with_wayfinding_apps_question
import org.scottishtecharmy.soundscape.resources.faq_what_can_I_set_answer
import org.scottishtecharmy.soundscape.resources.faq_what_can_I_set_question
import org.scottishtecharmy.soundscape.resources.faq_what_is_osm_answer
import org.scottishtecharmy.soundscape.resources.faq_what_is_osm_question
import org.scottishtecharmy.soundscape.resources.faq_when_to_use_soundscape_answer
import org.scottishtecharmy.soundscape.resources.faq_when_to_use_soundscape_question
import org.scottishtecharmy.soundscape.resources.faq_why_does_beacon_disappear_answer
import org.scottishtecharmy.soundscape.resources.faq_why_does_beacon_disappear_question
import org.scottishtecharmy.soundscape.resources.faq_why_not_every_business_answer
import org.scottishtecharmy.soundscape.resources.faq_why_not_every_business_question
import org.scottishtecharmy.soundscape.resources.help_assistant_page_title
import org.scottishtecharmy.soundscape.resources.help_config_voices_content
import org.scottishtecharmy.soundscape.resources.help_config_voices_content_ios
import org.scottishtecharmy.soundscape.resources.help_configuration_section_title
import org.scottishtecharmy.soundscape.resources.help_creating_markers_page_title
import org.scottishtecharmy.soundscape.resources.help_edit_markers_page_title
import org.scottishtecharmy.soundscape.resources.help_explore_page_title
import org.scottishtecharmy.soundscape.resources.help_offline_description
import org.scottishtecharmy.soundscape.resources.help_offline_limitations_description
import org.scottishtecharmy.soundscape.resources.help_offline_limitations_heading
import org.scottishtecharmy.soundscape.resources.help_offline_page_title
import org.scottishtecharmy.soundscape.resources.help_offline_troubleshooting_description
import org.scottishtecharmy.soundscape.resources.help_offline_troubleshooting_heading
import org.scottishtecharmy.soundscape.resources.help_orient_page_title
import org.scottishtecharmy.soundscape.resources.help_remote_page_title
import org.scottishtecharmy.soundscape.resources.help_text_ahead_of_me_how
import org.scottishtecharmy.soundscape.resources.help_text_ahead_of_me_what
import org.scottishtecharmy.soundscape.resources.help_text_ahead_of_me_when
import org.scottishtecharmy.soundscape.resources.help_text_around_me_how
import org.scottishtecharmy.soundscape.resources.help_text_around_me_what
import org.scottishtecharmy.soundscape.resources.help_text_around_me_when
import org.scottishtecharmy.soundscape.resources.help_text_assistant_commands
import org.scottishtecharmy.soundscape.resources.help_text_assistant_commands_ios
import org.scottishtecharmy.soundscape.resources.help_text_assistant_how
import org.scottishtecharmy.soundscape.resources.help_text_assistant_how_ios
import org.scottishtecharmy.soundscape.resources.help_text_assistant_notes
import org.scottishtecharmy.soundscape.resources.help_text_assistant_notes_ios
import org.scottishtecharmy.soundscape.resources.help_text_assistant_what
import org.scottishtecharmy.soundscape.resources.help_text_assistant_what_ios
import org.scottishtecharmy.soundscape.resources.help_text_assistant_when
import org.scottishtecharmy.soundscape.resources.help_text_automatic_callouts_how_1
import org.scottishtecharmy.soundscape.resources.help_text_automatic_callouts_how_2
import org.scottishtecharmy.soundscape.resources.help_text_automatic_callouts_what
import org.scottishtecharmy.soundscape.resources.help_text_automatic_callouts_when_1
import org.scottishtecharmy.soundscape.resources.help_text_automatic_callouts_when_2
import org.scottishtecharmy.soundscape.resources.help_text_automatic_callouts_when_3
import org.scottishtecharmy.soundscape.resources.help_text_creating_markers_content_1
import org.scottishtecharmy.soundscape.resources.help_text_creating_markers_content_2
import org.scottishtecharmy.soundscape.resources.help_text_customizing_markers_content_1
import org.scottishtecharmy.soundscape.resources.help_text_customizing_markers_content_2
import org.scottishtecharmy.soundscape.resources.help_text_destination_beacons_how_1
import org.scottishtecharmy.soundscape.resources.help_text_destination_beacons_how_2
import org.scottishtecharmy.soundscape.resources.help_text_destination_beacons_how_3
import org.scottishtecharmy.soundscape.resources.help_text_destination_beacons_what
import org.scottishtecharmy.soundscape.resources.help_text_destination_beacons_when
import org.scottishtecharmy.soundscape.resources.help_text_markers_content_1
import org.scottishtecharmy.soundscape.resources.help_text_markers_content_2
import org.scottishtecharmy.soundscape.resources.help_text_markers_content_3
import org.scottishtecharmy.soundscape.resources.help_text_my_location_how
import org.scottishtecharmy.soundscape.resources.help_text_my_location_what
import org.scottishtecharmy.soundscape.resources.help_text_my_location_when
import org.scottishtecharmy.soundscape.resources.help_text_nearby_markers_how
import org.scottishtecharmy.soundscape.resources.help_text_nearby_markers_what
import org.scottishtecharmy.soundscape.resources.help_text_nearby_markers_when
import org.scottishtecharmy.soundscape.resources.help_text_remote_control_how
import org.scottishtecharmy.soundscape.resources.help_text_remote_control_what
import org.scottishtecharmy.soundscape.resources.help_text_remote_control_when
import org.scottishtecharmy.soundscape.resources.help_text_routes_content_how_1
import org.scottishtecharmy.soundscape.resources.help_text_routes_content_how_2
import org.scottishtecharmy.soundscape.resources.help_text_routes_content_how_3
import org.scottishtecharmy.soundscape.resources.help_text_routes_content_what
import org.scottishtecharmy.soundscape.resources.help_text_routes_content_when
import org.scottishtecharmy.soundscape.resources.help_text_section_title_how
import org.scottishtecharmy.soundscape.resources.help_text_section_title_what
import org.scottishtecharmy.soundscape.resources.help_text_section_title_when
import org.scottishtecharmy.soundscape.resources.markers_title
import org.scottishtecharmy.soundscape.resources.menu_help
import org.scottishtecharmy.soundscape.resources.menu_open_source_licenses
import org.scottishtecharmy.soundscape.resources.openmaptiles_copyright
import org.scottishtecharmy.soundscape.resources.oboe_copyright
import org.scottishtecharmy.soundscape.resources.osm_copyright
import org.scottishtecharmy.soundscape.resources.routes_title
import org.scottishtecharmy.soundscape.resources.search_view_markers
import org.scottishtecharmy.soundscape.resources.settings_about_app
import org.scottishtecharmy.soundscape.resources.settings_help_section_beacons_and_pois
import org.scottishtecharmy.soundscape.resources.settings_help_section_home_screen_buttons
import org.scottishtecharmy.soundscape.resources.steam_copyright
import org.scottishtecharmy.soundscape.resources.trademark_disclaimer
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.resources.voice_voices
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

enum class SectionType {
    Title,          // A non-clickable title of a group of other text
    Link,           // A clickable text link
    Paragraph,      // A paragraph of text
    Faq             // A FAQ question with its answer in the section
}

data class Section(
    val textId: StringResource,           // There's always text, this is the resource id for it
    val type: SectionType,
    val skipTalkback: Boolean = false,
    val faqAnswer: StringResource? = null  // The resource id of the answer to a FAQ question
)

data class Sections(
    val titleId: StringResource,
    val sections: List<Section>
)

private fun platformString(android: StringResource, ios: StringResource): StringResource =
    if (isIos) ios else android

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

private fun markdownToHtml(markdown: String): String {
    val flavour = CommonMarkFlavourDescriptor()
    val tree = MarkdownParser(flavour, true, CancellationToken.NonCancellable)
        .buildMarkdownTreeFromString(markdown as CharSequence)
    return HtmlGenerator(markdown, tree, flavour).generateHtml()
}

// This is a list of all the possible Sections that can be displayed as part of the help screen.
// Each one has a unique titleId which is used to identify it in the route.
val helpPages = listOf(
    Sections(
        Res.string.beacon_audio_beacon,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_destination_beacons_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_destination_beacons_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_destination_beacons_how_1, SectionType.Paragraph),
            Section(Res.string.help_text_destination_beacons_how_2, SectionType.Paragraph),
            Section(Res.string.help_text_destination_beacons_how_3, SectionType.Paragraph)
        )
    ),

    Sections(
        Res.string.voice_voices,
        listOf(
            Section(
                platformString(
                    Res.string.help_config_voices_content,
                    Res.string.help_config_voices_content_ios
                ),
                SectionType.Paragraph,
            ),
        )
    ),

    Sections(
        Res.string.help_remote_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_remote_control_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_remote_control_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_remote_control_how, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.help_assistant_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(
                platformString(
                    Res.string.help_text_assistant_what,
                    Res.string.help_text_assistant_what_ios
                ),
                SectionType.Paragraph,
            ),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_assistant_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(
                platformString(
                    Res.string.help_text_assistant_how,
                    Res.string.help_text_assistant_how_ios
                ),
                SectionType.Paragraph,
            ),
            Section(
                platformString(
                    Res.string.help_text_assistant_commands,
                    Res.string.help_text_assistant_commands_ios
                ),
                SectionType.Paragraph,
            ),
            Section(
                platformString(
                    Res.string.help_text_assistant_notes,
                    Res.string.help_text_assistant_notes_ios
                ),
                SectionType.Paragraph,
            ),
        )
    ),

    Sections(
        Res.string.help_explore_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_ahead_of_me_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_ahead_of_me_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_ahead_of_me_how, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.help_orient_page_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_around_me_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_around_me_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_around_me_how, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.callouts_automatic_callouts,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_automatic_callouts_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_automatic_callouts_when_1, SectionType.Paragraph),
            Section(Res.string.help_text_automatic_callouts_when_2, SectionType.Paragraph),
            Section(Res.string.help_text_automatic_callouts_when_3, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_automatic_callouts_how_1, SectionType.Paragraph),
            Section(Res.string.help_text_automatic_callouts_how_2, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.directions_my_location,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_my_location_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_my_location_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_my_location_how, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.routes_title,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_routes_content_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_routes_content_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_routes_content_how_1, SectionType.Paragraph),
            Section(Res.string.help_text_routes_content_how_2, SectionType.Paragraph),
            Section(Res.string.help_text_routes_content_how_3, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.callouts_nearby_markers,
        listOf(
            Section(Res.string.help_text_section_title_what, SectionType.Title),
            Section(Res.string.help_text_nearby_markers_what, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_when, SectionType.Title),
            Section(Res.string.help_text_nearby_markers_when, SectionType.Paragraph),

            Section(Res.string.help_text_section_title_how, SectionType.Title),
            Section(Res.string.help_text_nearby_markers_how, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.markers_title,
        listOf(
            Section(Res.string.help_text_markers_content_1, SectionType.Paragraph),
            Section(Res.string.help_text_markers_content_2, SectionType.Paragraph),
            Section(Res.string.help_text_markers_content_3, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.help_creating_markers_page_title,
        listOf(
            Section(Res.string.help_text_creating_markers_content_1, SectionType.Paragraph),
            Section(Res.string.help_text_creating_markers_content_2, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.help_edit_markers_page_title,
        listOf(
            Section(Res.string.help_text_customizing_markers_content_1, SectionType.Paragraph),
            Section(Res.string.help_text_customizing_markers_content_2, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.faq_tips_title,
        listOf(
            Section(Res.string.faq_tip_finding_bus_stops, SectionType.Paragraph),
            Section(Res.string.faq_tip_setting_beacon_on_address, SectionType.Paragraph),
            Section(Res.string.faq_tip_create_marker_at_bus_stop, SectionType.Paragraph),
            Section(Res.string.faq_tip_beacon_quiet, SectionType.Paragraph),
            Section(Res.string.faq_tip_hold_phone_flat, SectionType.Paragraph),
            Section(Res.string.faq_tip_turning_beacon_off, SectionType.Paragraph),
            Section(Res.string.faq_tip_turning_off_auto_callouts, SectionType.Paragraph),
            //Section(Res.string.faq_tip_two_finger_double_tap, SectionType.Paragraph), Currently unsupported on Android
        )
    ),

    Sections(
        Res.string.help_offline_page_title,
        listOf(
            Section(Res.string.help_offline_page_title, SectionType.Title),
            Section(Res.string.help_offline_description, SectionType.Paragraph),
            Section(Res.string.help_offline_limitations_heading, SectionType.Title),
            Section(Res.string.help_offline_limitations_description, SectionType.Paragraph),
            Section(Res.string.help_offline_troubleshooting_heading, SectionType.Title),
            Section(Res.string.help_offline_troubleshooting_description, SectionType.Paragraph),
        )
    ),

    Sections(
        Res.string.menu_help,
        listOf(
            Section(Res.string.help_configuration_section_title, SectionType.Title),
            Section(Res.string.voice_voices, SectionType.Link),
            Section(Res.string.help_remote_page_title, SectionType.Link),
            Section(Res.string.help_assistant_page_title, SectionType.Link),

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
            Section(
                Res.string.faq_when_to_use_soundscape_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_when_to_use_soundscape_answer
            ),
            Section(
                Res.string.faq_markers_function_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_markers_function_answer
            ),

            Section(Res.string.faq_section_getting_the_best_experience, SectionType.Title),
            Section(
                Res.string.faq_what_can_I_set_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_what_can_I_set_answer
            ),
            Section(
                Res.string.faq_how_to_use_beacon_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_how_to_use_beacon_answer
            ),
            Section(
                Res.string.faq_why_does_beacon_disappear_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_why_does_beacon_disappear_answer
            ),
            Section(
                Res.string.faq_beacon_on_address_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_beacon_on_address_answer
            ),
            Section(
                Res.string.faq_beacon_on_home_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_beacon_on_home_answer
            ),
            Section(
                Res.string.faq_how_close_to_destination_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_how_close_to_destination_answer
            ),
            Section(
                Res.string.faq_turn_beacon_back_on_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_turn_beacon_back_on_answer
            ),
            Section(
                Res.string.faq_road_names_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_road_names_answer
            ),
            Section(
                Res.string.faq_why_not_every_business_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_why_not_every_business_answer
            ),
            Section(
                Res.string.faq_callouts_stopping_in_vehicle_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_callouts_stopping_in_vehicle_answer
            ),
            Section(
                Res.string.faq_miss_a_callout_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_miss_a_callout_answer
            ),

            Section(Res.string.faq_section_how_soundscape_works, SectionType.Title),
            Section(
                Res.string.faq_supported_phones_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_supported_phones_answer
            ),
            Section(
                Res.string.faq_supported_headsets_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_supported_headsets_answer
            ),
            Section(
                Res.string.faq_battery_impact_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_battery_impact_answer
            ),
            Section(
                Res.string.faq_sleep_mode_battery_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_sleep_mode_battery_answer
            ),
            Section(
                Res.string.faq_snooze_mode_battery_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_snooze_mode_battery_answer
            ),
            Section(
                Res.string.faq_headset_battery_impact_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_headset_battery_impact_answer
            ),
            Section(
                Res.string.faq_background_battery_impact_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_background_battery_impact_answer
            ),
            Section(
                Res.string.faq_mobile_data_use_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_mobile_data_use_answer
            ),
            Section(
                Res.string.faq_difference_from_map_apps_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_difference_from_map_apps_answer
            ),
            Section(
                Res.string.faq_use_with_wayfinding_apps_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_use_with_wayfinding_apps_answer
            ),
            Section(
                Res.string.faq_controlling_what_you_hear_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_controlling_what_you_hear_answer
            ),
            Section(
                Res.string.faq_holding_phone_flat_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_holding_phone_flat_answer
            ),
            Section(
                Res.string.faq_what_is_osm_question,
                SectionType.Faq,
                faqAnswer = Res.string.faq_what_is_osm_answer
            ),
        )
    ),
    Sections(
        Res.string.settings_about_app,
        listOf(
            Section(Res.string.about_soundscape, SectionType.Paragraph),
            Section(Res.string.copyright_notices, SectionType.Paragraph),
            Section(Res.string.osm_copyright, SectionType.Paragraph),
            Section(Res.string.openmaptiles_copyright, SectionType.Paragraph),
            Section(Res.string.steam_copyright, SectionType.Paragraph),
            Section(Res.string.oboe_copyright, SectionType.Paragraph),
            Section(Res.string.trademark_disclaimer, SectionType.Paragraph),
        )
    )
)

@Composable
fun SharedHelpScreen(
    topic: String,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenSourceLicenses: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var sections = Sections(Res.string.menu_help, emptyList())
    if (topic.startsWith("page")) {
        val key = topic.substring(4)
        for (page in helpPages) {
            if (page.titleId.key == key) {
                sections = page
            }
        }
    } else if (topic.startsWith("faq")) {
        val keys = topic.substring(3).split(".")
        val questionRes = findStringResourceByKey(keys[0])
        val answerRes = if (keys.size > 1) findStringResourceByKey(keys[1]) else null
        if (questionRes != null && answerRes != null) {
            sections = Sections(
                Res.string.faq_title_abbreviated,
                listOf(
                    Section(questionRes, SectionType.Title),
                    Section(answerRes, SectionType.Paragraph)
                )
            )
        }
    } else {
        for (page in helpPages) {
            if (page.titleId == Res.string.menu_help) {
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
                onNavigateUp = onNavigateUp,
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier.padding(padding)
            ) {
                LazyColumn(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .mediumPadding(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    items(sections.sections) { section ->
                        Box(
                            modifier = Modifier.semantics(mergeDescendants = true) {}
                        ) {
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
                                    val htmlText = markdownToHtml(stringResource(section.textId))
                                    Text(
                                        text = parseHtmlToAnnotatedString(htmlText),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .semantics {
                                                if (section.skipTalkback)
                                                    hideFromAccessibility()
                                            }
                                    )
                                }

                                SectionType.Link, SectionType.Faq -> {
                                    Button(
                                        onClick = {
                                            if (section.type == SectionType.Faq) {
                                                onNavigate("${SharedRoutes.HELP}/faq${section.textId.key}.${section.faqAnswer?.key}")
                                            } else {
                                                onNavigate("${SharedRoutes.HELP}/page${section.textId.key}")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("helpScreenTopic${section.textId.key}"),
                                        shape = RoundedCornerShape(spacing.extraSmall),
                                        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
                                    ) {
                                        Box(Modifier.weight(6f)) {
                                            Text(
                                                text = stringResource(section.textId),
                                                textAlign = TextAlign.Start,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                        Box(Modifier.weight(1f)) {
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
                    item {
                        if (sections.titleId == Res.string.settings_about_app && onOpenSourceLicenses != null) {
                            CustomButton(
                                onClick = onOpenSourceLicenses,
                                text = stringResource(Res.string.menu_open_source_licenses),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .smallPadding()
                                    .testTag("helpScreenOpenSourceLicenses"),
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
