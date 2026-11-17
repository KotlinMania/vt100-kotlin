import Testing
import Vt100

// Smoke test: the mere fact that this file compiles proves the Kotlin
// Swift Export bridge produced a usable `Vt100.swiftmodule`; the fact
// that the test executable links proves the static archive at
// `../build/swift-test/libVt100.a` is reachable; the fact that the
// test runs proves the full embedSwiftExportForXcode → SPM → swift
// test loop is green for this repo. SWIFT_EXPORT_ROLLOUT.md item 4.
@Suite
struct Vt100ExportTests {
    @Test
    func parserProcessesStyledTextAndColors() {
        let parser = Parser.Companion.shared.new(rows: 24, cols: 80, scrollbackLen: 0)
        parser.process(string: "\u{001B}[1;31mhi\u{001B}[m")

        let screen = parser.screen()
        #expect(screen.contents() == "hi")

        let firstCell = screen.cell(row: 0, col: 0)
        #expect(firstCell != nil)
        #expect(firstCell?.contents() == "h")
        #expect(firstCell?.hasContents() == true)
        #expect(firstCell?.bold() == true)
        #expect(firstCell?.italic() == false)
        #expect(firstCell?.fgcolor() != nil)
        #expect(firstCell?.bgcolor() != nil)
        switch firstCell?.fgcolor().sealedType() {
        case let .idx(idx):
            #expect(idx.value.value == 1)
        default:
            Issue.record("Expected Color.Idx(1) for red foreground")
        }
        switch firstCell?.bgcolor().sealedType() {
        case .default:
            break
        default:
            Issue.record("Expected Color.Default for default background")
        }

        let rgb = Vt100._ExportedKotlinPackages_io_github_kotlinmania_vt100_Color_Rgb(red: 12, green: 34, blue: 56)
        #expect(rgb.red == 12)
        #expect(rgb.green == 34)
        #expect(rgb.blue == 56)
        if case let .rgb(wrapped) = rgb.sealedType() {
            #expect(wrapped.value.green == 34)
        } else {
            Issue.record("Expected Color.Rgb sealed case")
        }
    }

    @Test
    func screenDefaultsAndMouseProtocolsWork() {
        let screen = Screen(rows: 4, cols: 10, scrollbackLen: 0)
        #expect(screen.mouseProtocolMode() == .None)
        #expect(screen.mouseProtocolEncoding() == .Default)
        #expect(screen.alternateScreen() == false)
        #expect(screen.scrollback() == 0)
        #expect(screen.scrollbackLen() == 0)

        screen.setSize(rows: 6, cols: 12)
        #expect(MouseProtocolMode.allCases.count == 5)
        #expect(MouseProtocolEncoding.allCases.count == 3)
        #expect(MouseProtocolMode(rawValue: 2) == .PressRelease)
        #expect(MouseProtocolEncoding(rawValue: 2) == .Sgr)
    }
}
