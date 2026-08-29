import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

// Sentinel: Rate limit check to prevent user enumeration
async function checkUserLookupRateLimit(uid: string): Promise<void> {
  const MAX_LOOKUPS_PER_HOUR = 30;
  const now = admin.firestore.Timestamp.now();
  const oneHourAgo = admin.firestore.Timestamp.fromMillis(
      now.toMillis() - 60 * 60 * 1000,
  );

  const lookupsRef = db.collection("rate_limits")
      .doc(uid)
      .collection("user_lookups");

  const recentLookups = await lookupsRef
      .where("timestamp", ">", oneHourAgo)
      .count()
      .get();

  if (recentLookups.data().count >= MAX_LOOKUPS_PER_HOUR) {
    throw new functions.https.HttpsError(
        "resource-exhausted",
        "Hourly user lookup limit reached.",
    );
  }

  // Log this lookup attempt
  await lookupsRef.add({
    timestamp: now,
  });
}

export const findUserHandler = async (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data: any,
    context: functions.https.CallableContext,
) => {
  // Auth check
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be logged in.",
    );
  }

  // Sentinel: Enforce rate limit
  await checkUserLookupRateLimit(context.auth.uid);

  const {phoneNumber, email} = data;
  if (!phoneNumber && !email) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Must provide phoneNumber or email.",
    );
  }

  try {
    let userRecord: admin.auth.UserRecord | null = null;

    if (phoneNumber) {
      try {
        userRecord = await admin.auth().getUserByPhoneNumber(phoneNumber);
      } catch (error) {
        const e = error as { code?: string };
        // If not found or invalid format, try adding '+' if missing
        if (
          (e.code === "auth/user-not-found" ||
            e.code === "auth/invalid-phone-number") &&
          !phoneNumber.startsWith("+")
        ) {
          try {
            userRecord = await admin.auth()
                .getUserByPhoneNumber(`+${phoneNumber}`);
          } catch {
            // Still not found via phone
          }
        }
      }
    }

    // If not found by phone (or phone not provided), try email
    if (!userRecord && email) {
      try {
        userRecord = await admin.auth().getUserByEmail(email);
      } catch {
        // Not found via email either
      }
    }

    if (!userRecord) {
      return {found: false};
    }

    const uid = userRecord.uid;
    // Fetch deviceId from Firestore users/{uid}
    const userDoc = await db.collection("users").doc(uid).get();
    if (!userDoc.exists) {
      return {found: false, message: "User profile not found."};
    }

    const userData = userDoc.data();
    const deviceId = userData?.deviceId;
    const displayName = userData?.ownerName || userRecord.displayName || "";
    const avatarUrl = userData?.avatarUrl || userRecord.photoURL || "";

    if (!deviceId) {
      return {found: false, message: "User has no active device."};
    }

    return {
      found: true,
      uid: uid,
      deviceId: deviceId,
      displayName: displayName,
      // Sentinel: Removed PII (email/phone) to prevent enumeration leaks
      avatarUrl: avatarUrl,
    };
  } catch (error) {
    console.error("Error finding user", error);
    return {found: false};
  }
};

export const findUser = functions.https.onCall(findUserHandler);

export const deleteAccount = functions.https.onCall(async (_data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be logged in.",
    );
  }

  const uid = context.auth.uid;
  const email = (context.auth.token.email as string | undefined) ?? null;
  const db = admin.firestore();

  try {
    const userDocRef = db.collection("users").doc(uid);

    // Delete user profile + subcollections
    await db.recursiveDelete(userDocRef);

    // Delete device registrations and beta agreements based on auth
    const deviceQuery = await db.collection("devices")
        .where("uid", "==", uid)
        .get();

    const deviceDeletes: FirebaseFirestore.WriteBatch[] = [];
    let batch = db.batch();
    let ops = 0;
    const queueDelete = (ref: FirebaseFirestore.DocumentReference) => {
      batch.delete(ref);
      ops += 1;
      if (ops >= 450) {
        deviceDeletes.push(batch);
        batch = db.batch();
        ops = 0;
      }
    };

    deviceQuery.forEach((doc) => {
      // Delete the device mapping
      queueDelete(doc.ref);
      // Delete the corresponding betaAgreement
      queueDelete(db.collection("betaAgreements").doc(doc.id));
    });

    if (ops > 0) deviceDeletes.push(batch);
    for (const b of deviceDeletes) {
      await b.commit();
    }

    // Delete link invites sent by or targeted to the user
    const inviteDeletes: Promise<FirebaseFirestore.WriteResult>[] = [];
    const inviteCollection = db.collection("linkEmailInvites");
    const senderInvites = await inviteCollection
        .where("senderUid", "==", uid)
        .get();
    senderInvites.forEach((doc) => inviteDeletes.push(doc.ref.delete()));
    if (email) {
      const targetInvites = await inviteCollection
          .where("targetEmailLowercase", "==", email.toLowerCase())
          .get();
      targetInvites.forEach((doc) => inviteDeletes.push(doc.ref.delete()));
    }
    await Promise.all(inviteDeletes);

    // Delete link docs containing this uid
    const linksSnap = await db.collection("links")
        .where("uids", "array-contains", uid)
        .get();
    const linkDeletes = linksSnap.docs.map((doc) => doc.ref.delete());
    await Promise.all(linkDeletes);

    // Finally, delete the Auth user
    await admin.auth().deleteUser(uid);

    return {success: true};
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error("Account deletion failed", error);
    throw new functions.https.HttpsError(
        "internal",
        "Unable to delete account. Please contact support.",
    );
  }
});

export const onUserUpdated = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const userId = context.params.userId;

      const wasWebEnabled = before.remoteWebAccessEnabled === true;
      const isWebEnabled = after.remoteWebAccessEnabled === true;
      const webAccessChanged = !wasWebEnabled && isWebEnabled;

      const wasPremium = before.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_ACTIVE" ||
                         before.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
      const isPremium = after.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_ACTIVE" ||
                        after.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
      const premiumChanged = !wasPremium && isPremium;

      const syncRequested = after.syncRequestedAt && after.syncRequestedAt !== before.syncRequestedAt;

      if (webAccessChanged || premiumChanged || syncRequested) {
        console.log(`User ${userId} settings/premium updated, sending SYNC_REQUEST`);

        const devicesQuery = await db.collection("devices")
            .where("uid", "==", userId)
            .get();

        if (devicesQuery.empty) return;

        const tokens: string[] = [];
        devicesQuery.forEach((doc) => {
          const data = doc.data();
          if (data.fcmToken) {
            tokens.push(data.fcmToken);
          }
        });

        if (tokens.length > 0) {
          const payload = {
            data: {
              type: "SYNC_REQUEST",
              userId: userId,
              reason: webAccessChanged ? "web_access_enabled" : (premiumChanged ? "premium_upgrade" : "manual_request"),
            },
            tokens: tokens,
            android: {
              priority: "high" as const,
            },
          };

          try {
            const response = await admin.messaging().sendMulticast(payload);
            console.log(`Sent SYNC_REQUEST to ${response.successCount} devices for user ${userId}`);
          } catch (error) {
            console.error("Error sending SYNC_REQUEST", error);
          }
        }
      }
    });
