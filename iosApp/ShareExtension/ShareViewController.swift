import UIKit
import MapKit
import MobileCoreServices
import UniformTypeIdentifiers

/// Pure-Swift Share Extension. Reads an MKMapItem (Apple Maps share) or a URL
/// from the host share sheet, builds an equivalent soundscape:// URL and asks
/// the system to open it — the host app's onOpenURL handler does the rest.
///
/// Intentionally does not link the Shared.framework to keep extension memory
/// well below the 50MB peak budget.
final class ShareViewController: UIViewController {

    private static let mapItemType = "com.apple.mapkit.map-item"

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        processInputItems()
    }

    private func processInputItems() {
        guard let items = extensionContext?.inputItems as? [NSExtensionItem] else {
            finish()
            return
        }
        let rawProviders = items.flatMap { $0.attachments ?? [] }
        // Try the most data-rich providers first: a public.url from Apple Maps carries
        // coordinate+name; a map-item is next best; plain-text is just the place name.
        let providers = rawProviders.sorted { a, b in
            providerPriority(a) < providerPriority(b)
        }
        guard !providers.isEmpty else {
            finish()
            return
        }
        tryNext(providers: providers, index: 0)
    }

    private func providerPriority(_ provider: NSItemProvider) -> Int {
        if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) { return 0 }
        if provider.hasItemConformingToTypeIdentifier(Self.mapItemType) { return 1 }
        if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) { return 2 }
        return 3
    }

    private func tryNext(providers: [NSItemProvider], index: Int) {
        if index >= providers.count {
            finish()
            return
        }
        let provider = providers[index]
        if provider.hasItemConformingToTypeIdentifier(Self.mapItemType) {
            provider.loadItem(forTypeIdentifier: Self.mapItemType, options: nil) { [weak self] item, _ in
                if let mapItem = item as? MKMapItem,
                   let url = self?.buildSoundscapeURL(from: mapItem) {
                    self?.open(url: url)
                } else {
                    self?.tryNext(providers: providers, index: index + 1)
                }
            }
            return
        }
        if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { [weak self] item, _ in
                if let url = item as? URL,
                   let soundscapeURL = self?.buildSoundscapeURL(from: url) {
                    self?.open(url: soundscapeURL)
                } else {
                    self?.tryNext(providers: providers, index: index + 1)
                }
            }
            return
        }
        if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { [weak self] item, _ in
                if let text = item as? String,
                   let url = URL(string: text),
                   let soundscapeURL = self?.buildSoundscapeURL(from: url) {
                    self?.open(url: soundscapeURL)
                } else {
                    self?.tryNext(providers: providers, index: index + 1)
                }
            }
            return
        }
        tryNext(providers: providers, index: index + 1)
    }

    // MARK: - URL construction

    private func buildSoundscapeURL(from mapItem: MKMapItem) -> URL? {
        let coord = mapItem.placemark.coordinate
        guard CLLocationCoordinate2DIsValid(coord) else { return nil }
        return makeSoundscapeURL(latitude: coord.latitude, longitude: coord.longitude, name: mapItem.name)
    }

    private func buildSoundscapeURL(from url: URL) -> URL? {
        guard let host = url.host?.lowercased(), host.hasSuffix("maps.apple.com") else {
            return nil
        }
        let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let items = comps?.queryItems ?? []
        // Apple Maps URLs use a few different keys depending on the path/version:
        //   - older /maps?q=name&ll=lat,lon
        //   - newer /place?coordinate=lat,lon&name=name (seen on iOS 17+)
        //   - search results: sll=lat,lon
        let name = items.first(where: { $0.name == "name" })?.value
            ?? items.first(where: { $0.name == "q" })?.value
        let coord = items.first(where: { $0.name == "ll" })?.value
            ?? items.first(where: { $0.name == "coordinate" })?.value
            ?? items.first(where: { $0.name == "sll" })?.value
        guard let latLonStr = coord else { return nil }
        let parts = latLonStr.split(separator: ",")
        guard parts.count == 2,
              let lat = Double(parts[0].trimmingCharacters(in: .whitespaces)),
              let lon = Double(parts[1].trimmingCharacters(in: .whitespaces)) else {
            return nil
        }
        return makeSoundscapeURL(latitude: lat, longitude: lon, name: name)
    }

    private func makeSoundscapeURL(latitude: Double, longitude: Double, name: String?) -> URL? {
        // Use a `location` host with explicit lat/lon query params rather than putting
        // the comma in the host portion — iOS can reject URLs that have invalid host
        // characters (commas violate RFC 3986) before they ever reach the host app.
        var comps = URLComponents()
        comps.scheme = "soundscape"
        comps.host = "location"
        var query: [URLQueryItem] = [
            URLQueryItem(name: "lat", value: String(latitude)),
            URLQueryItem(name: "lon", value: String(longitude)),
        ]
        if let name = name, !name.isEmpty {
            query.append(URLQueryItem(name: "name", value: name))
        }
        comps.queryItems = query
        return comps.url
    }

    // MARK: - Open + dismiss

    private func open(url: URL) {
        guard let context = extensionContext else {
            finish()
            return
        }
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            context.open(url) { success in
                if success {
                    DispatchQueue.main.async {
                        context.completeRequest(returningItems: nil, completionHandler: nil)
                    }
                } else {
                    // Share-services on iOS routinely returns false from extensionContext.open
                    // even for the host app's own URL scheme. Fall back to walking the
                    // responder chain for a UIApplication, which honours the modern open API.
                    DispatchQueue.main.async {
                        self.openViaResponderChain(url: url, context: context)
                    }
                }
            }
        }
    }

    private func openViaResponderChain(url: URL, context: NSExtensionContext) {
        var responder: UIResponder? = self
        while let current = responder {
            if let app = current as? UIApplication {
                app.open(url, options: [:]) { _ in
                    DispatchQueue.main.async {
                        context.completeRequest(returningItems: nil, completionHandler: nil)
                    }
                }
                return
            }
            responder = current.next
        }
        context.completeRequest(returningItems: nil, completionHandler: nil)
    }

    private func finish() {
        DispatchQueue.main.async { [weak self] in
            self?.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
    }
}
