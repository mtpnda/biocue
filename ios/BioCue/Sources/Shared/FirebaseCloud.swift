import Foundation
import FirebaseFirestore
import BioCueCore

/// Firestore implementation of all three role protocols. Documents are read/written with the SAME field names and types
/// as the Android app so both platforms interoperate on one database. Matches docs/spec/07_MULTI_ACCOUNT.md (as amended):
/// direct code lookup (never a list), single-use codes, Deny deletes, removal records `removedAt`, re-pairing on the same
/// document, and caregiver reviews in `pairings/{id}/reviews`.
final class FirebaseCloud: ProfileCloud, WearerCloud, CaregiverCloud, @unchecked Sendable {
    private let db = Firestore.firestore()

    private static func nowMs() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    // MARK: Parsing

    static func profile(from data: [String: Any], fallbackUid: String) -> UserProfile? {
        guard let roleRaw = data["role"] as? String, let role = UserRole(rawValue: roleRaw) else { return nil }
        return UserProfile(
            uid: (data["uid"] as? String) ?? fallbackUid,
            role: role,
            displayName: (data["displayName"] as? String) ?? "",
            pairingCode: data["pairingCode"] as? String,
            pairingCodeExpiresAt: (data["pairingCodeExpiresAt"] as? NSNumber)?.int64Value
        )
    }

