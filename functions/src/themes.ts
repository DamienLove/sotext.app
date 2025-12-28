import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

/**
 * Cloud Function that monitors theme submissions and automatically approves
 * themes without images (which don't require moderation).
 *
 * Security: This function acts as the trusted backend that can write to
 * themes_public. Clients cannot write directly to themes_public per Firestore rules.
 *
 * Workflow:
 * 1. User submits theme to themes_submissions (via web app)
 * 2. This function triggers on new submission
 * 3. If theme has no images (hasImages = false), auto-approve and copy to themes_public
 * 4. If theme has images (hasImages = true), leave in themes_submissions for manual review
 */
export const onThemeSubmitted = onDocumentCreated(
  "themes_submissions/{themeId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      console.log("No data associated with the event");
      return;
    }

    const themeData = snapshot.data();
    const themeId = event.params.themeId;

    console.log(`Theme submitted: ${themeId}`, {
      hasImages: themeData.hasImages,
      status: themeData.status,
      ownerUid: themeData.ownerUid,
    });

    // Only auto-approve themes without images
    if (!themeData.hasImages && themeData.status === "pending") {
      try {
        const db = admin.firestore();

        // Prepare the approved theme data
        const approvedThemeData = {
          ...themeData,
          status: "approved",
          approvedAt: admin.firestore.FieldValue.serverTimestamp(),
          approvedBy: "auto", // Indicates automatic approval
        };

        // Copy to themes_public collection
        await db.collection("themes_public").doc(themeId).set(approvedThemeData);

        // Update the submission status
        await snapshot.ref.update({
          status: "approved",
          approvedAt: admin.firestore.FieldValue.serverTimestamp(),
          approvedBy: "auto",
        });

        console.log(`Theme ${themeId} auto-approved and published`);
      } catch (error) {
        console.error(`Error auto-approving theme ${themeId}:`, error);
        // Update submission with error status
        await snapshot.ref.update({
          status: "error",
          error: error instanceof Error ? error.message : "Unknown error",
        });
      }
    } else if (themeData.hasImages) {
      console.log(`Theme ${themeId} requires manual review (has images)`);
      // Theme with images stays in pending status for manual review
    }
  }
);
