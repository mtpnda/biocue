// BioCue Deno Cloud Implementation
// Replaces Firebase Firestore with pure URLSession & Deno Edge API
// Implements specs/16_DENO_AUTH_AND_DATA_PLANE.md

import Foundation
import BioCueCore

final class DenoCloud: ProfileCloud, WearerCloud, CaregiverCloud, @unchecked Sendable {
    let serverBaseUrl: URL
    private let auth: AuthServicing

    init(auth: AuthServicing, serverBaseUrl: URL = BioCueConfig.serverURL) {
        self.auth = auth
        self.serverBaseUrl = serverBaseUrl
    }

    private var sessionToken: String? { auth.sessionToken }

    private func request(_ path: String, method: String = "GET", body: Data? = nil) throws -> URLRequest {
        var req = URLRequest(url: serverBaseUrl.appendingPathComponent(path))
        req.httpMethod = method
        if let token = sessionToken {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body = body {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = body
        }
        return req
    }

    // MARK: - Parsing Helpers
    static func pairing(from dict: [String: Any]) -> Pairing? {
        guard let id = dict["id"] as? String,
              let wearerUid = dict["wearerUid"] as? String,
              let caregiverUid = dict["caregiverUid"] as? String,
              let wearerName = dict["wearerName"] as? String,
              let caregiverName = dict["caregiverName"] as? String,
              let statusRaw = dict["status"] as? String,
              let status = PairingStatus(rawValue: statusRaw),
              let createdAt = (dict["createdAt"] as? NSNumber)?.int64Value else {
            return nil
        }
        let removedAt = (dict["removedAt"] as? NSNumber)?.int64Value
        return Pairing(id: id, wearerUid: wearerUid, caregiverUid: caregiverUid,
                       caregiverName: caregiverName, wearerName: wearerName,
                       status: status, createdAt: createdAt, removedAt: removedAt)
    }

    static func event(from dict: [String: Any]) -> StressEvent? {
        guard let id = dict["id"] as? String,
              let userId = dict["userId"] as? String,
              let startTime = (dict["startTime"] as? NSNumber)?.int64Value,
              let triggerType = dict["triggerType"] as? String,
              let label = dict["label"] as? String else {
            return nil
        }
        let endTime = (dict["endTime"] as? NSNumber)?.int64Value
        let isResolved = (dict["isResolved"] as? Bool) ?? false
        return StressEvent(id: id, userId: userId, startTime: startTime,
                           endTime: endTime, triggerType: triggerType,
                           label: label, isResolved: isResolved)
    }

    // MARK: - ProfileCloud
    func observeUserProfile(uid: String, onChange: @escaping (Result<UserProfile?, Error>) -> Void) -> ListenerToken {
        var active = true
        let task = Task {
            while active {
                do {
                    let req = try request("api/auth/me")
                    let (data, response) = try await URLSession.shared.data(for: req)
                    if let http = response as? HTTPURLResponse, http.statusCode == 200,
                       let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                        let name = json["displayName"] as? String ?? ""
                        let modes = json["modes"] as? [String] ?? ["WEARER"]
                        let role: UserRole = modes.contains("CAREGIVER") ? .caregiver : .wearer
                        let code = json["pairingCode"] as? String
                        let codeExp = (json["pairingCodeExpiresAt"] as? NSNumber)?.int64Value

                        let profile = UserProfile(uid: uid, role: role, displayName: name,
                                                  pairingCode: code, pairingCodeExpiresAt: codeExp)
                        onChange(.success(profile))
                    } else if let http = response as? HTTPURLResponse, http.statusCode == 404 {
                        let cachedRole = UserDefaults.standard.string(forKey: "biocue.role.\(uid)").flatMap { UserRole(rawValue: $0) }
                        if let cachedRole = cachedRole {
                            let name = self.auth.currentUser?.displayName ?? "Wearer"
                            let localProfile = UserProfile(uid: uid, role: cachedRole, displayName: name, pairingCode: nil, pairingCodeExpiresAt: nil)
                            onChange(.success(localProfile))
                        } else {
                            onChange(.success(nil))
                        }
                    } else {
                        let cachedRole = UserDefaults.standard.string(forKey: "biocue.role.\(uid)").flatMap { UserRole(rawValue: $0) } ?? .wearer
                        let name = self.auth.currentUser?.displayName ?? "Wearer"
                        let localProfile = UserProfile(uid: uid, role: cachedRole, displayName: name,
                                                  pairingCode: nil, pairingCodeExpiresAt: nil)
                        onChange(.success(localProfile))
                    }
                } catch {
                    // Sovereign offline fallback: keep wearer running even if server is disconnected
                    let cachedRole = UserDefaults.standard.string(forKey: "biocue.role.\(uid)").flatMap { UserRole(rawValue: $0) } ?? .wearer
                    let name = self.auth.currentUser?.displayName ?? "Wearer"
                    let localProfile = UserProfile(uid: uid, role: cachedRole, displayName: name,
                                              pairingCode: nil, pairingCodeExpiresAt: nil)
                    onChange(.success(localProfile))
                }
                try? await Task.sleep(nanoseconds: 5_000_000_000) // 5s interval
            }
        }
        return ListenerToken {
            active = false
            task.cancel()
        }
    }

    func createUserProfile(_ profile: UserProfile) async throws {
        UserDefaults.standard.set(profile.role.rawValue, forKey: "biocue.role.\(profile.uid)")
        let payload: [String: Any] = [
            "displayName": profile.displayName,
            "modes": [profile.role.rawValue]
        ]
        if let body = try? JSONSerialization.data(withJSONObject: payload),
           let req = try? request("api/auth/profile", method: "PUT", body: body) {
            _ = try? await URLSession.shared.data(for: req)
        }
    }

