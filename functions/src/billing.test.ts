
import { verifySubscription } from './billing';
import * as admin from 'firebase-admin';
import { google } from 'googleapis';

// --- Mocks Setup ---

jest.mock('firebase-admin', () => {
  const mSet = jest.fn().mockResolvedValue({});
  const mDoc = jest.fn().mockReturnValue({ set: mSet });
  const mCollection = jest.fn().mockReturnValue({ doc: mDoc });

  // Mock firestore function
  const mFirestore: any = jest.fn().mockReturnValue({ collection: mCollection });
  // Mock firestore.FieldValue
  mFirestore.FieldValue = { delete: jest.fn().mockReturnValue('DELETE_SENTINEL') };

  const mSetCustomUserClaims = jest.fn().mockResolvedValue({});
  const mRevokeRefreshTokens = jest.fn().mockResolvedValue({});
  const mGetUser = jest.fn().mockResolvedValue({ customClaims: {} });
  const mAuth = jest.fn().mockReturnValue({
    getUser: mGetUser,
    setCustomUserClaims: mSetCustomUserClaims,
    revokeRefreshTokens: mRevokeRefreshTokens,
  });

  return {
    firestore: mFirestore,
    auth: mAuth,
    initializeApp: jest.fn(),
  };
});

jest.mock('googleapis', () => {
  return {
    google: {
      auth: { GoogleAuth: jest.fn() },
      androidpublisher: jest.fn().mockReturnValue({
        purchases: {
          subscriptionsv2: {
            get: jest.fn()
          }
        }
      })
    }
  };
});

jest.mock('firebase-functions/v2/https', () => ({
  onCall: (opts: any, handler: any) => handler,
  HttpsError: class extends Error { constructor(code: string, message: string) { super(message); } }
}));

const mockLoggerError = jest.fn();
jest.mock('firebase-functions', () => ({
  logger: { error: (...args: any[]) => mockLoggerError(...args), info: jest.fn() },
  https: {
      onRequest: (handler: any) => handler
  }
}));

describe('verifySubscription', () => {
  let firestoreMock: any;
  let authMock: any;

  beforeEach(() => {
    jest.clearAllMocks();
    mockLoggerError.mockClear();
    firestoreMock = (admin.firestore as unknown as jest.Mock);
    authMock = (admin.auth as unknown as jest.Mock)();

    // Wire up the google mock manually since we want to control the 'get' method
    const mockGet = jest.fn();
    (google.androidpublisher as unknown as jest.Mock).mockReturnValue({
        purchases: {
            subscriptionsv2: {
                get: mockGet
            }
        }
    });
  });

  it('should set remoteWebAccessEnabled = true when subscription is active', async () => {
    const mockGet = (google.androidpublisher as unknown as jest.Mock)().purchases.subscriptionsv2.get;
    mockGet.mockResolvedValue({
      data: {
        subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
        lineItems: [{ expiryTime: '1735689600000' }]
      }
    });

    const request = {
      auth: { uid: 'test-user-1' },
      data: {
        purchaseToken: 'token123',
        packageName: 'com.example.app',
        productId: 'premium_sub'
      }
    };

    await (verifySubscription as any)(request);

    // Retrieve mocks via the mocked module (which is firestoreMock itself now)
    const collectionMock = firestoreMock().collection;
    const docMock = collectionMock().doc;
    const setMock = docMock().set;

    expect(collectionMock).toHaveBeenCalledWith('users');
    expect(docMock).toHaveBeenCalledWith('test-user-1');
    expect(setMock).toHaveBeenCalledWith(
      expect.objectContaining({
        premiumSubscriptionStatus: 'SUBSCRIPTION_STATE_ACTIVE',
        remoteWebAccessEnabled: true
      }),
      { merge: true }
    );

    expect(authMock.setCustomUserClaims).toHaveBeenCalledWith('test-user-1', expect.objectContaining({ premium: true }));
  });

  it('should NOT set remoteWebAccessEnabled when subscription is expired', async () => {
    const mockGet = (google.androidpublisher as unknown as jest.Mock)().purchases.subscriptionsv2.get;
    mockGet.mockResolvedValue({
      data: {
        subscriptionState: 'SUBSCRIPTION_STATE_EXPIRED',
        lineItems: [{ expiryTime: '1700000000000' }]
      }
    });

    const request = {
      auth: { uid: 'test-user-2' },
      data: {
        purchaseToken: 'token456',
        packageName: 'com.example.app',
        productId: 'premium_sub'
      }
    };

    await (verifySubscription as any)(request);

    const collectionMock = firestoreMock().collection;
    const docMock = collectionMock().doc;
    const setMock = docMock().set;

    expect(setMock).toHaveBeenCalledWith(
      expect.not.objectContaining({
        remoteWebAccessEnabled: true
      }),
      { merge: true }
    );
    expect(authMock.setCustomUserClaims).toHaveBeenCalledWith('test-user-2', expect.objectContaining({ premium: undefined }));
  });
});
