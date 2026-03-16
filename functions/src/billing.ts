import * as functions from "firebase-functions";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {logger} from "firebase-functions";
import * as admin from "firebase-admin";
import {google} from "googleapis";

const auth = new google.auth.GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});

/**
 * Helper: set/unset premium custom claim on the user.
 */
export async function setPremiumClaim(uid: string, enabled: boolean) {
  const existingClaims = (await admin.auth().getUser(uid)).customClaims || {};
  await admin.auth().setCustomUserClaims(uid, {
    ...existingClaims,
    premium: enabled || undefined,
  });
  await admin.auth().revokeRefreshTokens(uid);
}

/**
 * Callable: verify a Play subscription token against the Subscriptions v2 API.
 */
export const verifySubscription = onCall({maxInstances: 10}, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Auth required");
  const token = request.data?.purchaseToken as string | undefined;
  const packageName = request.data?.packageName as string | undefined;
  const productId = request.data?.productId as string | undefined;
  if (!token || !packageName || !productId) {
    throw new HttpsError("invalid-argument", "purchaseToken, packageName, productId required");
  }

  const androidPublisher = google.androidpublisher({version: "v3", auth});
  try {
    const {data} = await androidPublisher.purchases.subscriptionsv2.get({
      token,
      packageName,
      subscriptionId: productId,
    } as any);

    const status = data?.subscriptionState || "SUBSCRIPTION_STATE_UNSPECIFIED";
    const expiryMillis = Number(data?.lineItems?.[0]?.expiryTime || 0);

    const active = status === "SUBSCRIPTION_STATE_ACTIVE" ||
                   status === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const updateData: any = {
      premiumSubscriptionStatus: status,
      premiumSubscriptionExpiry: expiryMillis,
      subscriptionPurchaseToken: token,
      tierBeforePremium: admin.firestore.FieldValue.delete(),
    };
    if (active) {
      updateData.remoteWebAccessEnabled = true;
    }

    await admin.firestore().collection("users").doc(uid)
        .set(updateData, {merge: true});
    await setPremiumClaim(uid, active);
    return {status, expiryMillis, active};
  } catch (err) {
    logger.error("verifySubscription failed", err);
    throw new HttpsError("internal", "Verification failed");
  }
});

/**
 * Callable: get current premium status and refresh custom token.
 */
export const getPremiumStatus = onCall({maxInstances: 20}, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Auth required");
  const doc = await admin.firestore().collection("users").doc(uid).get();
  const data = doc.data() || {};
  const status = data.premiumSubscriptionStatus || "UNKNOWN";
  const expiry = data.premiumSubscriptionExpiry || 0;
  const hasClaim = (await admin.auth().getUser(uid)).customClaims?.premium === true;
  return {status, expiry, hasClaim};
});

/**
 * HTTPS endpoint for Play RTDN (Real-Time Developer Notifications).
 * Expects body of Pub/Sub push with message.data base64 payload.
 */
export const handlePlayStoreRTDN = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method not allowed");
    return;
  }

  try {
    const message = req.body?.message;
    if (!message?.data) {
      res.status(400).send("Bad payload");
      return;
    }
    const decoded = JSON.parse(Buffer.from(message.data, "base64").toString());
    const {packageName, subscriptionId, purchaseToken} = decoded?.subscriptionNotification || {};
    logger.info("RTDN", decoded);

    if (!purchaseToken || !packageName || !subscriptionId) {
      res.status(200).send("ignored");
      return;
    }

    const androidPublisher = google.androidpublisher({version: "v3", auth});
    const {data} = await androidPublisher.purchases.subscriptionsv2.get({
      token: purchaseToken,
      packageName,
      subscriptionId,
    } as any);
    const status = data.subscriptionState || "UNKNOWN";
    const expiryMillis = Number(data?.lineItems?.[0]?.expiryTime || 0);

    const snapshot = await admin.firestore()
        .collection("users")
        .where("subscriptionPurchaseToken", "==", purchaseToken)
        .limit(1)
        .get();

    if (!snapshot.empty) {
      const doc = snapshot.docs[0];
      const uid = doc.id;
      const active = status === "SUBSCRIPTION_STATE_ACTIVE" ||
                     status === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const updateData: any = {
        premiumSubscriptionStatus: status,
        premiumSubscriptionExpiry: expiryMillis,
      };
      if (active) {
        updateData.remoteWebAccessEnabled = true;
      }

      await doc.ref.set(updateData, {merge: true});
      await setPremiumClaim(uid, active);
    }

    res.status(200).send("ok");
  } catch (err) {
    logger.error("RTDN handler error", err);
    res.status(500).send("error");
  }
});