    static func pairing(from data: [String: Any], id: String) -> Pairing? {
        guard let statusRaw = data["status"] as? String, let status = PairingStatus(rawValue: statusRaw) else { return nil }
        return Pairing(
            id: (data["id"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? id,
            wearerUid: (data["wearerUid"] as? String) ?? "",
            caregiverUid: (data["caregiverUid"] as? String) ?? "",
            caregiverName: (data["caregiverName"] as? String) ?? "",
            wearerName: (data["wearerName"] as? String) ?? "",
            status: status,
            createdAt: (data["createdAt"] as? NSNumber)?.int64Value ?? 0,
            removedAt: (data["removedAt"] as? NSNumber)?.int64Value
        )
    }

    static func event(from data: [String: Any], wearerUid: String) -> StressEvent? {
        guard let id = data["id"] as? String, !id.isEmpty else { return nil }
        return StressEvent(
            id: id,
            userId: wearerUid,
            startTime: (data["startTime"] as? NSNumber)?.int64Value ?? 0,
            endTime: (data["endTime"] as? NSNumber)?.int64Value,
            triggerType: (data["triggerType"] as? String) ?? "MANUAL",
            label: (data["label"] as? String) ?? "NONE",
            isResolved: (data["isResolved"] as? Bool) ?? false
        )
    }

    // MARK: ProfileCloud

    func observeUserProfile(uid: String, onChange: @escaping (Result<UserProfile?, Error>) -> Void) -> ListenerToken {
        let reg = db.collection("users").document(uid).addSnapshotListener { snap, error in
            if let error = error { onChange(.failure(error)); return }
            guard let snap = snap, snap.exists, let data = snap.data() else { onChange(.success(nil)); return }
            onChange(.success(Self.profile(from: data, fallbackUid: uid)))
        }
        return ListenerToken { reg.remove() }
    }

    func createUserProfile(_ profile: UserProfile) async throws {
        let data: [String: Any] = [
            "uid": profile.uid,
            "role": profile.role.rawValue,
            "displayName": profile.displayName,
            "pairingCode": NSNull(),
            "pairingCodeExpiresAt": NSNull()
        ]
        try await db.collection("users").document(profile.uid).setData(data)
    }

    // MARK: WearerPairingStore

    func pairingsOfWearer(_ wearerUid: String) async throws -> [Pairing] {
        let snap = try await db.collection("pairings").whereField("wearerUid", isEqualTo: wearerUid).getDocuments()
        return snap.documents.compactMap { Self.pairing(from: $0.data(), id: $0.documentID) }
    }

    func issueCode(wearerUid: String, expiresAt: Int64) async throws -> String {
        for _ in 0..<10 {
            let candidate = String(format: "%06d", Int.random(in: 0..<1_000_000))
            let codeRef = db.collection("active_codes").document(candidate)
            let userRef = db.collection("users").document(wearerUid)

            let claimed: Bool = try await withCheckedThrowingContinuation { cont in
                db.runTransaction({ (transaction, errorPointer) -> Any? in
                    let snap: DocumentSnapshot
                    do {
                        snap = try transaction.getDocument(codeRef)
                    } catch let e as NSError {
                        errorPointer?.pointee = e
                        return nil
                    }
                    let existingExpiry = (snap.data()?["expiresAt"] as? NSNumber)?.int64Value ?? 0
                    if snap.exists && existingExpiry >= Self.nowMs() { return false } // live code owned by someone: try another
                    transaction.setData(["wearerUid": wearerUid, "expiresAt": expiresAt], forDocument: codeRef)
                    transaction.updateData(["pairingCode": candidate, "pairingCodeExpiresAt": expiresAt], forDocument: userRef)
                    return true
                }, completion: { result, error in
                    if let error = error { cont.resume(throwing: error) }
                    else { cont.resume(returning: (result as? Bool) ?? false) }
                })
            }
            if claimed { return candidate }
        }
        throw CloudError.couldNotAllocateCode
    }

    /// Single-use (spec 07): delete the lookup record and clear the profile fields.
    func consumeCodes(wearerUid: String) async throws {
        let userRef = db.collection("users").document(wearerUid)
        let code = try await userRef.getDocument().data()?["pairingCode"] as? String
        if let code = code { try await db.collection("active_codes").document(code).delete() }
        try await userRef.updateData(["pairingCode": NSNull(), "pairingCodeExpiresAt": NSNull()])
    }

    /// Spec 07 Deny: the pairing document is deleted.
    func deletePairing(_ pairingId: String) async throws {
        try await db.collection("pairings").document(pairingId).delete()
    }

    /// Spec 07 Removal: REMOVED plus removedAt.
    func markRemoved(_ pairingId: String, removedAt: Int64) async throws {
        try await db.collection("pairings").document(pairingId)
            .updateData(["status": PairingStatus.removed.rawValue, "removedAt": removedAt])
    }

    func markApproved(_ pairingId: String) async throws {
        try await db.collection("pairings").document(pairingId).updateData(["status": PairingStatus.approved.rawValue])
    }

    // MARK: WearerCloud

    func syncEvent(wearerUid: String, event: StressEvent) async throws {
        let data: [String: Any] = [
            "id": event.id,
            "startTime": event.startTime,
            "endTime": event.endTime.map { $0 as Any } ?? NSNull(),
            "triggerType": event.triggerType,
            "label": event.label,
            "isResolved": event.isResolved
        ]
        try await db.collection("wearerData").document(wearerUid).collection("events").document(event.id).setData(data)
    }

    func observeCaregivers(wearerUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken {
        let reg = db.collection("pairings").whereField("wearerUid", isEqualTo: wearerUid).addSnapshotListener { snap, error in
            if let error = error { onChange(.failure(error)); return }
            let list = (snap?.documents ?? []).compactMap { Self.pairing(from: $0.data(), id: $0.documentID) }
            onChange(.success(list))
        }
        return ListenerToken { reg.remove() }
    }

    // MARK: CaregiverPairingStore

    /// Direct `get` of the code, then of the wearer's profile. There is no list/query on users or active_codes.
    func lookupCode(_ code: String) async throws -> CodeRecord? {
        let codeDoc = try await db.collection("active_codes").document(code).getDocument()
        guard codeDoc.exists, let data = codeDoc.data(), let wearerUid = data["wearerUid"] as? String else { return nil }
        let expiresAt = (data["expiresAt"] as? NSNumber)?.int64Value ?? 0
        let userDoc = try await db.collection("users").document(wearerUid).getDocument()
        let name = (userDoc.data()?["displayName"] as? String) ?? ""
        return CodeRecord(wearerUid: wearerUid, wearerName: name, expiresAt: expiresAt)
    }

    func pairing(_ pairingId: String) async throws -> Pairing? {
        let doc = try await db.collection("pairings").document(pairingId).getDocument()
        guard doc.exists, let data = doc.data() else { return nil }
        return Self.pairing(from: data, id: pairingId)
    }

    func createPairing(_ pairing: Pairing) async throws {
        let data: [String: Any] = [
            "id": pairing.id,
            "wearerUid": pairing.wearerUid,
            "caregiverUid": pairing.caregiverUid,
            "caregiverName": pairing.caregiverName,
            "wearerName": pairing.wearerName,
            "status": pairing.status.rawValue,
            "createdAt": pairing.createdAt
        ]
        try await db.collection("pairings").document(pairing.id).setData(data)
    }

    /// Spec 07 re-pairing: REMOVED -> PENDING on the same document; removedAt cleared.
    func reRequest(_ pairingId: String, caregiverName: String, createdAt: Int64) async throws {
        try await db.collection("pairings").document(pairingId).updateData([
            "status": PairingStatus.pending.rawValue,
            "removedAt": FieldValue.delete(),
            "createdAt": createdAt,
            "caregiverName": caregiverName
        ])
    }

    // MARK: CaregiverCloud

    func observePairings(caregiverUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken {
        let reg = db.collection("pairings").whereField("caregiverUid", isEqualTo: caregiverUid).addSnapshotListener { snap, error in
            if let error = error { onChange(.failure(error)); return }
            let list = (snap?.documents ?? []).compactMap { Self.pairing(from: $0.data(), id: $0.documentID) }
            onChange(.success(list))
        }
        return ListenerToken { reg.remove() }
    }

    func observeEvents(wearerUid: String, onChange: @escaping (Result<[StressEvent], Error>) -> Void) -> ListenerToken {
        let reg = db.collection("wearerData").document(wearerUid).collection("events").addSnapshotListener { snap, error in
            if let error = error { onChange(.failure(error)); return }
            let list = (snap?.documents ?? []).compactMap { Self.event(from: $0.data(), wearerUid: wearerUid) }
            onChange(.success(list))
        }
        return ListenerToken { reg.remove() }
    }

    func leavePairing(pairingId: String) async throws {
        try await db.collection("pairings").document(pairingId)
            .updateData(["status": PairingStatus.removed.rawValue, "removedAt": Self.nowMs()])
    }

    // MARK: ReviewStore + review observation (both roles read; only the caregiver writes)

    func writeReview(_ review: Review) async throws {
        try await db.collection("pairings").document(review.pairingId).collection("reviews").document(review.eventId)
            .setData(["label": review.label, "isResolved": review.isResolved, "updatedAt": review.updatedAt])
    }

    func observeReviews(pairingId: String, onChange: @escaping (Result<[Review], Error>) -> Void) -> ListenerToken {
        let reg = db.collection("pairings").document(pairingId).collection("reviews").addSnapshotListener { snap, error in
            if let error = error { onChange(.failure(error)); return }
            let list: [Review] = (snap?.documents ?? []).compactMap { d in
                let data = d.data()
                guard let label = data["label"] as? String else { return nil }
                return Review(pairingId: pairingId, eventId: d.documentID, label: label,
                              isResolved: (data["isResolved"] as? Bool) ?? false,
                              updatedAt: (data["updatedAt"] as? NSNumber)?.int64Value ?? 0)
            }
            onChange(.success(list))
        }
        return ListenerToken { reg.remove() }
    }
}
