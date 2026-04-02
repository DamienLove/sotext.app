#!/bin/bash
cat << 'DIFF1' > patch_spotify.diff
--- functions/src/spotify.ts
+++ functions/src/spotify.ts
@@ -1,6 +1,10 @@
 import {onCall, HttpsError} from "firebase-functions/v2/https";
 import {logger} from "firebase-functions";
+import {defineSecret} from "firebase-functions/params";

+const spotifyClientId = defineSecret("SPOTIFY_CLIENT_ID");
+const spotifyClientSecret = defineSecret("SPOTIFY_CLIENT_SECRET");
+
 const SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";

-export const getSpotifyAccessToken = onCall(async (request) => {
+export const getSpotifyAccessToken = onCall({
+  secrets: [spotifyClientId, spotifyClientSecret],
+}, async (request) => {
   // 1. Authenticate the user
   if (!request.auth) {
     throw new HttpsError("unauthenticated", "User must be authenticated.");
   }

-  // 2. Retrieve secrets (assumed to be in process.env for this environment)
-  // In production, use defineSecret() for better security, but process.env matches existing pattern (email.ts).
-  const clientId = process.env.SPOTIFY_CLIENT_ID;
-  const clientSecret = process.env.SPOTIFY_CLIENT_SECRET;
+  // 2. Retrieve secrets securely
+  const clientId = spotifyClientId.value();
+  const clientSecret = spotifyClientSecret.value();

   if (!clientId || !clientSecret) {
DIFF1

cat << 'DIFF2' > patch_email.diff
--- functions/src/email.ts
+++ functions/src/email.ts
@@ -3,20 +3,18 @@
 import * as nodemailer from "nodemailer";
 import {escapeHtml} from "./security";
+import {defineSecret} from "firebase-functions/params";

 // Ensure admin is initialized (handled in index.ts, but safe to check)
 if (admin.apps.length === 0) {
   admin.initializeApp();
 }
 const db = admin.firestore();

-const transporter = nodemailer.createTransport({
-  service: "gmail",
-  auth: {
-    user: process.env.EMAIL_USER,
-    pass: process.env.EMAIL_PASS,
-  },
-});
+const emailUser = defineSecret("EMAIL_USER");
+const emailPass = defineSecret("EMAIL_PASS");
+
+let transporter: nodemailer.Transporter | null = null;

-export const sendEmailNotification = functions.https.onCall(
+export const sendEmailNotification = functions
+    .runWith({secrets: [emailUser, emailPass]})
+    .https.onCall(
     async (data, context) => {
       if (!context.auth) {
@@ -25,2 +23,12 @@
       }
+
+      if (!transporter) {
+        transporter = nodemailer.createTransport({
+          service: "gmail",
+          auth: {
+            user: emailUser.value(),
+            pass: emailPass.value(),
+          },
+        });
+      }

@@ -94,3 +102,3 @@
       const mailOptions = {
-        from: `SoText <${process.env.EMAIL_USER}>`,
+        from: `SoText <${emailUser.value()}>`,
         to: email,
DIFF2

patch functions/src/spotify.ts < patch_spotify.diff
patch functions/src/email.ts < patch_email.diff
