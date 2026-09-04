/**
 * Tests for the scheduled-message sweep function (functions/src/scheduledMessages.ts). Firestore
 * and FCM are mocked - this is not an emulator test - focused on the sweep's own logic: dedup,
 * per-user isolation, and the "wake, never send" contract.
 */

const mockCollectionGroupGet = jest.fn();
const mockDevicesGet = jest.fn();
const mockSendMulticast = jest.fn();

function makeChainable(getImpl: jest.Mock) {
  const chain: any = {
    where: jest.fn().mockReturnThis(),
    limit: jest.fn().mockReturnThis(),
    get: getImpl,
  };
  return chain;
}

jest.mock('firebase-admin', () => {
  const collectionGroupChain = makeChainable(mockCollectionGroupGet);
  const devicesChain = makeChainable(mockDevicesGet);

  const firestoreInstance: any = {
    collectionGroup: jest.fn().mockReturnValue(collectionGroupChain),
    collection: jest.fn((name: string) => {
      if (name === 'devices') return devicesChain;
      // users/{uid}/scheduledMessages rate-limit path
      return {
        doc: jest.fn().mockReturnValue({
          collection: jest.fn().mockReturnValue(makeChainable(jest.fn())),
        }),
      };
    }),
  };

  const mFirestore: any = jest.fn().mockReturnValue(firestoreInstance);
  mFirestore.Timestamp = { fromMillis: jest.fn((ms: number) => ({ ms })) };

  return {
    apps: [],
    initializeApp: jest.fn(),
    firestore: mFirestore,
    messaging: jest.fn().mockReturnValue({ sendMulticast: mockSendMulticast }),
  };
});

jest.mock('firebase-functions', () => {
  class HttpsError extends Error {
    code: string;
    constructor(code: string, message: string) {
      super(message);
      this.code = code;
    }
  }
  return {
    pubsub: {
      schedule: jest.fn().mockReturnValue({
        onRun: (handler: () => Promise<unknown>) => handler,
      }),
    },
    https: {
      onCall: (handler: (data: unknown, context: unknown) => unknown) => handler,
      HttpsError,
    },
  };
});

import { sweepOverdueScheduledMessages, checkScheduledMessageRateLimit } from './scheduledMessages';

function fakeDoc(uid: string, id: string, data: Record<string, unknown>) {
  return {
    id,
    data: () => data,
    ref: { parent: { parent: { id: uid } } },
  };
}

describe('sweepOverdueScheduledMessages', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('does nothing when there are no overdue messages', async () => {
    mockCollectionGroupGet.mockResolvedValue({ empty: true, docs: [], forEach: (fn: any) => [].forEach(fn) });

    await (sweepOverdueScheduledMessages as unknown as () => Promise<null>)();

    expect(mockSendMulticast).not.toHaveBeenCalled();
  });

  it('sends one wake per user even with multiple overdue docs for that user', async () => {
    const docs = [
      fakeDoc('user-a', 'occ-1', { status: 'SCHEDULED', scheduledForUtcMillis: 1 }),
      fakeDoc('user-a', 'occ-2', { status: 'SCHEDULED', scheduledForUtcMillis: 2 }),
    ];
    mockCollectionGroupGet.mockResolvedValue({ empty: false, docs, forEach: (fn: any) => docs.forEach(fn) });
    mockDevicesGet.mockResolvedValue({
      forEach: (fn: any) => [{ data: () => ({ fcmToken: 'token-1' }) }].forEach(fn),
    });
    mockSendMulticast.mockResolvedValue({ successCount: 1 });

    await (sweepOverdueScheduledMessages as unknown as () => Promise<null>)();

    expect(mockSendMulticast).toHaveBeenCalledTimes(1);
    const payload = mockSendMulticast.mock.calls[0][0];
    expect(payload.data.type).toBe('SCHEDULED_MESSAGE_WAKE');
    expect(payload.tokens).toEqual(['token-1']);
  });

  it('skips users with no registered FCM tokens without throwing', async () => {
    const docs = [fakeDoc('user-b', 'occ-1', { status: 'SCHEDULED', scheduledForUtcMillis: 1 })];
    mockCollectionGroupGet.mockResolvedValue({ empty: false, docs, forEach: (fn: any) => docs.forEach(fn) });
    mockDevicesGet.mockResolvedValue({ forEach: (fn: any) => [].forEach(fn) });

    await (sweepOverdueScheduledMessages as unknown as () => Promise<null>)();

    expect(mockSendMulticast).not.toHaveBeenCalled();
  });

  it('one user failing to wake does not stop the sweep from processing others', async () => {
    const docs = [
      fakeDoc('user-fail', 'occ-1', { status: 'SCHEDULED', scheduledForUtcMillis: 1 }),
      fakeDoc('user-ok', 'occ-2', { status: 'SCHEDULED', scheduledForUtcMillis: 2 }),
    ];
    mockCollectionGroupGet.mockResolvedValue({ empty: false, docs, forEach: (fn: any) => docs.forEach(fn) });
    mockDevicesGet.mockResolvedValue({
      forEach: (fn: any) => [{ data: () => ({ fcmToken: 'token-x' }) }].forEach(fn),
    });
    mockSendMulticast
      .mockRejectedValueOnce(new Error('fcm down'))
      .mockResolvedValueOnce({ successCount: 1 });

    await expect(
        (sweepOverdueScheduledMessages as unknown as () => Promise<null>)(),
    ).resolves.not.toThrow();

    expect(mockSendMulticast).toHaveBeenCalledTimes(2);
  });
});

describe('checkScheduledMessageRateLimit (callable wrapper)', () => {
  it('rejects unauthenticated calls', async () => {
    const handler = checkScheduledMessageRateLimit as unknown as (data: unknown, context: unknown) => Promise<unknown>;
    await expect(handler({}, { auth: null })).rejects.toThrow('Auth required');
  });
});
