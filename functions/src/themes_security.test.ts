import {onThemeSubmitted} from "./themes";
import * as admin from "firebase-admin";
import {MAX_URL_LENGTH} from "./security";

// --- Mocks Setup ---

jest.mock("firebase-admin", () => {
  const mSet = jest.fn().mockResolvedValue({});
  const mDelete = jest.fn().mockResolvedValue({});
  const mUpdate = jest.fn().mockResolvedValue({});
  const mDoc = jest.fn().mockReturnValue({
    set: mSet,
    delete: mDelete,
    update: mUpdate,
  });
  const mCollection = jest.fn().mockReturnValue({doc: mDoc});
  const mRunTransaction = jest.fn().mockImplementation(async (cb: any) => {
    const t = {
      set: mSet,
      delete: mDelete,
    };
    await cb(t);
  });

  const mFirestore: any = jest.fn().mockReturnValue({
    collection: mCollection,
    runTransaction: mRunTransaction,
  });
  mFirestore.FieldValue = {
    serverTimestamp: jest.fn().mockReturnValue("TIMESTAMP"),
  };

  return {
    firestore: mFirestore,
    initializeApp: jest.fn(),
    apps: [], // Mock apps array
  };
});

jest.mock("firebase-functions/v2/firestore", () => ({
  onDocumentWritten: (trigger: string, handler: any) => handler,
}));

jest.mock("firebase-functions/logger", () => ({
  error: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
}));

describe("onThemeSubmitted Security", () => {
  let firestoreMock: any;
  let deleteMock: any;
  let setMock: any;
  let updateMock: any;

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = (admin.firestore as unknown as jest.Mock);
    // Access the spies created in the factory
    // We trigger the chain to get the mock functions
    const db = firestoreMock();
    const collection = db.collection();
    const doc = collection.doc();
    deleteMock = doc.delete;
    setMock = doc.set;
    updateMock = doc.update;
  });

  const createEvent = (data: any, exists = true) => ({
    data: {
      after: {
        exists,
        data: () => data,
        ref: {delete: deleteMock, update: updateMock},
      },
    },
    params: {themeId: "test-theme-1"},
  });

  it("should AUTO-APPROVE safe text-only themes", async () => {
    const safeTheme = {
      name: "Safe Theme",
      status: "pending",
      theme: {
        primaryColor: "#000000",
        // No backgroundImageUrl
      },
    };
    const event = createEvent(safeTheme);

    await (onThemeSubmitted as any)(event);

    // Verify transaction wrote to public
    expect(setMock).toHaveBeenCalledWith(
        expect.anything(), // publicRef
        expect.objectContaining({
          status: "approved",
          approvedBy: "system",
        }),
    );
    // Verify submission deleted
    expect(deleteMock).toHaveBeenCalled();
  });

  it("should REQUIRE REVIEW for themes with background images", async () => {
    const imageTheme = {
      name: "Image Theme",
      status: "pending",
      theme: {
        backgroundImageUrl: "https://example.com/bg.png",
      },
    };
    const event = createEvent(imageTheme);

    await (onThemeSubmitted as any)(event);

    // Should NOT auto-approve (no set called)
    expect(setMock).not.toHaveBeenCalled();
    // Should NOT delete (no delete called) - stays in pending
    expect(deleteMock).not.toHaveBeenCalled();
  });

  it("should REJECT unsafe (javascript:) URLs in background", async () => {
    const xssTheme = {
      name: "XSS Theme",
      status: "pending",
      theme: {
        backgroundImageUrl: "javascript:alert(1)",
      },
    };
    const event = createEvent(xssTheme);

    await (onThemeSubmitted as any)(event);

    // Should delete the submission (reject) immediately
    expect(deleteMock).toHaveBeenCalled();
    // Should NOT approve
    expect(setMock).not.toHaveBeenCalled();
  });

  it("should REJECT unsafe (javascript:) URLs in icons", async () => {
    const xssTheme = {
      name: "XSS Icon Theme",
      status: "pending",
      theme: {
        iconOverrides: {
          "icon.back": "javascript:alert(1)",
        },
      },
    };
    const event = createEvent(xssTheme);

    await (onThemeSubmitted as any)(event);

    // Should delete the submission (reject) immediately
    expect(deleteMock).toHaveBeenCalled();
    // Should NOT approve
    expect(setMock).not.toHaveBeenCalled();
  });

  it("should REJECT themes with long names even if they have images", async () => {
    const longNameTheme = {
      name: "A".repeat(51), // Too long
      status: "pending",
      theme: {
        backgroundImageUrl: "https://example.com/bg.png", // Has image
      },
    };
    const event = createEvent(longNameTheme);

    await (onThemeSubmitted as any)(event);

    // Should delete (reject)
    expect(deleteMock).toHaveBeenCalled();
    // Should NOT approve
    expect(setMock).not.toHaveBeenCalled();
  });

  it("should REJECT themes with excessively long background URLs", async () => {
    const longUrlTheme = {
      name: "Valid Name",
      status: "pending",
      theme: {
        backgroundImageUrl: "https://example.com/" + "a".repeat(MAX_URL_LENGTH),
      },
    };
    const event = createEvent(longUrlTheme);

    await (onThemeSubmitted as any)(event);

    // Should delete (reject)
    expect(deleteMock).toHaveBeenCalled();
    // Should NOT approve
    expect(setMock).not.toHaveBeenCalled();
  });
});
