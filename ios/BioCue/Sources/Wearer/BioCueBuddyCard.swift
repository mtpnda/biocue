import SwiftUI
import BioCueCore
#if canImport(UIKit)
import UIKit
#endif

/// BioCue Buddy Card: Compassionate on-device co-regulation, breathing pacer,
/// executive function support, and safety guardian for the wearer (Spec 19).
struct BioCueBuddyCard: View {
    @ObservedObject var service: WearerService
    @State private var isPacerActive = false
    @State private var pacerScale: CGFloat = 0.85
    @State private var pacerPhaseText = "Breathe in"
    @State private var pacerSubtitleText = "Feel your chest expand · 4s"
    @State private var noteText = ""
    @State private var pacerTimer: Timer?

    private var guidance: CompanionGuidance? {
        service.companionGuidance
    }

    var body: some View {
        if let g = guidance {
            SoftCard(container: cardBackground(for: g), border: cardBorder(for: g), padding: 18) {
                VStack(alignment: .leading, spacing: 14) {
                    // Header: Buddy identity + Status Dot
                    HStack(alignment: .center, spacing: 10) {
                        ZStack {
                            Circle()
                                .fill(badgeFill(for: g))
                                .frame(width: 32, height: 32)
                            Text(badgeEmoji(for: g))
                                .font(.system(size: 16))
                        }
                        VStack(alignment: .leading, spacing: 1) {
                            Text("BIOCUE BUDDY")
                                .font(AppFont.labelSmall)
                                .tracking(0.6)
                                .foregroundStyle(badgeInk(for: g))
                            Text(phaseName(g.phase))
                                .font(AppFont.bodySmall)
                                .foregroundStyle(Palette.inkSecondary)
                        }
                        Spacer()
                        Circle()
                            .fill(g.isEmergencyEscalation ? StatePalette.alert.ink : StatePalette.calm.ink)
                            .frame(width: 8, height: 8)
                    }

                    // Headline & Empathetic Copy
                    VStack(alignment: .leading, spacing: 4) {
                        Text(g.headline)
                            .font(AppFont.titleLarge)
                            .foregroundStyle(Palette.inkPrimary)
                        Text(g.empatheticMessage)
                            .font(AppFont.bodyMedium)
                            .foregroundStyle(Palette.inkSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    // Interactive Guided Breathing Pacer Pod
                    VStack(spacing: 12) {
                        HStack {
                            HStack(spacing: 6) {
                                Image(systemName: "wind")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(StatePalette.calm.ink)
                                Text("GUIDED BREATHING")
                                    .font(AppFont.labelSmall)
                                    .tracking(0.5)
                                    .foregroundStyle(Palette.inkSecondary)
                            }
                            Spacer()
                            Button(action: togglePacer) {
                                HStack(spacing: 5) {
                                    Image(systemName: isPacerActive ? "pause.fill" : "play.fill")
                                        .font(.system(size: 9, weight: .bold))
                                    Text(isPacerActive ? "Pause" : "Start")
                                        .font(AppFont.labelSmall)
                                }
                                .foregroundStyle(isPacerActive ? Palette.onCharcoal : Palette.inkPrimary)
                                .padding(.horizontal, 12)
                                .frame(height: 28)
                                .background(isPacerActive ? Palette.charcoal : Palette.card, in: Capsule())
                                .overlay(Capsule().stroke(Palette.line, lineWidth: isPacerActive ? 0 : 1))
                            }
                            .buttonStyle(.plain)
                        }

                        // Organic Concentric Breathing Orb
                        ZStack {
                            // Layer 1: Ambient Calming Glow
                            Circle()
                                .fill(
                                    RadialGradient(
                                        colors: [StatePalette.calm.chip.opacity(isPacerActive ? 0.50 : 0.20), StatePalette.calm.soft.opacity(0.0)],
                                        center: .center,
                                        startRadius: 10,
                                        endRadius: 60
                                    )
                                )
                                .frame(width: 120, height: 120)
                                .scaleEffect(isPacerActive ? pacerScale : 0.85)

                            // Layer 2: Harmonic Halo Ring
                            Circle()
                                .stroke(StatePalette.calm.ink.opacity(isPacerActive ? 0.35 : 0.15), lineWidth: 1.5)
                                .frame(width: 82, height: 82)
                                .scaleEffect(isPacerActive ? pacerScale * 0.95 : 0.85)

                            // Layer 3: Floating Pebble Anchor
                            ZStack {
                                Circle()
                                    .fill(Palette.card)
                                    .frame(width: 58, height: 58)
                                    .softShadow(radius: 6, y: 2)

                                Image(systemName: isPacerActive ? "lungs.fill" : "sparkles")
                                    .font(.system(size: 20, weight: .medium))
                                    .foregroundStyle(StatePalette.calm.ink)
                            }
                            .scaleEffect(isPacerActive ? (pacerScale > 1.0 ? 1.04 : 0.96) : 1.0)
                        }
                        .frame(height: 110)
                        .frame(maxWidth: .infinity)
                        .clipped()

                        // Phase Guidance & Rhythm Label
                        VStack(spacing: 2) {
                            Text(isPacerActive ? pacerPhaseText : "Paced Regulation")
                                .font(AppFont.titleMedium)
                                .foregroundStyle(Palette.inkPrimary)
                                .animation(.easeInOut(duration: 0.25), value: pacerPhaseText)

                            Text(isPacerActive ? pacerSubtitleText : "Rhythm: \(g.suggestedRhythm.rawValue)")
                                .font(AppFont.bodySmall)
                                .foregroundStyle(Palette.inkSecondary)
                                .animation(.easeInOut(duration: 0.25), value: pacerSubtitleText)
                        }
                    }
                    .padding(14)
                    .background(Palette.surface2, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).stroke(Palette.line, lineWidth: 1))
                    .clipped()

                    // Free-Text Reflection / Context Input
                    HStack(spacing: 8) {
                        TextField("Add a note (e.g. noise, panic, dizzy)...", text: $noteText)
                            .font(AppFont.bodySmall)
                            .padding(.horizontal, 12)
                            .frame(minHeight: 38)
                            .background(Palette.surface2, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Palette.line, lineWidth: 1))

                        Button(action: saveNote) {
                            Text("Save")
                                .font(AppFont.labelSmall)
                                .foregroundStyle(noteText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Palette.inkMuted : Palette.onCharcoal)
                                .padding(.horizontal, 14)
                                .frame(minHeight: 38)
                                .background(noteText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Palette.surface2 : Palette.charcoal, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        .buttonStyle(.plain)
                        .disabled(noteText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }

                    // Primary Action & Safety Controls
                    VStack(spacing: 8) {
                        if g.isEmergencyEscalation || caseGuardianGrace {
                            // High priority safety buttons
                            PillButton(title: "I Am Safe · Still Coping") {
                                service.confirmSafe()
                            }
                            HStack(spacing: 10) {
                                OutlinedPillButton(title: "End Event") {
                                    Task { await service.endOngoingEvent() }
                                }
                                OutlinedPillButton(title: "Dial 988 Lifeline", ink: StatePalette.alert.ink) {
                                    if let url = URL(string: "tel://988") {
                                        #if canImport(UIKit)
                                        UIApplication.shared.open(url)
                                        #endif
                                    }
                                }
                            }
                        } else {
                            PillButton(title: g.primaryActionTitle) {
                                Task { await service.endOngoingEvent() }
                            }
                            if g.phase == 3 {
                                Button(action: { service.confirmSafe() }) {
                                    Text("I am safe · Just need some time")
                                        .font(AppFont.labelMedium)
                                        .foregroundStyle(Palette.inkSecondary)
                                        .frame(maxWidth: .infinity, minHeight: 34)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
            .onDisappear {
                stopPacer()
            }
        }
    }

    private var caseGuardianGrace: Bool {
        if case .graceCountdown = service.guardianState { return true }
        if case .crisisUnresponsive = service.guardianState { return true }
        return false
    }

    private func togglePacer() {
        if isPacerActive {
            stopPacer()
        } else {
            startPacer()
        }
    }

    private func startPacer() {
        isPacerActive = true
        cyclePacer()
    }

    private func cyclePacer() {
        guard isPacerActive else { return }
        #if canImport(UIKit)
        let impact = UIImpactFeedbackGenerator(style: .soft)
        impact.prepare()
        impact.impactOccurred()
        #endif

        // Inhale 4s
        pacerPhaseText = "Breathe in slowly"
        pacerSubtitleText = "Feel your chest expand · 4s"
        withAnimation(.easeInOut(duration: 4.0)) { pacerScale = 1.15 }

        DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
            guard self.isPacerActive else { return }
            self.pacerPhaseText = "Hold gently"
            self.pacerSubtitleText = "Soften your jaw & shoulders · 4s"
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                guard self.isPacerActive else { return }
                #if canImport(UIKit)
                impact.impactOccurred()
                #endif
                self.pacerPhaseText = "Slow exhale"
                self.pacerSubtitleText = "Gently release all tension · 6s"
                withAnimation(.easeInOut(duration: 5.0)) { self.pacerScale = 0.75 }
                DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                    if self.isPacerActive { self.cyclePacer() }
                }
            }
        }
    }

    private func stopPacer() {
        isPacerActive = false
        pacerScale = 0.85
        pacerPhaseText = "Breathe in slowly"
        pacerSubtitleText = "Feel your chest expand · 4s"
        pacerTimer?.invalidate()
        pacerTimer = nil
    }

    private func saveNote() {
        let note = noteText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !note.isEmpty else { return }
        Task {
            await service.addOngoingNote(note)
            noteText = ""
        }
    }

    private func phaseName(_ phase: Int) -> String {
        switch phase {
        case 1: return "Presence · Listening"
        case 2: return "Co-Regulation · Pacing"
        case 3: return "Safety Check-in"
        case 4: return "Recovery · Return to Calm"
        default: return "Active Support"
        }
    }

    private func cardBackground(for g: CompanionGuidance) -> Color {
        if g.isEmergencyEscalation { return StatePalette.alert.soft }
        if g.phase == 4 { return StatePalette.calm.soft }
        return Palette.card
    }

    private func cardBorder(for g: CompanionGuidance) -> Color {
        if g.isEmergencyEscalation { return StatePalette.alert.chip }
        if g.phase == 4 { return StatePalette.calm.chip }
        return Palette.line
    }

    private func badgeFill(for g: CompanionGuidance) -> Color {
        if g.isEmergencyEscalation { return StatePalette.alert.chip }
        if g.phase == 4 { return StatePalette.calm.chip }
        return StatePalette.info.soft
    }

    private func badgeInk(for g: CompanionGuidance) -> Color {
        if g.isEmergencyEscalation { return StatePalette.alert.ink }
        if g.phase == 4 { return StatePalette.calm.ink }
        return StatePalette.info.ink
    }

    private func badgeEmoji(for g: CompanionGuidance) -> String {
        if g.isEmergencyEscalation { return "🚨" }
        if g.phase == 4 { return "✨" }
        if g.phase == 2 { return "🌿" }
        return "🌱"
    }
}
