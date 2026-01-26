
import {submitExtension} from "./extensions";
import * as admin from "firebase-admin";

// --- Mocks Setup ---

jest.mock("firebase-admin", () => {
  const mSet = jest.fn().mockResolvedValue({});
  const mGet = jest.fn().mockResolvedValue({exists: false}); // Default: doc doesn't exist
  const mDoc = jest.fn().mockReturnValue({set: mSet, get: mGet});
  const mCollection = jest.fn().mockReturnValue({doc: mDoc});

  // Mock firestore function
  const mFirestore: any = jest.fn().mockReturnValue({collection: mCollection});
  // Mock firestore.FieldValue
  mFirestore.FieldValue = {serverTimestamp: jest.fn().mockReturnValue("TIMESTAMP")};

  return {
    firestore: mFirestore,
    initializeApp: jest.fn(),
  };
});

jest.mock("firebase-functions/v2/https", () => ({
  onCall: (handler: any) => handler, // Unwrap the handler
  HttpsError: class extends Error {
    code: string;
    constructor(code: string, message: string) {
      super(message);
      this.code = code;
    }
  },
}));

jest.mock("firebase-functions/logger", () => ({
  error: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
}));

describe("submitExtension Security", () => {
  let firestoreMock: any;

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = (admin.firestore as unknown as jest.Mock);
  });

  it("should STRIP polluted fields (SECURITY FIX)", async () => {
    const request = {
      auth: {uid: "attacker-1"},
      data: {
        manifest: {
          id: "polluted-ext",
          name: "Polluted Extension",
          entry_point: "https://example.com",
          hugeField: "JUNK DATA", // Polluted field
          isAdmin: true, // Attempted privilege escalation
        },
      },
    };

    await (submitExtension as any)(request);

    const collectionMock = firestoreMock().collection;
    const docMock = collectionMock().doc;
    const setMock = docMock().set;

    expect(collectionMock).toHaveBeenCalledWith("extensions_store");
    expect(docMock).toHaveBeenCalledWith("polluted-ext");

    // Assert that polluted fields are NOT present
    expect(setMock).toHaveBeenCalledWith(
        expect.not.objectContaining({
          hugeField: "JUNK DATA",
          isAdmin: true,
        }),
        {merge: true},
    );

    // Assert that valid fields are present
    expect(setMock).toHaveBeenCalledWith(
        expect.objectContaining({
          id: "polluted-ext",
          name: "Polluted Extension",
          entry_point: "https://example.com",
          ownerUid: "attacker-1",
        }),
        {merge: true},
    );
  });

  it("should REJECT javascript: URLs (SECURITY FIX)", async () => {
    const request = {
      auth: {uid: "attacker-1"},
      data: {
        manifest: {
          id: "xss-ext",
          name: "XSS Extension",
          entry_point: "javascript:alert(1)", // Malicious URL
        },
      },
    };

    // Should throw invalid-argument
    await expect((submitExtension as any)(request))
        .rejects
        .toThrow("entry_point must be a valid http/https URL.");

    // Ensure NO write happened
    const setMock = firestoreMock().collection().doc().set;
    expect(setMock).not.toHaveBeenCalled();
  });
});
