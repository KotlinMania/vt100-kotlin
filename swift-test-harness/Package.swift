// swift-tools-version: 6.0
import PackageDescription

// Smoke test for the Vt100 Kotlin Swift Export bridge. The Kotlin Gradle
// plugin emits a Swift Package at `../build/SPMPackage/macosArm64/Debug`
// but does NOT add `libVt100.a` as a binary target there. We pull the
// static archive in via `unsafeFlags` so `swift test` (run outside an
// `xcodebuild`-driven invocation) can resolve the `__root____*` symbols
// and KotlinError types from the archive that `embedSwiftExportForXcode`
// drops next to the SPM source tree. SWIFT_EXPORT_ROLLOUT.md gap #1.

let package = Package(
    name: "SwiftTestHarness",
    platforms: [
        .macOS(.v14),
    ],
    products: [],
    dependencies: [
        .package(name: "Vt100", path: "../build/SPMPackage/macosArm64/Debug"),
    ],
    targets: [
        .testTarget(
            name: "SwiftTestHarnessTests",
            dependencies: [
                .product(name: "Vt100Library", package: "Vt100"),
            ],
            swiftSettings: [
                .unsafeFlags([
                    "-F", "/Library/Developer/CommandLineTools/Library/Developer/Frameworks",
                ]),
            ],
            linkerSettings: [
                .unsafeFlags([
                    "-L", "../build/swift-test",
                    "-lVt100",
                    "-F", "/Library/Developer/CommandLineTools/Library/Developer/Frameworks",
                    "-Xlinker", "-rpath", "-Xlinker", "/Library/Developer/CommandLineTools/Library/Developer/Frameworks",
                    "-Xlinker", "-rpath", "-Xlinker", "/Library/Developer/CommandLineTools/Library/Developer/usr/lib",
                ]),
            ]
        ),
    ]
)
