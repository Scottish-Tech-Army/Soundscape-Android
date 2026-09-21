package org.scottishtecharmy.soundscape.utils

/** A single GPX entry inside an export/import bundle. */
data class NamedGpx(val filename: String, val content: String)

/**
 * Platform bridge for advanced-markers-and-routes export/import.
 *
 * The shared ViewModel produces/consumes the GPX content; bundling the entries
 * into a zip and presenting the platform share/file-picker UI is the platform's
 * responsibility. Android uses java.util.zip + FileProvider/Intent, iOS uses
 * libcompression with NSFileCoordinator + UIActivityViewController/UIDocumentPicker.
 */
interface MarkersAndRoutesIo {
    /**
     * Bundle [files] into a zip archive and present a platform export/share UI
     * so the user can save or send it. [suggestedFilename] is the basename for
     * the produced archive (without extension). [shareTitle] is shown in the
     * Android chooser; iOS ignores it.
     */
    suspend fun exportGpxZip(
        files: List<NamedGpx>,
        suggestedFilename: String,
        shareTitle: String,
    )

    /**
     * Trigger a platform file picker for a `.zip` file containing GPX entries.
     * Returns the parsed list of entries, or `null` if the user cancelled.
     */
    suspend fun pickGpxZip(): List<NamedGpx>?
}
