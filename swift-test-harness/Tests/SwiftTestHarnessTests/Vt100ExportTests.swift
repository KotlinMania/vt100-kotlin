import XCTest
import Vt100

// Smoke test: the mere fact that this file compiles proves the Kotlin
// Swift Export bridge produced a usable `Vt100.swiftmodule`; the fact
// that the test executable links proves the static archive at
// `../build/swift-test/libVt100.a` is reachable; the fact that the
// test runs proves the full embedSwiftExportForXcode → SPM → swift
// test loop is green for this repo. SWIFT_EXPORT_ROLLOUT.md item 4.
final class Vt100ExportTests: XCTestCase {
    func testKotlinSwiftExportModuleImports() {
        XCTAssertTrue(true, "Vt100 Swift module imported successfully")
    }
}
