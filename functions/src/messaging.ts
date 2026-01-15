import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const onMessageCreated = functions.firestore
    .document("linkChannels/{channelId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
      const message = snapshot.data();
      const receiverId = message.receiverId;
      const senderId = message.senderId;
      const type = message.type || "manual";

      // Ensure newly created messages carry an initial SENT status for clients.
      await snapshot.ref.set({status: "SENT"}, {merge: true});

      if (!receiverId) {
        console.log("No receiverId found in message");
        return;
      }

      const db = admin.firestore();
      // Get receiver's FCM token from devices collection
      const deviceDoc = await db.collection("devices").doc(receiverId).get();

      if (!deviceDoc.exists) {
        console.log(`No device document found for receiver ${receiverId}`);
        return;
      }

      const fcmToken = deviceDoc.data()?.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token found for receiver ${receiverId}`);
        return;
      }

      // FCM data payload - all values must be strings
      const dataPayload: {[key: string]: string} = {
        type: type,
        senderId: senderId,
        timestamp: String(message.timestamp || Date.now()),
        messageId: context.params.messageId,
        channelId: context.params.channelId,
      };

      // Add optional fields if present
      if (message.body) {
        // Sentinel: Truncate body to prevent FCM payload size limit errors
        // (4KB limit). If the body is too large, notifications will fail.
        const maxBodyLength = 1000;
        dataPayload.body = message.body.length > maxBodyLength ?
            message.body.substring(0, maxBodyLength) + "..." :
            message.body;
      }
      if (message.linkCode) dataPayload.linkCode = message.linkCode;

      const payload = {
        data: dataPayload,
        token: fcmToken,
      };

      try {
        await admin.messaging().send(payload);
        console.log(`FCM notification sent to ${receiverId}`);
      } catch (error) {
        console.error("Error sending FCM notification", error);
      }
    });
