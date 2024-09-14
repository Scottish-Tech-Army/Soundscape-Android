package org.scottishtecharmy.soundscape.screens.markers_routes.validators

fun isFieldValid(field: String): Boolean {
    return field.isNotBlank()
}

fun validateFields(name: String, description: String): Pair<Boolean, Pair<Boolean, Boolean>> {
    val isNameValid = isFieldValid(name)
    val isDescriptionValid = isFieldValid(description)
    return Pair(isNameValid && isDescriptionValid, Pair(isNameValid, isDescriptionValid))
}
