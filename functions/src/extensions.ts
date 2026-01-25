import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";

// Simple extension submission function
export const submitExtension = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "User must be logged in to submit an extension.",
    );
  }

  const {manifest} = request.data;
  if (!manifest || !manifest.id || !manifest.name || !manifest.entry_point) {
    throw new HttpsError("invalid-argument", "Invalid manifest format.");
  }

  // Basic validation
  if (manifest.id.length > 50 || manifest.name.length > 50) {
    throw new HttpsError("invalid-argument", "ID or Name too long.");
  }

  // Sentinel: Helper to validate secure URLs
  const isValidUrl = (url: string): boolean => {
    try {
      const parsed = new URL(url);
      return parsed.protocol === "http:" || parsed.protocol === "https:";
    } catch {
      return false;
    }
  };

  // Sentinel: Sanitize manifest to prevent pollution and injection
  // Only allow specific fields to be saved.
  const cleanManifest = {
    id: String(manifest.id),
    name: String(manifest.name),
    description: manifest.description ?
        String(manifest.description).slice(0, 500) : "",
    entry_point: String(manifest.entry_point),
    iconUrl: manifest.iconUrl ? String(manifest.iconUrl) : null,
    version: manifest.version ? String(manifest.version) : "1.0.0",
    author: manifest.author ? String(manifest.author) : null,
  };

  // Sentinel: Validate URL format to prevent XSS (javascript:)
  if (!isValidUrl(cleanManifest.entry_point)) {
    throw new HttpsError(
        "invalid-argument",
        "entry_point must be a valid http/https URL.",
    );
  }

  if (cleanManifest.iconUrl && !isValidUrl(cleanManifest.iconUrl)) {
    throw new HttpsError(
        "invalid-argument",
        "iconUrl must be a valid http/https URL.",
    );
  }

  // Store in firestore
  const db = admin.firestore();
  const extensionRef = db.collection("extensions_store").doc(cleanManifest.id);

  // Check if exists and ownership
  const doc = await extensionRef.get();
  if (doc.exists) {
    const data = doc.data();
    if (data?.ownerUid !== request.auth.uid) {
      throw new HttpsError(
          "permission-denied",
          "You do not own this extension ID.",
      );
    }
  }

  await extensionRef.set({
    ...cleanManifest,
    ownerUid: request.auth.uid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "submitted", // Requires review
  }, {merge: true});

  return {success: true, message: "Extension submitted for review."};
});

// Auto-approve extensions from trusted developers (e.g. the owner)
export const onExtensionSubmitted = onDocumentWritten(
    "extensions_store/{extensionId}",
    async (event) => {
      if (!event.data) return; // Delete
      const after = event.data.after.data();
      if (!after || after.status !== "submitted") return;

      const ownerUid = after.ownerUid;
      if (!ownerUid) {
        logger.warn(
            `Extension ${event.params.extensionId} submitted without ownerUid.`,
        );
        return;
      }

      try {
        const user = await admin.auth().getUser(ownerUid);
        // Sentinel: Only auto-approve if the submitter is an Admin.
        // Prevents untrusted users from publishing extensions automatically.
        const isAdmin = user.customClaims?.admin === true;

        if (isAdmin) {
          await event.data.after.ref.update({status: "approved"});
          logger.info(
              `Auto-approved ${event.params.extensionId} for admin ` +
              `${ownerUid}.`,
          );
        } else {
          logger.info(
              `Extension ${event.params.extensionId} from ${ownerUid} ` +
            "requires manual review.",
          );
        }
      } catch (e) {
        logger.error(
            `Failed to verify owner for ${event.params.extensionId}`,
            e,
        );
      }
    },
);
