import { initializeApp, getApps } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getFunctions } from "firebase/functions";
import { getStorage } from "firebase/storage";
import { getAnalytics } from "firebase/analytics";

const firebaseConfig = {
  apiKey: "AIzaSyDT-dDiNWjpd-5Ek2qxMFIQznxuuW9QvXw",
  authDomain: "sotextapp.firebaseapp.com",
  projectId: "sotextapp",
  storageBucket: "sotextapp.firebasestorage.app",
  messagingSenderId: "861460679274",
  appId: "1:861460679274:web:f6179ec058b1c0bf55d813",
  measurementId: "G-0NZRE9QGS0"
};

const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

export const auth = getAuth(app);
export const db = getFirestore(app);
export const functions = getFunctions(app, "us-central1");
export const storage = getStorage(app);
export const analytics = getAnalytics(app);
