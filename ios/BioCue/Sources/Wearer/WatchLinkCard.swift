import SwiftUI
import BioCueCore

// WEARER-ONLY. Shows the state of the Bluetooth link to the watch and walks through pairing.
struct WatchLinkCard: View {
    let state: WatchLinkState
    let onConnect: () -> Void
    let onForget: () -> Void

    var body: some View {
        SoftCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 10) {
                    EmojiAvatar(emoji: "⌚", size: 44, fill: palette.soft, border: palette.chip)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(AppFont.titleMedium)
                            .foregroundStyle(Palette.inkPrimary)
                        Text(detail)
                            .font(AppFont.bodySmall)
                            .foregroundStyle(Palette.inkSecondary)
                    }
                }
                if showSteps { steps }
                actions
            }
        }
        .accessibilityElement(children: .contain)
    }

    private var isConnected: Bool { if case .connected = state { return true }; return false }

    private var palette: StatePalette {
        switch state {
        case .connected: return .calm
        case .connecting, .handshaking, .scanning: return .rising
        case .refused, .bluetoothOff, .unauthorized, .unsupported: return .alert
        default: return .neutral
        }
    }

    private var title: String {
        switch state {
        case .connected(let name, _): return name
        default: return "Your watch"
        }
    }

    private var detail: String {
        switch state {
        case .stopped, .idle: return "Not connected yet"
        case .scanning: return "Looking for your watch…"
        case .connecting: return "Reconnecting…"
        case .handshaking: return "Confirm the code on both screens"
        case .connected(_, let battery): return battery.map { "Connected · battery \($0)%" } ?? "Connected"
        case .bluetoothOff: return "Bluetooth is off. Turn it on in Settings."
        case .unauthorized: return "BioCue needs Bluetooth access. Allow it in Settings."
        case .unsupported: return "This device does not support Bluetooth LE."
        case .refused(let message): return message
        }
    }

    private var showSteps: Bool {
        switch state {
        case .idle, .stopped, .scanning, .handshaking, .refused: return true
        default: return false
        }
    }

    private var steps: some View {
        VStack(alignment: .leading, spacing: 8) {
            StepRow(step: 1, text: "On the watch, open BioCue and tap “I use an iPhone”, then “Pair iPhone”.")
            StepRow(step: 2, text: "Here, tap “Connect watch”.")
            StepRow(step: 3, text: "Your iPhone and the watch both show a 6-digit number. If they match, accept on both.")
        }
        .padding(14)
        .background(Palette.surface2, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    }

    @ViewBuilder private var actions: some View {
        switch state {
        case .connected:
            OutlinedPillButton(title: "Forget this watch", action: onForget)
        case .bluetoothOff, .unauthorized, .unsupported:
            if state == .unauthorized || state == .bluetoothOff {
                OutlinedPillButton(title: "Open Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
            }
        case .scanning, .connecting, .handshaking:
            EmptyView()
        default:
            PillButton(title: "Connect watch", action: onConnect)
        }
    }
}
