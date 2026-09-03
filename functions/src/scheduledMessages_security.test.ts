/**
 * Rate-limit boundary enforcement for scheduled-message cloud sync, following the `*_security.test.ts`
 * convention used elsewhere in this package (e.g. ai_security.test.ts) for input/abuse-surface
 * suites. checkScheduleRateLimit is modeled directly on alertRelay.ts's checkAlertRateLimit.
 */

let mockCount = 0;

jest.mock('firebase-admin', () => {
  const countChain = {
    count: jest.fn().mockReturnThis(),
    get: jest.fn(() => Promise.resolve({ data: () => ({ count: mockCount }) })),
  };
  const whereChain: any = {
    where: jest.fn().mockReturnValue(countChain),
  };
  const firestoreInstance: any = {
    collection: jest.fn().mockReturnValue({
      doc: jest.fn().mockReturnValue({
        collection: jest.fn().mockReturnValue(whereChain),
      }),
    }),
    collectionGroup: jest.fn(),
  };
  const mFirestore: any = jest.fn().mockReturnValue(firestoreInstance);
  return {
    apps: [],
    initializeApp: jest.fn(),
    firestore: mFirestore,
    messaging: jest.fn(),
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
    pubsub: { schedule: jest.fn().mockReturnValue({ onRun: (h: unknown) => h }) },
    https: { onCall: (h: unknown) => h, HttpsError },
  };
});

import { checkScheduleRateLimit } from './scheduledMessages';

describe('checkScheduleRateLimit', () => {
  beforeEach(() => {
    mockCount = 0;
  });

  it('allows creation well under the daily cap', async () => {
    mockCount = 5;
    await expect(checkScheduleRateLimit('user-1')).resolves.toBeUndefined();
  });

  it('allows creation exactly one below the cap', async () => {
    mockCount = 199;
    await expect(checkScheduleRateLimit('user-1')).resolves.toBeUndefined();
  });

  it('rejects at exactly the cap boundary', async () => {
    mockCount = 200;
    await expect(checkScheduleRateLimit('user-1')).rejects.toThrow('Daily scheduled-message limit reached.');
  });

  it('rejects above the cap', async () => {
    mockCount = 500;
    await expect(checkScheduleRateLimit('user-1')).rejects.toThrow('Daily scheduled-message limit reached.');
  });

  it('the rejection carries the resource-exhausted error code', async () => {
    mockCount = 200;
    try {
      await checkScheduleRateLimit('user-1');
      fail('expected checkScheduleRateLimit to throw');
    } catch (error: any) {
      expect(error.code).toBe('resource-exhausted');
    }
  });
});
