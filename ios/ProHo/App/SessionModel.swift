import Foundation
import Observation
import ProHoKit

/// App-wide auth state. Demo-only, like the Android client: the token
/// lives in memory and is lost when the app is killed. Persisting it in
/// the Keychain is step 2 of the plan in ios/README.md.
@MainActor
@Observable
final class SessionModel {
    let api: APIClient
    private(set) var token: String?
    private(set) var account: Account?

    init(api: APIClient) {
        self.api = api
    }

    var isLoggedIn: Bool { token != nil && account != nil }

    func didLogIn(_ result: LoginResult) {
        token = result.token
        account = result.account
    }

    func logOut() {
        let oldToken = token
        token = nil
        account = nil
        if let oldToken {
            // Best effort: the session is gone locally whether or not the
            // server hears about it.
            Task { [api] in try? await api.logout(token: oldToken) }
        }
    }
}
