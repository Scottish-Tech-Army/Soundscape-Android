package org.scottishtecharmy.soundscape

import kotlinx.cinterop.ExperimentalForeignApi
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.platform.appVersionName
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.currentRoute
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.MessageUI.MFMailComposeResult
import platform.MessageUI.MFMailComposeViewController
import platform.MessageUI.MFMailComposeViewControllerDelegateProtocol
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIDevice
import platform.darwin.NSObject

private const val SUPPORT_RECIPIENT = "soundscape@scottishtecharmy.support"

@OptIn(ExperimentalForeignApi::class)
internal fun presentContactSupport(service: IosSoundscapeService) {
    val subject = buildSupportSubject()
    val htmlBody = buildSupportHtmlBody(service)

    if (MFMailComposeViewController.canSendMail()) {
        val composer = MFMailComposeViewController()
        composer.setSubject(subject)
        composer.setToRecipients(listOf(SUPPORT_RECIPIENT))
        composer.setMessageBody(htmlBody, isHTML = true)
        val delegate = MailComposeDelegate()
        composer.mailComposeDelegate = delegate
        retainedMailDelegates.add(delegate)
        presentTopViewController(composer)
    } else {
        val urlString = buildString {
            append("mailto:")
            append(SUPPORT_RECIPIENT)
            append("?subject=")
            append(supportUrlEncode(subject))
            append("&body=")
            append(supportUrlEncode(htmlToPlainText(htmlBody)))
        }
        val url = NSURL.URLWithString(urlString)
        if (url != null) openExternalUrl(url)
    }
}

private val retainedMailDelegates = mutableListOf<MailComposeDelegate>()

@OptIn(ExperimentalForeignApi::class)
private class MailComposeDelegate :
    NSObject(),
    MFMailComposeViewControllerDelegateProtocol {

    override fun mailComposeController(
        controller: MFMailComposeViewController,
        didFinishWithResult: MFMailComposeResult,
        error: NSError?,
    ) {
        controller.dismissViewControllerAnimated(true) {
            retainedMailDelegates.remove(this@MailComposeDelegate)
        }
    }
}

private fun buildSupportSubject(): String {
    val osVersion = UIDevice.currentDevice.systemVersion
    val deviceModel = UIDevice.currentDevice.model
    val locale = NSLocale.currentLocale
    val country = locale.countryCode
    val language = if (country != null) "${locale.languageCode}-$country" else locale.languageCode
    return "Soundscape Feedback (iOS $osVersion, Apple $deviceModel, $language, ${appVersionName()})"
}

@OptIn(ExperimentalForeignApi::class)
private fun buildSupportHtmlBody(service: IosSoundscapeService): String = buildString {
    append("-----------------------------<br/>")
    append(tableRow("Summary", buildSupportSubject()))
    append(tableRow("Product", UIDevice.currentDevice.model))
    append(tableRow("Manufacturer", "Apple"))
    append(
        tableRow(
            "System",
            "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
        ),
    )

    append(tableRow("VoiceOver", if (UIAccessibilityIsVoiceOverRunning()) "On" else "Off"))

    val session = AVAudioSession.sharedInstance()
    for (output in session.currentRoute.outputs) {
        val port = output as? AVAudioSessionPortDescription ?: continue
        append(tableRow("Audio Output", "${port.portName} (${port.portType})"))
    }
    for (input in session.currentRoute.inputs) {
        val port = input as? AVAudioSessionPortDescription ?: continue
        append(tableRow("Audio Input", "${port.portName} (${port.portType})"))
    }

    val downloaded = service.offlineMapManager.downloadedExtractsFc.value
    for (feature in downloaded.features) {
        val name = feature.properties?.get("name")
        val filename = feature.properties?.get("filename")
        append(tableRow("Offline extract", "$name, $filename"))
    }

    @Suppress("UNCHECKED_CAST")
    val all = NSUserDefaults.standardUserDefaults.dictionaryRepresentation() as? Map<Any?, Any?>
    if (all != null) {
        for ((key, value) in all) {
            append(tableRow(key.toString(), value.toString()))
        }
    }
    append("-----------------------------<br/><br/>")

    append("Untranslated OSM keys:<br/>")
    for (key in ResourceMapper.getUnfoundKeys()) {
        append("\t$key<br/>")
    }
    append("-----------------------------<br/><br/>")
}

private fun tableRow(key: String, value: String): String = "$key:\t\t$value<br/>"

private fun htmlToPlainText(html: String): String =
    html.replace("<br/>", "\n").replace("<br>", "\n")

private fun supportUrlEncode(value: String): String {
    val bytes = value.encodeToByteArray()
    val builder = StringBuilder(bytes.size)
    for (b in bytes) {
        val c = b.toInt() and 0xFF
        val isUnreserved = (c in 0x30..0x39) ||
            (c in 0x41..0x5A) ||
            (c in 0x61..0x7A) ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            builder.append(c.toChar())
        } else {
            builder.append('%')
            builder.append(c.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return builder.toString()
}
