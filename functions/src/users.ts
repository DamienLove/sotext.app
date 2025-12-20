import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

export const findUser = functions.https.onCall(async (data, context) => {
    // Auth check
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { phoneNumber, email } = data;
    if (!phoneNumber && !email) {
        throw new functions.https.HttpsError("invalid-argument", "Must provide phoneNumber or email.");
    }

    try {
        let userRecord: admin.auth.UserRecord | null = null;

        if (phoneNumber) {
            try {
                userRecord = await admin.auth().getUserByPhoneNumber(phoneNumber);
            } catch (e: any) {
                // If not found or invalid format, try adding '+' if missing
                if ((e.code === 'auth/user-not-found' || e.code === 'auth/invalid-phone-number') && !phoneNumber.startsWith('+')) {
                     try {
                        userRecord = await admin.auth().getUserByPhoneNumber(`+${phoneNumber}`);
                     } catch (inner) {
                        // Still not found via phone
                     }
                }
            }
        }

        // If not found by phone (or phone not provided), try email
        if (!userRecord && email) {
            try {
                userRecord = await admin.auth().getUserByEmail(email);
            } catch (e) {
                // Not found via email either
            }
        }

        if (!userRecord) {
            return { found: false };
        }

        const uid = userRecord.uid;
        // Fetch deviceId from Firestore users/{uid}
        const userDoc = await db.collection("users").doc(uid).get();
        if (!userDoc.exists) {
            return { found: false, message: "User profile not found." };
        }

        const userData = userDoc.data();
        const deviceId = userData?.deviceId;
        const displayName = userData?.ownerName || userRecord.displayName || "";
        const avatarUrl = userData?.avatarUrl || userRecord.photoURL || "";

        if (!deviceId) {
             return { found: false, message: "User has no active device." };
        }

        return {
            found: true,
            uid: uid,
            deviceId: deviceId,
            displayName: displayName,
            phoneNumber: userRecord.phoneNumber,
            email: userRecord.email,
            avatarUrl: avatarUrl
        };

    } catch (error: any) {
        console.error("Error finding user", error);
        return { found: false };
    }
});
