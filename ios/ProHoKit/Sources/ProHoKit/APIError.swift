import Foundation

/// An API call failure. `server` carries the backend's machine-readable
/// `{"error": "<code>"}` body; the other cases never reached a usable
/// response.
public enum APIError: Error, Equatable, Sendable {
    case server(status: Int, code: String)
    case transport(String)
    case decoding(String)

    /// The backend error code, or a client-side stand-in.
    public var code: String {
        switch self {
        case let .server(_, code): return code
        case .transport: return "network_error"
        case .decoding: return "unknown_error"
        }
    }

    /// Key into the app's Localizable.strings. Kept here (not in the app)
    /// so the mapping is unit-testable without UIKit, and so it stays in
    /// step with the Android client's `when (e.code)` blocks.
    public var messageKey: String {
        switch code {
        case "invalid_credentials": return "error_invalid_credentials"
        case "email_already_registered": return "error_email_already_registered"
        case "invalid_request", "unsupported_community_kind": return "error_invalid_request"
        case "community_key_already_registered": return "error_community_key_already_registered"
        case "network_error": return "error_network"
        default: return "error_unknown"
        }
    }
}
