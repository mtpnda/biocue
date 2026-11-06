import Foundation
import UIKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn

struct AuthUser: Equatable {
    let uid: String
    let displayName: String
}

protocol AuthServicing {
    var currentUser: AuthUser? { get }
    func addListener(_ onChange: @escaping (AuthUser?) -> Void) -> ListenerToken
    func signIn(email: String, password: String) async throws
    func register(email: String, password: String, displayName: String) async throws
    func signInWithGoogle() async throws
    func signOut() throws
}

enum AuthFailure: LocalizedError {
    case missingGoogleConfig
    case noPresenter
    case missingGoogleToken
    var errorDescription: String? {
        switch self {
        case .missingGoogleConfig: return "Google sign-in is not configured (GoogleService-Info.plist)."
        case .noPresenter: return "Could not present the Google sign-in screen."
        case .missingGoogleToken: return "Google did not return a sign-in token."
        }
    }
}

final class FirebaseAuthService: AuthServicing {
    private static func map(_ user: User?) -> AuthUser? {
        guard let user = user else { return nil }
        return AuthUser(uid: user.uid, displayName: user.displayName ?? "User")
    }

    var currentUser: AuthUser? { Self.map(Auth.auth().currentUser) }

    func addListener(_ onChange: @escaping (AuthUser?) -> Void) -> ListenerToken {
        let handle = Auth.auth().addStateDidChangeListener { _, user in onChange(Self.map(user)) }
        return ListenerToken { Auth.auth().removeStateDidChangeListener(handle) }
    }

    func signIn(email: String, password: String) async throws {
        _ = try await Auth.auth().signIn(withEmail: email, password: password)
    }

    func register(email: String, password: String, displayName: String) async throws {
        let result = try await Auth.auth().createUser(withEmail: email, password: password)
        let change = result.user.createProfileChangeRequest()
        change.displayName = displayName
        try await change.commitChanges()
    }

    @MainActor
    func signInWithGoogle() async throws {
        guard let clientID = FirebaseApp.app()?.options.clientID else { throw AuthFailure.missingGoogleConfig }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        guard let presenter = Self.topViewController() else { throw AuthFailure.noPresenter }
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        guard let idToken = result.user.idToken?.tokenString else { throw AuthFailure.missingGoogleToken }
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: result.user.accessToken.tokenString)
        _ = try await Auth.auth().signIn(with: credential)
    }

    func signOut() throws {
        try Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var top = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
