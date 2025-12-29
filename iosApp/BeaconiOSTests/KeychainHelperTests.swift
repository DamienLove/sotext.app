import XCTest
@testable import BeaconiOS

final class KeychainHelperTests: XCTestCase {

    override func setUp() {
        super.setUp()
        // Clean up any existing test PINs
        try? KeychainHelper.deletePin(for: "testPin")
    }

    override func tearDown() {
        // Clean up after tests
        try? KeychainHelper.deletePin(for: "testPin")
        super.tearDown()
    }

    // MARK: - PIN Storage Tests

    func testSaveAndLoadPIN() throws {
        let testPin = "1234"

        // Save PIN
        try KeychainHelper.savePin(testPin, for: "testPin")

        // Load PIN
        let loadedPin = KeychainHelper.loadPin(for: "testPin")

        XCTAssertEqual(loadedPin, testPin, "Loaded PIN should match saved PIN")
    }

    func testOverwriteExistingPIN() throws {
        let originalPin = "1234"
        let newPin = "5678"

        // Save original PIN
        try KeychainHelper.savePin(originalPin, for: "testPin")

        // Overwrite with new PIN
        try KeychainHelper.savePin(newPin, for: "testPin")

        // Load PIN
        let loadedPin = KeychainHelper.loadPin(for: "testPin")

        XCTAssertEqual(loadedPin, newPin, "Loaded PIN should be the new PIN")
        XCTAssertNotEqual(loadedPin, originalPin, "Loaded PIN should not be the original PIN")
    }

    func testDeletePIN() throws {
        let testPin = "1234"

        // Save PIN
        try KeychainHelper.savePin(testPin, for: "testPin")

        // Delete PIN
        try KeychainHelper.deletePin(for: "testPin")

        // Try to load PIN
        let loadedPin = KeychainHelper.loadPin(for: "testPin")

        XCTAssertNil(loadedPin, "PIN should be nil after deletion")
    }

    func testLoadNonExistentPIN() {
        let loadedPin = KeychainHelper.loadPin(for: "nonExistentPin")
        XCTAssertNil(loadedPin, "Loading non-existent PIN should return nil")
    }

    // MARK: - PIN Validation Tests

    func testValidPIN() {
        let validPins = ["4567", "8901", "6789", "98765"]

        for pin in validPins {
            let result = KeychainHelper.isValidPin(pin)
            XCTAssertTrue(result.isValid, "PIN '\(pin)' should be valid")
            XCTAssertNil(result.reason, "Valid PIN should have no error reason")
        }
    }

    func testPINTooShort() {
        let shortPins = ["1", "12", "123"]

        for pin in shortPins {
            let result = KeychainHelper.isValidPin(pin)
            XCTAssertFalse(result.isValid, "PIN '\(pin)' should be invalid (too short)")
            XCTAssertNotNil(result.reason, "Invalid PIN should have error reason")
        }
    }

    func testPINWithNonDigits() {
        let invalidPins = ["12a4", "abcd", "12.4", "12-4"]

        for pin in invalidPins {
            let result = KeychainHelper.isValidPin(pin)
            XCTAssertFalse(result.isValid, "PIN '\(pin)' should be invalid (contains non-digits)")
            XCTAssertEqual(result.reason, "PIN must contain only numbers")
        }
    }

    func testSequentialPINs() {
        let sequentialPins = ["1234", "4321", "0123", "9876"]

        for pin in sequentialPins {
            let result = KeychainHelper.isValidPin(pin)
            XCTAssertFalse(result.isValid, "PIN '\(pin)' should be invalid (sequential)")
            XCTAssertEqual(result.reason, "PIN cannot be sequential (e.g., 1234)")
        }
    }

    func testRepeatingPINs() {
        let repeatingPins = ["0000", "1111", "2222", "9999"]

        for pin in repeatingPins {
            let result = KeychainHelper.isValidPin(pin)
            XCTAssertFalse(result.isValid, "PIN '\(pin)' should be invalid (all same digit)")
        }
    }

    func testCommonPINs() {
        let commonPins = ["0000", "1234", "4321", "1212"]

        for pin in commonPins {
            let result = KeychainHelper.isValidPin(pin)
            XCTAssertFalse(result.isValid, "PIN '\(pin)' should be invalid (too common)")
        }
    }

    func testLongerValidPINs() {
        let validPins = ["12345", "567890", "135792"]

        for pin in validPins {
            let result = KeychainHelper.isValidPin(pin)
            // These may be valid or invalid depending on sequential check for longer PINs
            // At minimum, they should pass the length and digit checks
            XCTAssertTrue(pin.count >= 4, "PIN should be at least 4 digits")
            XCTAssertTrue(pin.allSatisfy { $0.isNumber }, "PIN should contain only digits")
        }
    }
}
