import Foundation
import Security

/// Helper class for securely storing sensitive data in the iOS Keychain
enum KeychainHelper {
    private static let service = "com.pulselink.beacon"

    enum KeychainError: Error {
        case saveFailed
        case loadFailed
        case deleteFailed
    }

    /// Save a PIN to the Keychain
    static func savePin(_ pin: String, for account: String = "privateSafePin") throws {
        guard let data = pin.data(using: .utf8) else {
            throw KeychainError.saveFailed
        }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        // Delete any existing item
        SecItemDelete(query as CFDictionary)

        // Add new item
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed
        }
    }

    /// Load a PIN from the Keychain
    static func loadPin(for account: String = "privateSafePin") -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data,
              let pin = String(data: data, encoding: .utf8) else {
            return nil
        }

        return pin
    }

    /// Delete a PIN from the Keychain
    static func deletePin(for account: String = "privateSafePin") throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deleteFailed
        }
    }

    /// Validate PIN strength
    static func isValidPin(_ pin: String) -> (isValid: Bool, reason: String?) {
        // Must be at least 4 digits
        guard pin.count >= 4 else {
            return (false, "PIN must be at least 4 digits")
        }

        // Must be all digits
        guard pin.allSatisfy({ $0.isNumber }) else {
            return (false, "PIN must contain only numbers")
        }

        // Check for sequential numbers (e.g., 1234, 4321)
        let isSequential = isSequentialNumbers(pin)
        if isSequential {
            return (false, "PIN cannot be sequential (e.g., 1234)")
        }

        // Check for all same digits (e.g., 1111)
        let isRepeating = Set(pin).count == 1
        if isRepeating {
            return (false, "PIN cannot be all the same digit")
        }

        // Check for common PINs
        let commonPins = ["0000", "1234", "4321", "1111", "2222", "3333", "4444",
                         "5555", "6666", "7777", "8888", "9999", "1212", "0123"]
        if commonPins.contains(pin) {
            return (false, "PIN is too common, please choose a different one")
        }

        return (true, nil)
    }

    private static func isSequentialNumbers(_ pin: String) -> Bool {
        let digits = pin.compactMap { Int(String($0)) }
        guard digits.count == pin.count else { return false }

        // Check ascending sequence
        var isAscending = true
        for i in 1..<digits.count {
            if digits[i] != digits[i-1] + 1 {
                isAscending = false
                break
            }
        }

        // Check descending sequence
        var isDescending = true
        for i in 1..<digits.count {
            if digits[i] != digits[i-1] - 1 {
                isDescending = false
                break
            }
        }

        return isAscending || isDescending
    }
}
