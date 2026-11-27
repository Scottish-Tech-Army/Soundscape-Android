package org.scottishtecharmy.soundscape

import kotlin.math.roundToInt
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.locationprovider.IosHeadTrackingProvider
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.logging.SupportLogBuffer
import org.scottishtecharmy.soundscape.platform.appVersionName
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.currentRoute
import platform.AVFAudio.outputVolume
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreMotion.CMAuthorizationStatus
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMAuthorizationStatusDenied
import platform.CoreMotion.CMAuthorizationStatusNotDetermined
import platform.CoreMotion.CMAuthorizationStatusRestricted
import platform.CoreMotion.CMHeadphoneMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.dataUsingEncoding
import platform.Foundation.languageCode
import platform.Foundation.lowPowerModeEnabled
import platform.MessageUI.MFMailComposeResult
import platform.MessageUI.MFMailComposeViewController
import platform.MessageUI.MFMailComposeViewControllerDelegateProtocol
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundRefreshStatus
import platform.UIKit.UIDevice
import platform.darwin.NSObject

private val logger = AppLog.tag("IosContactSupport")

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun presentContactSupport(service: IosSoundscapeService, includeRecording: Boolean = false) {
    val subject = buildSupportSubject()
    val report = buildSupportReport(service, permissionsDescription())
    // The device details go only in the attached log, not the email body, where they confused
    // users. Logged a row at a time before the log buffer is dumped so that they're in it.
    report.lines().filter { it.isNotBlank() }.forEach { line ->
        logger.i { line }
    }

    if (MFMailComposeViewController.canSendMail()) {
        val composer = MFMailComposeViewController()
        composer.setSubject(subject)
        composer.setToRecipients(listOf(SUPPORT_EMAIL))
        // Attaching the support log buffer is only possible via this in-app composer -
        // the mailto: fallback below has no attachment mechanism.
        val logData = NSString.create(string = SupportLogBuffer.dumpAsText())
            .dataUsingEncoding(NSUTF8StringEncoding)
        if (logData != null) {
            composer.addAttachmentData(logData, "text/plain", "soundscape_log.txt")
        }
        // Only when the user agreed to it in ContactSupportRecordingDialog: it holds their
        // recent locations
        if (includeRecording) {
            val gpxData = NSString.create(string = service.recordingGpx())
                .dataUsingEncoding(NSUTF8StringEncoding)
            if (gpxData != null) {
                composer.addAttachmentData(gpxData, "application/gpx+xml", "travel.gpx")
            }
        }
        val delegate = MailComposeDelegate()
        composer.mailComposeDelegate = delegate
        retainedMailDelegates.add(delegate)
        presentTopViewController(composer)
    } else {
        // mailto: can't carry an attachment, so this is the one case where the details
        // still have to go in the body - and where a recording the user agreed to send is lost.
        if (includeRecording) logger.w { "No mail account set up: travel recording not sent" }
        val urlString = buildString {
            append("mailto:")
            append(SUPPORT_EMAIL)
            append("?subject=")
            append(supportUrlEncode(subject))
            append("&body=")
            append(supportUrlEncode(report))
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

/**
 * The system output volume for the current route (the hardware volume buttons), 0-1. iOS has no
 * separate media/ringer split for app audio, so this is the volume Soundscape plays at.
 */
private fun mediaVolumeDescription(): String =
    "${(AVAudioSession.sharedInstance().outputVolume * 100).roundToInt()}%"

@OptIn(ExperimentalForeignApi::class)
private fun buildSupportReport(
    service: IosSoundscapeService,
    permissions: List<Pair<String, String>>,
): String = buildString {
    append("Contact Support report\n")
    append(reportLine("Summary", buildSupportSubject()))
    append(reportLine("Product", UIDevice.currentDevice.model))
    append(reportLine("Manufacturer", "Apple"))
    append(
        reportLine(
            "System",
            "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
        ),
    )

    append(reportLine("VoiceOver", if (UIAccessibilityIsVoiceOverRunning()) "On" else "Off"))

    val session = AVAudioSession.sharedInstance()
    for (output in session.currentRoute.outputs) {
        val port = output as? AVAudioSessionPortDescription ?: continue
        append(reportLine("Audio Output", "${port.portName} (${port.portType})"))
    }
    for (input in session.currentRoute.inputs) {
        val port = input as? AVAudioSessionPortDescription ?: continue
        append(reportLine("Audio Input", "${port.portName} (${port.portType})"))
    }

    append(reportLine("Media volume", mediaVolumeDescription()))

    for ((key, value) in permissions) append(reportLine(key, value))

    val downloaded = service.offlineMapManager.downloadedExtractsFc.value
    for (feature in downloaded.features) {
        val name = feature.properties?.get("name")
        val filename = feature.properties?.get("filename")
        append(reportLine("Offline extract", "$name, $filename"))
    }

    @Suppress("UNCHECKED_CAST")
    val all = NSUserDefaults.standardUserDefaults.dictionaryRepresentation()
    for ((key, value) in all) {
        append(reportLine(key.toString(), value.toString()))
    }
    for (key in ResourceMapper.getUnfoundKeys()) {
        append(reportLine("Untranslated OSM key", key.toString()))
    }
}

/**
 * What the user has actually granted, for the support email and log. The important one is
 * location: Soundscape needs Always, and "While Using" stops callouts once the phone locks.
 *
 * CoreLocation can't tell a real Always grant from provisional Always (answering "Allow While
 * Using" to requestAlwaysAuthorization): both read back as Always, but provisional withholds
 * background fixes until the user accepts iOS's later "Change to Always Allow?" prompt. So
 * Always is reported as "Always (or provisional Always)"; Settings shows "While Using" for the
 * provisional case.
 */
@OptIn(ExperimentalForeignApi::class)
private fun permissionsDescription(): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val locationManager = CLLocationManager()
    result += "Permission: Location" to locationStatusName(locationManager.authorizationStatus)
    result += "Location accuracy" to when (locationManager.accuracyAuthorization) {
        CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy -> "Precise"
        CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy -> "Approximate (Precise Location off)"
    }
    result += "Permission: Motion & Fitness" to
        motionStatusName(CMPedometer.authorizationStatus())
    if (IosHeadTrackingProvider.isHeadphoneMotionSupported()) {
        result += "Permission: Headphone motion" to
            motionStatusName(CMHeadphoneMotionManager.authorizationStatus())
    }
    result += "Background App Refresh" to
        when (UIApplication.sharedApplication.backgroundRefreshStatus) {
            UIBackgroundRefreshStatus.UIBackgroundRefreshStatusAvailable -> "On"
            UIBackgroundRefreshStatus.UIBackgroundRefreshStatusDenied -> "Off"
            UIBackgroundRefreshStatus.UIBackgroundRefreshStatusRestricted -> "Restricted"
        }
    result += "Low Power Mode" to
        if (NSProcessInfo.processInfo.lowPowerModeEnabled) "On" else "Off"
    return result
}

private fun locationStatusName(status: CLAuthorizationStatus): String = when (status) {
    kCLAuthorizationStatusAuthorizedAlways -> "Always (or provisional Always)"
    kCLAuthorizationStatusAuthorizedWhenInUse -> "While Using the App"
    kCLAuthorizationStatusDenied -> "Denied (or Location Services off)"
    kCLAuthorizationStatusRestricted -> "Restricted"
    kCLAuthorizationStatusNotDetermined -> "Not asked yet"
    else -> "Unknown ($status)"
}

private fun motionStatusName(status: CMAuthorizationStatus): String = when (status) {
    CMAuthorizationStatusAuthorized -> "Granted"
    CMAuthorizationStatusDenied -> "Denied"
    CMAuthorizationStatusRestricted -> "Restricted"
    CMAuthorizationStatusNotDetermined -> "Not asked yet"
    else -> "Unknown ($status)"
}

private fun reportLine(key: String, value: String): String = "$key: $value\n"

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
