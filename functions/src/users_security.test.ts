/* eslint-disable @typescript-eslint/no-explicit-any */
import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import {findUserHandler} from "./users";

// Mocks
jest.mock("firebase-admin", () => {
  const firestoreMock = {
    collection: jest.fn(),
  };
  // Timestamp mock
  const Timestamp = {
    now: jest.fn(() => ({toMillis: () => 1600000000000})), // Fixed time
    fromMillis: jest.fn((ms) => ({toMillis: () => ms})),
  };

  return {
    apps: [{}],
    initializeApp: jest.fn(),
    firestore: Object.assign(jest.fn(() => firestoreMock), {Timestamp}),
    auth: jest.fn(() => ({
      getUserByPhoneNumber: jest.fn(),
      getUserByEmail: jest.fn(),
    })),
  };
});

describe("User Security - Rate Limiting", () => {
  let collectionMock: any;
  let docMock: any;
  let subCollectionMock: any;
  let whereMock: any;
  let countMock: any;
  let getMock: any;
  let addMock: any;

  beforeEach(() => {
    jest.clearAllMocks();

    addMock = jest.fn();
    getMock = jest.fn();
    countMock = jest.fn().mockReturnValue({get: getMock});
    whereMock = jest.fn().mockReturnValue({count: countMock});
    subCollectionMock = jest.fn().mockReturnValue({
      where: whereMock,
      add: addMock,
    });
    docMock = jest.fn().mockReturnValue({collection: subCollectionMock});
    collectionMock = jest.fn().mockReturnValue({doc: docMock});

    // Inject mocks into the existing db instance used by users.ts
    const db = admin.firestore();
    db.collection = collectionMock;
  });

  const context = {
    auth: {uid: "attacker-uid", token: {}},
  } as unknown as functions.https.CallableContext;

  const data = {phoneNumber: "+15550000000"};

  test("should allow lookup if under rate limit", async () => {
    // Mock count to return 29
    getMock.mockResolvedValue({data: () => ({count: 29})});

    // Mock user lookup to fail gracefully
    const authMock = admin.auth() as any;
    authMock.getUserByPhoneNumber
        .mockRejectedValue({code: "auth/user-not-found"});
    authMock.getUserByEmail
        .mockRejectedValue({code: "auth/user-not-found"});

    await findUserHandler(data, context);

    expect(collectionMock).toHaveBeenCalledWith("rate_limits");
    expect(docMock).toHaveBeenCalledWith("attacker-uid");
    expect(subCollectionMock).toHaveBeenCalledWith("user_lookups");
    expect(countMock).toHaveBeenCalled();
    expect(addMock).toHaveBeenCalled(); // Should log the attempt
  });

  test("should deny lookup if rate limit reached", async () => {
    // Mock count to return 30
    getMock.mockResolvedValue({data: () => ({count: 30})});

    await expect(findUserHandler(data, context))
        .rejects.toThrow("Hourly user lookup limit reached.");

    expect(addMock).not.toHaveBeenCalled();
  });
});
