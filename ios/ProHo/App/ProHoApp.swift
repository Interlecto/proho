import SwiftUI
import ProHoKit

@main
struct ProHoApp: App {
    @State private var session = SessionModel(api: .configured)

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(session)
                .tint(Theme.accent)
        }
    }
}

extension APIClient {
    /// Base URL from Info.plist `ProHoAPIBaseURL`, which comes from the
    /// `PROHO_API_BASE` build setting in Config/*.xcconfig.
    static var configured: APIClient {
        let raw = Bundle.main.object(forInfoDictionaryKey: "ProHoAPIBaseURL") as? String ?? ""
        guard let url = URL(string: raw), url.scheme != nil else {
            fatalError("ProHoAPIBaseURL missing or invalid in Info.plist: '\(raw)'")
        }
        return APIClient(baseURL: url)
    }
}
