package org.scottishtecharmy.soundscape.navigation

object RouteGuidanceAnnouncementFormatter {
    fun format(event: RouteGuidanceEvent): String {
        return when (event) {
            is RouteGuidanceEvent.Instruction -> event.instruction.text
            is RouteGuidanceEvent.OffRoute -> "You are off route."
            is RouteGuidanceEvent.Arrived -> "Arrived."
        }
    }
}
