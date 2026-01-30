package org.scottishtecharmy.soundscape.screens.home.home

class StructureLog(private val log: (String) -> Unit) {
    private var depth = 0

    fun start(label: String) {
        log("${indent()}'${label}' start")
        depth++
    }

    fun end(label: String) {
        depth--
        log("${indent()}'${label}' end")
    }

    fun unstructured(message: String) {
        log("${indent()}${message}")
    }

    private fun indent() = "  ".repeat(depth)
}