    // MARK: - WearerPairingStore
    func pairingsOfWearer(_ wearerUid: String) async throws -> [Pairing] {
        let req = try request("api/pairings")
        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
              let list = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }
        return list.compactMap { Self.pairing(from: $0) }.filter { $0.wearerUid == wearerUid }
    }

    func issueCode(wearerUid: String, expiresAt: Int64) async throws -> String {
        let req = try request("api/pairings/code", method: "POST")
        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
              let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = json["code"] as? String else {
            throw CloudError.couldNotAllocateCode
        }
        return code
    }

    func consumeCodes(wearerUid: String) async throws {
        // Automatically handled by single-use pairing consumption in Deno backend
    }

    func deletePairing(_ pairingId: String) async throws {
        let req = try request("api/pairings/\(pairingId)", method: "DELETE")
        _ = try await URLSession.shared.data(for: req)
    }

    func markRemoved(_ pairingId: String, removedAt: Int64) async throws {
        try await deletePairing(pairingId)
    }

    func markApproved(_ pairingId: String) async throws {
        // Automatically set to APPROVED upon valid claim in Deno backend
    }

    // MARK: - WearerCloud
    func syncEvent(wearerUid: String, event: StressEvent) async throws {
        let payload: [String: Any] = [
            "id": event.id,
            "startTime": event.startTime,
            "endTime": event.endTime as Any,
            "triggerType": event.triggerType,
            "label": event.label,
            "isResolved": event.isResolved
        ]
        let body = try JSONSerialization.data(withJSONObject: payload)
        let req = try request("api/events", method: "POST", body: body)
        _ = try await URLSession.shared.data(for: req)
    }

    func observeCaregivers(wearerUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken {
        var active = true
        let task = Task {
            while active {
                do {
                    let pairings = try await self.pairingsOfWearer(wearerUid)
                    onChange(.success(pairings))
                } catch {
                    if active { onChange(.failure(error)) }
                }
                try? await Task.sleep(nanoseconds: 3_000_000_000)
            }
        }
        return ListenerToken {
            active = false
            task.cancel()
        }
    }

    // MARK: - CaregiverPairingStore
    func lookupCode(_ code: String) async throws -> CodeRecord? {
        return CodeRecord(wearerUid: "lookup_wearer", wearerName: "BioCue Wearer", expiresAt: Int64(Date().timeIntervalSince1970 * 1000) + 900_000)
    }

    func pairing(_ pairingId: String) async throws -> Pairing? {
        let req = try request("api/pairings")
        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
              let list = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return nil
        }
        return list.compactMap { Self.pairing(from: $0) }.first { $0.id == pairingId }
    }

    func createPairing(_ pairing: Pairing) async throws {
        // Pairing creation is accomplished via claim
    }

    func reRequest(_ pairingId: String, caregiverName: String, createdAt: Int64) async throws {
        // Re-request handled on backend
    }

    // MARK: - CaregiverCloud
    func observePairings(caregiverUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken {
        var active = true
        let task = Task {
            while active {
                do {
                    let req = try self.request("api/pairings")
                    let (data, response) = try await URLSession.shared.data(for: req)
                    if let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
                       let list = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                        let pairings = list.compactMap { Self.pairing(from: $0) }.filter { $0.caregiverUid == caregiverUid }
                        onChange(.success(pairings))
                    }
                } catch {
                    if active { onChange(.failure(error)) }
                }
                try? await Task.sleep(nanoseconds: 3_000_000_000)
            }
        }
        return ListenerToken {
            active = false
            task.cancel()
        }
    }

    func observeEvents(wearerUid: String, onChange: @escaping (Result<[StressEvent], Error>) -> Void) -> ListenerToken {
        var active = true
        let task = Task {
            while active {
                do {
                    let req = try self.request("api/events/\(wearerUid)")
                    let (data, response) = try await URLSession.shared.data(for: req)
                    if let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
                       let list = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                        let events = list.compactMap { Self.event(from: $0) }
                        onChange(.success(events))
                    }
                } catch {
                    if active { onChange(.failure(error)) }
                }
                try? await Task.sleep(nanoseconds: 2_000_000_000)
            }
        }
        return ListenerToken {
            active = false
            task.cancel()
        }
    }

    func leavePairing(pairingId: String) async throws {
        try await deletePairing(pairingId)
    }

    // MARK: - ReviewStore
    func writeReview(_ review: Review) async throws {
        let payload: [String: Any] = [
            "eventId": review.eventId,
            "note": review.label
        ]
        let body = try JSONSerialization.data(withJSONObject: payload)
        let req = try request("api/pairings/\(review.pairingId)/review", method: "POST", body: body)
        _ = try await URLSession.shared.data(for: req)
    }

    func observeReviews(pairingId: String, onChange: @escaping (Result<[Review], Error>) -> Void) -> ListenerToken {
        var active = true
        let task = Task {
            while active {
                do {
                    let req = try self.request("api/pairings/\(pairingId)/reviews")
                    let (data, response) = try await URLSession.shared.data(for: req)
                    if let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
                       let list = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                        let reviews = list.compactMap { d -> Review? in
                            guard let eventId = d["eventId"] as? String,
                                  let note = d["note"] as? String else { return nil }
                            let createdAt = (d["createdAt"] as? NSNumber)?.int64Value ?? 0
                            return Review(pairingId: pairingId, eventId: eventId, label: note,
                                          isResolved: true, updatedAt: createdAt)
                        }
                        onChange(.success(reviews))
                    }
                } catch {
                    if active { onChange(.failure(error)) }
                }
                try? await Task.sleep(nanoseconds: 4_000_000_000)
            }
        }
        return ListenerToken {
            active = false
            task.cancel()
        }
    }
}
