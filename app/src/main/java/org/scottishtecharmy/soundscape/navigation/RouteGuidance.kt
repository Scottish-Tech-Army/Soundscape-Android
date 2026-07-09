package org.scottishtecharmy.soundscape.navigation

sealed class RouteGuidanceEvent {
    data class Instruction(
        val instructionIndex: Int,
        val instruction: NavigationInstruction,
        val update: RouteProgressUpdate
    ) : RouteGuidanceEvent()

    data class OffRoute(
        val update: RouteProgressUpdate
    ) : RouteGuidanceEvent()

    data class Arrived(
        val update: RouteProgressUpdate
    ) : RouteGuidanceEvent()
}

class RouteGuidance(
    route: NavigationRoute,
    config: RouteFollowerConfig = RouteFollowerConfig()
) {
    private val follower = RouteFollower(route, config)
    private var lastStatus: RouteProgressStatus? = null
    private var lastInstructionIndex: Int? = null

    fun isOffRoute(): Boolean = lastStatus == RouteProgressStatus.OFF_ROUTE

    fun update(location: NavigationPoint): RouteGuidanceEvent? {
        val progress = follower.update(location)
        val previousStatus = lastStatus
        lastStatus = progress.status

        return when (progress.status) {
            RouteProgressStatus.ARRIVED -> {
                if (previousStatus == RouteProgressStatus.ARRIVED) {
                    null
                } else {
                    RouteGuidanceEvent.Arrived(progress)
                }
            }
            RouteProgressStatus.OFF_ROUTE -> {
                if (previousStatus == RouteProgressStatus.OFF_ROUTE) {
                    null
                } else {
                    RouteGuidanceEvent.OffRoute(progress)
                }
            }
            RouteProgressStatus.ON_ROUTE -> {
                val instructionIndex = progress.nextInstructionIndex ?: return null
                if (
                    instructionIndex == lastInstructionIndex &&
                    previousStatus != RouteProgressStatus.OFF_ROUTE
                ) {
                    null
                } else {
                    lastInstructionIndex = instructionIndex
                    RouteGuidanceEvent.Instruction(
                        instructionIndex = instructionIndex,
                        instruction = progress.nextInstruction!!,
                        update = progress
                    )
                }
            }
        }
    }
}
