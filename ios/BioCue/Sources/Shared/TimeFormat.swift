import Foundation

/// Role-agnostic clock formatting ("h:mm a", en_US so it matches the Android app).
enum TimeFormat {
    private static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "h:mm a"
        return f
    }()

    static func clock(_ ms: Int64) -> String {
        formatter.string(from: Date(timeIntervalSince1970: TimeInterval(ms) / 1000))
    }
}
