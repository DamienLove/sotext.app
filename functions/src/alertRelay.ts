import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

interface RelayRequest {
  message: string;
  severity: "emergency" | "non_urgent" | "check_in";
  recipients: Array<{
    phoneNumber?: string;
    pushToken?: string;
    email?: string;
  }>;
  senderId?: string;
  location?: { latitude: number; longitude: number; accuracyMeters?: number };
  metadata?: Record<string, string>;
}

export const alertRelay = functions.https.onCall(async (data: RelayRequest, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }

  if (!data || typeof data.message !== "string" || !Array.isArray(data.recipients)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid payload");
  }

  const cleanRecipients = data.recipients
    .filter((r) => !!(r.phoneNumber || r.pushToken || r.email))
    .map((r) => ({
      phoneNumber: r.phoneNumber,
      pushToken: r.pushToken,
      email: r.email,
    }));

  if (cleanRecipients.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "No recipients provided");
  }

  const relayDoc = {
    message: data.message,
    severity: data.severity ?? "non_urgent",
    recipients: cleanRecipients,
    senderId: data.senderId ?? context.auth.uid,
    location: data.location ?? null,
    metadata: data.metadata ?? {},
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "queued",
    platform: "kmp",
  };

  const docRef = await db.collection("relayAlerts").add(relayDoc);

  // Fan-out implementation (SMS/push/voice) will run from a separate worker/queue.
  return {
    status: "queued",
    relayId: docRef.id,
    estimatedFanOut: cleanRecipients.length,
  };
});
