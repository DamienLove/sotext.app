import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

// Sentinel: Shared rate limit check, modeled directly on alertRelay.ts's checkAlertRateLimit.
// Scheduled-message creation itself is entirely local/on-device (Room, no Firestore write) for
// free users; this only bounds how many *cloud-synced* schedules (Premium, remoteWebAccessEnabled)
// a single user can create per day, guarding the sweep function's own read volume against abuse.
export async function checkScheduleRateLimit(uid: string): Promise<void> {
  const ONE_DAY_MS = 24 * 60 * 60 * 1000;
  const recentSnapshot = await db
      .collection("users")
      .doc(uid)
      .collection("scheduledMessages")
      .where(
          "createdAt",
          ">",
          Date.now() - ONE_DAY_MS,
      )
      .count()
      .get();

  if (recentSnapshot.data().count >= 200) {
    throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily scheduled-message limit reached.",
    );
  }
}

/**
 * Callable wrapper so a client can pre-flight-check the rate limit before creating a batch of
 * cloud-synced schedules (e.g. a long recurring series) without waiting for a write to fail.
 */
export const checkScheduledMessageRateLimit = functions.https.onCall(
    async (_data, context) => {
      if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Auth required");
      }
      await checkScheduleRateLimit(context.auth.uid);
      return {ok: true};
    },
);

/**
 * Fallback dispatcher - NOT a sender. Cloud Functions have no SIM/carrier access; sending always
 * happens on the owning phone via the local exact alarm (primary path) or
 * ScheduledMessageSweepWorker (local fallback). This is the *second* fallback layer: if a phone's
 * own alarm and periodic sweep both miss a due send (app force-killed, doze abuse, or the process
 * simply isn't running), this sweep finds the overdue Firestore doc and sends a high-priority FCM
 * wake to the owning device(s), mirroring onOutboxCreated's wake mechanism in messaging.ts. This
 * is the first scheduled (cron) function in this codebase; uses the v1-compat pubsub.schedule API
 * for consistency with the rest of functions/src, which is entirely v1-compat style.
 */
export const sweepOverdueScheduledMessages = functions.pubsub
    .schedule("every 5 minutes")
    .onRun(async () => {
      const now = Date.now();
      const overdue = await db
          .collectionGroup("scheduledMessages")
          .where("status", "==", "SCHEDULED")
          .where("scheduledForUtcMillis", "<=", now)
          .limit(200)
          .get();

      if (overdue.empty) {
        return null;
      }

      // Dedup: multiple overdue docs for the same user only need one wake.
      const uids = new Set<string>();
      overdue.forEach((doc) => {
        const uid = doc.ref.parent.parent?.id;
        if (uid) uids.add(uid);
      });

      for (const uid of uids) {
        try {
          const devicesQuery = await db.collection("devices").where("uid", "==", uid).get();
          const tokens: string[] = [];
          devicesQuery.forEach((doc) => {
            const token = doc.data().fcmToken;
            if (token) tokens.push(token);
          });

          if (tokens.length === 0) {
            console.log(`No FCM tokens found for user ${uid}, cannot wake for overdue scheduled messages`);
            continue;
          }

          const response = await admin.messaging().sendMulticast({
            data: {
              type: "SCHEDULED_MESSAGE_WAKE",
              timestamp: String(now),
            },
            tokens,
            android: {priority: "high" as const},
          });
          console.log(`Sent scheduled-message wake to ${response.successCount}/${tokens.length} devices for user ${uid}`);
        } catch (error) {
          // Per-user try/catch: one user's failure (bad token, transient error) must not abort
          // the sweep for every other overdue user in this pass.
          console.error(`Failed to wake devices for user ${uid}`, error);
        }
      }

      return null;
    });
