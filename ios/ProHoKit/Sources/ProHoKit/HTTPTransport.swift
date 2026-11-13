import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking // URLSession on Windows and Linux
#endif

/// The one seam between `APIClient` and the network, so tests can stub it.
public protocol HTTPTransport: Sendable {
    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse)
}

/// Default transport. Wraps the completion-handler `dataTask` API rather
/// than `URLSession.data(for:)` because the async variant isn't available
/// in every swift-corelibs-foundation release used on Windows/Linux.
public struct URLSessionTransport: HTTPTransport, @unchecked Sendable {
    private let session: URLSession

    public init(session: URLSession = .shared) {
        self.session = session
    }

    public func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        try await withCheckedThrowingContinuation { continuation in
            let task = session.dataTask(with: request) { data, response, error in
                if let error = error {
                    continuation.resume(throwing: APIError.transport(error.localizedDescription))
                    return
                }
                guard let http = response as? HTTPURLResponse else {
                    continuation.resume(throwing: APIError.transport("no_http_response"))
                    return
                }
                continuation.resume(returning: (data ?? Data(), http))
            }
            task.resume()
        }
    }
}
