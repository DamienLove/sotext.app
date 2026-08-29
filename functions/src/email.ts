import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as nodemailer from "nodemailer";
import {escapeHtml} from "./security";
import {defineSecret} from "firebase-functions/params";

// Ensure admin is initialized (handled in index.ts, but safe to check)
if (admin.apps.length === 0) {
  admin.initializeApp();
}
const db = admin.firestore();

const emailUser = defineSecret("EMAIL_USER");
const emailPass = defineSecret("EMAIL_PASS");

export const sendEmailNotification = functions
    .runWith({secrets: [emailUser, emailPass]})
    .https.onCall(async (data, context) => {
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
          user: emailUser.value(),
          pass: emailPass.value(),
        },
      });

      if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated", "User must be authenticated");
      }

      const {email, messageType, payload} = data;

      // Sentinel: Validate email format
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!email || !emailRegex.test(email)) {
        throw new functions.https.HttpsError(
            "invalid-argument", "Valid email is required");
      }

      // Sentinel: Fetch verified user identity to prevent spoofing
      const uid = context.auth.uid;
      let verifiedName = "SoText User";

      try {
        // Try to get name from Firestore profile first (often more up to date)
        const userDoc = await db.collection("users").doc(uid).get();
        const userData = userDoc.data();

        if (userData?.ownerName) {
          verifiedName = userData.ownerName;
        } else {
          // Fallback to Auth profile
          const userRecord = await admin.auth().getUser(uid);
          verifiedName = userRecord.displayName ||
        userRecord.phoneNumber ||
        userRecord.email ||
        "SoText User";
        }
      } catch (error) {
        console.error("Failed to verify sender identity", error);
        // Proceed with default name rather than failing,
        // but do NOT trust client senderName
      }

      // Sentinel: Sanitize verified name to prevent HTML injection
      const safeSenderName = escapeHtml(verifiedName);

      let subject = "SoText Notification";
      let text = `You have a new message from ${verifiedName}`;
      let html = "<p>You have a new message from " +
        `<strong>${safeSenderName}</strong></p>`;

      if (messageType === "LinkRequest") {
        subject = "SoText Request";
        const code = payload?.code || "unknown";
        // Sentinel: Encode URI component for link and escape for HTML
        const deepLinkBase = "https://sotext.app/link?code=";
        const deepLink = `${deepLinkBase}${encodeURIComponent(code)}`;
        const safeCode = escapeHtml(code);

        text = `${verifiedName} wants to link with you on SoText.\n\n` +
               `Link Code: ${code}\n\n` +
               `Tap here to accept: ${deepLink}`;
        html = `
            <p><strong>${safeSenderName}</strong> ` +
            "wants to link with you on SoText.</p>" +
            `<p>Link Code: <strong>${safeCode}</strong></p>` +
            `<p><a href="${deepLink}">Tap here to accept</a></p>`;
      } else if (messageType === "ManualMessage") {
        const body = payload?.body || "Alert";
        subject = `SoText Alert from ${verifiedName}`;
        text = `${verifiedName}: ${body}`;

        // Sentinel: Escape body content
        const safeBody = escapeHtml(body);
        html = `<p><strong>${safeSenderName}</strong>: ${safeBody}</p>`;
      }

      const mailOptions = {
        from: `SoText <${emailUser.value()}>`,
        to: email,
        subject: subject,
        text: text,
        html: html,
      };

      try {
        await transporter.sendMail(mailOptions);
        return {success: true};
      } catch (error) {
        console.error("Error sending email", error);
        throw new functions.https.HttpsError(
            "internal", "Failed to send email");
      }
    });
