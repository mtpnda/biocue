import SwiftUI
import BioCueCore

// CAREGIVER-ONLY screens. No watch, no raw vitals, no event creation.

struct CaregiverHomeView: View {
    @ObservedObject var vm: CaregiverViewModel
    let onProfile: () -> Void

    private var wearerName: String { vm.wearerName ?? "Wearer" }
    private var tier: Int { vm.hasOngoing ? 2 : 0 }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    HStack(spacing: 12) {
                        Text("BioCue")
                            .font(AppFont.titleLarge)
                            .foregroundStyle(Palette.inkPrimary)
                        StatusPill(text: "Caregiver", palette: .neutral, showDot: false)
                    }
                    Spacer()
                    CircleIconButton(systemName: "gearshape", label: "Settings and profile", action: onProfile)
                }
                StatusMeterCard(userName: wearerName, tier: tier, activity: "🪑 Sitting Still")
                if vm.hasOngoing { SosAlertCard(userName: wearerName) }
                ReviewCard(reviews: vm.reviews, userName: wearerName) { id, label in
                    Task { await vm.resolve(eventId: id, newLabelOrContext: label) }
                }
                JournalCard(entries: vm.journal, offset: vm.journalOffset) { target in
                    vm.changeJournalDate(delta: target - vm.journalOffset)
                }
            }
            .padding(.horizontal, 20).padding(.bottom, 32)
        }
        .calmBackground()
    }
}

/// Enter a wearer's pairing code. Also shows pending / failed states.
struct CaregiverPairingView: View {
    @ObservedObject var vm: CaregiverViewModel
    var onDone: (() -> Void)? = nil
    @State private var code = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                EmojiAvatar(emoji: "🔗", size: 80).padding(.top, 24)

                if case .connected(let pairing) = vm.pairingState {
                    Text("Connected to Wearer")
                        .font(AppFont.headlineSmall)
                        .foregroundStyle(Palette.inkPrimary)
                    SoftCard {
                        Text(pairing.wearerName.isEmpty ? "Unknown Wearer" : pairing.wearerName)
                            .font(AppFont.titleLarge)
                            .foregroundStyle(Palette.inkPrimary)
                            .frame(maxWidth: .infinity)
                    }
                    if let onDone = onDone { PillButton(title: "Continue", action: onDone) }
                    CoralOutlinedPillButton(title: "Unlink from Wearer") { Task { await vm.unlink() } }
                } else {
                    Text("Join a circle")
                        .font(AppFont.headlineLarge)
                        .foregroundStyle(Palette.inkPrimary)
                        .multilineTextAlignment(.center)
                    Text("Ask the wearer to open BioCue and show you their 6-digit pairing code.")
                        .font(AppFont.bodyLarge).foregroundStyle(Palette.inkSecondary).multilineTextAlignment(.center)
                    CalmTextField(label: "6-digit code", text: $code, keyboard: .numberPad, contentType: .oneTimeCode)
                        .onChange(of: code) { new in
                            let digits = String(new.filter { $0.isNumber }.prefix(6))
                            if digits != new { code = digits }
                        }
                    PillButton(title: "Connect", disabled: code.count != 6) { Task { await vm.submitCode(code) } }
                    status
                }
            }
            .padding(.horizontal, 28).padding(.bottom, 32)
        }
        .calmBackground()
    }

    @ViewBuilder private var status: some View {
        switch vm.pairingState {
        case .searching: InlineNotice(text: "Looking for that code…", tone: .rising)
        case .pending(let name): InlineNotice(text: "Waiting for \(name.isEmpty ? "the wearer" : name) to approve you.", tone: .rising)
        case .notFound: InlineNotice(text: "That code was not found or has expired. Ask for a new one.")
        case .failed(let message): InlineNotice(text: message)
        default: EmptyView()
        }
    }
}

struct AccessRevokedView: View {
    let onDismiss: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            EmojiAvatar(emoji: "🔒", size: 88, fill: StatePalette.rising.soft, border: StatePalette.rising.chip)
            Text("Access removed")
                .font(AppFont.headlineSmall)
                .foregroundStyle(Palette.inkPrimary)
            Text("The wearer ended this connection. You will no longer see their updates.")
                .font(AppFont.bodyLarge).foregroundStyle(Palette.inkSecondary).multilineTextAlignment(.center)
            Spacer()
            PillButton(title: "OK", action: onDismiss)
        }
        .padding(32).calmBackground()
    }
}
