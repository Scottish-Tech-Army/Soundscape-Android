import CoreLocation
import Foundation
import Shared

/// Routes inbound URLs (custom scheme, universal link, file URL) into the
/// shared IntentParser, then publishes the result onto IosSoundscapeService's
/// pendingIntent flow where SharedNavHost picks it up.
final class IntentBridge {
    static let shared = IntentBridge()

    private let geocoder = CLGeocoder()
    private let geocodeTimeout: TimeInterval = 3.0

    private init() {}

    func handle(url: URL) {
        let service = IosSoundscapeService.companion.getInstance()

        if url.isFileURL {
            handleFileImport(url: url, service: service)
            return
        }

        guard let parsed = IntentParser.shared.parseUrl(url: url.absoluteString) else { return }

        if let latLon = parsed as? IncomingIntent.OpenLatLon {
            if let name = latLon.displayName, !name.isEmpty {
                service.publishPendingIntent(intent: latLon)
            } else {
                upgradeWithGeocoder(intent: latLon, service: service)
            }
        } else {
            service.publishPendingIntent(intent: parsed)
        }
    }

    // MARK: - File imports

    private func handleFileImport(url: URL, service: IosSoundscapeService) {
        let needsScopedAccess = url.startAccessingSecurityScopedResource()
        defer { if needsScopedAccess { url.stopAccessingSecurityScopedResource() } }

        guard let data = try? Data(contentsOf: url, options: .mappedIfSafe) else { return }
        guard data.count <= 1_000_000 else { return }
        guard let text = String(data: data, encoding: .utf8) else { return }

        let parsed: IncomingIntent.ImportRoute? = url.pathExtension.lowercased() == "gpx"
            ? IntentParser.shared.parseGpx(text: text)
            : IntentParser.shared.parseRouteJson(text: text)

        if let intent = parsed {
            service.publishPendingIntent(intent: intent)
        }
    }

    // MARK: - Geocoder upgrade

    private func upgradeWithGeocoder(intent: IncomingIntent.OpenLatLon, service: IosSoundscapeService) {
        let location = CLLocation(latitude: intent.latitude, longitude: intent.longitude)
        var didPublish = false
        let timeoutWork = DispatchWorkItem { [weak self] in
            guard let self = self else { return }
            if !didPublish {
                didPublish = true
                self.geocoder.cancelGeocode()
                service.publishPendingIntent(intent: intent)
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + geocodeTimeout, execute: timeoutWork)

        geocoder.reverseGeocodeLocation(location) { placemarks, _ in
            DispatchQueue.main.async {
                if didPublish { return }
                didPublish = true
                timeoutWork.cancel()
                let displayName = placemarks?.first.flatMap { Self.displayName(for: $0) }
                if let name = displayName {
                    let upgraded = IncomingIntent.OpenLatLon(
                        latitude: intent.latitude,
                        longitude: intent.longitude,
                        displayName: name
                    )
                    service.publishPendingIntent(intent: upgraded)
                } else {
                    service.publishPendingIntent(intent: intent)
                }
            }
        }
    }

    private static func displayName(for placemark: CLPlacemark) -> String? {
        if let name = placemark.name, !name.isEmpty { return name }
        if let thoroughfare = placemark.thoroughfare {
            if let subThoroughfare = placemark.subThoroughfare {
                return "\(subThoroughfare) \(thoroughfare)"
            }
            return thoroughfare
        }
        return placemark.locality ?? placemark.country
    }
}
