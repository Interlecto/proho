// swift-tools-version:5.9

// ProHoKit is the platform-independent core of the iOS client: REST
// models, the API client and error mapping. It deliberately avoids UIKit,
// SwiftUI, Combine and the Keychain so it builds and tests with a plain
// Swift toolchain on Windows and Linux as well as on macOS. Anything that
// needs Apple frameworks lives in the app target (ios/ProHo/).
import PackageDescription

let package = Package(
    name: "ProHoKit",
    platforms: [
        .iOS(.v17),
        .macOS(.v14),
    ],
    products: [
        .library(name: "ProHoKit", targets: ["ProHoKit"]),
    ],
    targets: [
        .target(name: "ProHoKit"),
        .testTarget(name: "ProHoKitTests", dependencies: ["ProHoKit"]),
    ]
)
