import Foundation
import Combine
import BioCueCore

// WEARER-ONLY. Manages the wearer's circle: pairing code, pending requests, connected caregivers, and applying the
// reviews caregivers write. All pairing rules live in the tested `WearerPairingLifecycle` (BioCueCore); this class only
// drives it and publishes state. It only ever sees `WearerCloud`.

@MainActor
final class WearerCircleViewModel: ObservableObject {
    @Published private(set) var pairingCode = ""
    @Published private(set) var secondsRemaining = 0
    @Published private(set) var connected: [Pairing] = []
    @Published private(set) var pending: [Pairing] = []
    @Published var errorMessage: String?
    @Published private(set) var isWorking = false

    static let maxCaregivers = WearerPairingLifecycle.maxCaregivers

    private let uid: String
    private let cloud: WearerCloud
    private let lifecycle: WearerPairingLifecycle
    private let now: @Sendable () -> Date
    private var token: ListenerToken?
    private var reviewTokens: [String: ListenerToken] = [:]
    private var countdown: Task<Void, Never>?
    private var expiresAt: Int64 = 0

    init(uid: String, gate: RoleGate, cloud: WearerCloud, now: @escaping @Sendable () -> Date = Date.init) throws {
        try gate.require(\.manageCaregivers, action: "manage caregivers")
        self.uid = uid
        self.cloud = cloud
        self.now = now
        self.lifecycle = WearerPairingLifecycle(store: cloud, nowMs: { Int64(now().timeIntervalSince1970 * 1000) })
    }

    var canAddCaregiver: Bool { connected.count < Self.maxCaregivers }

    func start() {
        guard token == nil else { return }
        token = cloud.observeCaregivers(wearerUid: uid) { [weak self] result in
            Task { @MainActor in
                guard let self = self else { return }
                switch result {
                case .success(let list):
                    self.connected = list.filter { $0.status == .approved }
                    self.pending = list.filter { $0.status == .pending }
                    await self.afterPairingsChanged()
                case .failure(let error):
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func stop() { token = nil; reviewTokens = [:]; countdown?.cancel() }

    /// Spec 07: the code is single-use. As soon as a request is pending the lifecycle consumes it; hide it here too.
    private func afterPairingsChanged() async {
        try? await lifecycle.onPairingsChanged(wearerUid: uid)
        if !pending.isEmpty && !pairingCode.isEmpty {
            pairingCode = ""
            countdown?.cancel()
        }
    }

    func generateCode() async {
        isWorking = true; defer { isWorking = false }
        do {
            switch try await lifecycle.generateCode(wearerUid: uid) {
            case .maxCaregiversReached:
                errorMessage = "You already have \(Self.maxCaregivers) caregivers. Remove one first."
            case .issued(let code, let expires):
                pairingCode = code
                expiresAt = expires
                startCountdown(for: code)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func startCountdown(for code: String) {
        countdown?.cancel()
        countdown = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self, self.pairingCode == code else { return }
                let remaining = Int((TimeInterval(self.expiresAt) / 1000) - self.now().timeIntervalSince1970)
                self.secondsRemaining = max(0, remaining)
                if remaining <= 0 {
                    self.pairingCode = ""
                    try? await self.cloud.consumeCodes(wearerUid: self.uid)
                    return
                }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    func approve(_ pairing: Pairing) async {
        do {
            if try await lifecycle.approve(wearerUid: uid, pairingId: pairing.id) == .maxCaregiversReached {
                errorMessage = "You already have \(Self.maxCaregivers) caregivers. Remove one first."
            }
        } catch { errorMessage = error.localizedDescription }
    }

    /// Spec 07: Deny deletes the pairing document.
    func deny(_ pairing: Pairing) async {
        do { try await lifecycle.deny(pairing.id) } catch { errorMessage = error.localizedDescription }
    }

    /// Spec 07: Remove sets REMOVED + removedAt.
    func remove(_ pairing: Pairing) async {
        do { try await lifecycle.remove(pairing.id) } catch { errorMessage = error.localizedDescription }
    }

    /// Spec 07: caregivers write reviews to `pairings/{id}/reviews`; `apply` decides (first resolution wins).
    func startReviewImport(apply: @escaping @MainActor (Review) async -> Void) {
        let ids = Set(connected.map { $0.id })
        reviewTokens = reviewTokens.filter { ids.contains($0.key) }   // dropping a token cancels that listener
        for p in connected where reviewTokens[p.id] == nil {
            reviewTokens[p.id] = cloud.observeReviews(pairingId: p.id) { result in
                guard case .success(let reviews) = result else { return }
                Task { @MainActor in for review in reviews { await apply(review) } }
            }
        }
    }
}
