// swift-tools-version:5.9
import PackageDescription

// Pure Swift (Foundation only). No UIKit / CoreBluetooth / Firebase — so everything
// here can be unit-tested without a simulator and shared by every app target.
let package = Package(
    name: "BioCueCore",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [
        .library(name: "BioCueCore", targets: ["BioCueCore"])
    ],
    targets: [
        .target(name: "BioCueCore"),
        .testTarget(name: "BioCueCoreTests", dependencies: ["BioCueCore"])
    ]
)
