import { useState } from 'react';
import { db, auth } from './firebase';
import { doc, serverTimestamp, writeBatch } from "firebase/firestore";

/**
 * Time constants for date calculations
 */
const MS_PER_MINUTE = 1000 * 60;
const MS_PER_HOUR = MS_PER_MINUTE * 60;

/**
 * Constants for mock data to avoid magic numbers/strings
 */
const MOCK_DATA = {
  LINE_ID: "line_mock_1",
  PHONE_NUMBER: "+15550199",
  THREAD_ID: "thread_1",
  THREAD_ADDRESS: "+15551234567",
  THREAD_NAME: "Alice Wonderland",
  THREAD_SNIPPET: "Hey, are we still on for dinner?",
  VERIFICATION_THREAD_ID: "thread_2",
  VERIFICATION_ADDRESS: "28400",
  VERIFICATION_NAME: "Bank Alert",
  VERIFICATION_SNIPPET: "Your verification code is 849201. Do not share this code."
};

/**
 * DevTools Component
 *
 * DEBUG/DEVELOPMENT ONLY.
 * Allows developers to inject mock data into the connected Firebase project
 * to simulate premium user state and synced messages without needing an Android device.
 *
 * SECURITY WARNING: This component allows writing to the database.
 * It ensures it only runs in development mode, but should never be exposed in production builds.
 * REQUIRES AUTHENTICATION: Users must be logged in to populate mock data.
 */
const DevTools = ({ isVisible, onClose }) => {
  const [status, setStatus] = useState('');

  const populateMockData = async () => {
    // SECURITY: Double-check we are in DEV mode
    if (!import.meta.env.DEV) {
      setStatus('Error: Operations restricted to development environment.');
      return;
    }

    // SECURITY: Require authentication - do not allow arbitrary user ID injection
    if (!auth.currentUser) {
      setStatus('Error: You must be logged in to populate mock data.');
      return;
    }

    try {
      setStatus('Populating...');

      // Use authenticated user's ID
      const uid = auth.currentUser.uid;

      const batch = writeBatch(db);

      // 1. Set Premium Status
      const userRef = doc(db, "users", uid);
      batch.set(userRef, {
        subscriptionStatus: 'premium',
        remoteWebAccessEnabled: true,
        ownerName: 'Test User',
        createdAt: serverTimestamp()
      }, { merge: true });

      // 2. Create a Mock Line
      const lineRef = doc(db, "users", uid, "lines", MOCK_DATA.LINE_ID);
      batch.set(lineRef, {
        label: "Pixel 7 Pro",
        phoneNumber: MOCK_DATA.PHONE_NUMBER,
        updatedAt: serverTimestamp()
      });

      // 3. Create Threads (Legacy & Line)
      const threads = [
        {
          id: MOCK_DATA.THREAD_ID,
          address: MOCK_DATA.THREAD_ADDRESS,
          display_name: MOCK_DATA.THREAD_NAME,
          snippet: MOCK_DATA.THREAD_SNIPPET,
          // Using Date.now() for message/thread timestamps to match client usage
          date: Date.now() - (5 * MS_PER_MINUTE), // 5 mins ago
          unread: true,
          unreadCount: 2
        },
        {
          id: MOCK_DATA.VERIFICATION_THREAD_ID,
          address: MOCK_DATA.VERIFICATION_ADDRESS,
          display_name: MOCK_DATA.VERIFICATION_NAME,
          snippet: MOCK_DATA.VERIFICATION_SNIPPET,
          date: Date.now() - (2 * MS_PER_HOUR), // 2 hours ago
          unread: false
        }
      ];

      for (const t of threads) {
        // Legacy path
        const tRef = doc(db, "users", uid, "synced_threads", t.id);
        batch.set(tRef, t);

        // Line path
        const lineTRef = doc(db, "users", uid, "lines", MOCK_DATA.LINE_ID, "threads", t.id);
        batch.set(lineTRef, t);
      }

      await batch.commit();

      // 4. Create Messages for Thread 1
      const msgs = [
        { body: "Hello!", date: Date.now() - MS_PER_HOUR, type: 2 }, // Sent
        { body: "Hi there!", date: Date.now() - (55 * MS_PER_MINUTE), type: 1 }, // Received
        { body: "Are we still on for dinner?", date: Date.now() - (5 * MS_PER_MINUTE), type: 1 } // Received
      ];

      const msgBatch = writeBatch(db);
      let messageCounter = 0;
      for (const m of msgs) {
         // Use slice() instead of deprecated substr() and ensure unique IDs
         const msgId = `msg_${Date.now() + messageCounter}_${Math.random().toString(36).slice(2, 11)}`;
         messageCounter++;
         const mRef = doc(db, "users", uid, "synced_threads", MOCK_DATA.THREAD_ID, "messages", msgId);
         const lineMRef = doc(db, "users", uid, "lines", MOCK_DATA.LINE_ID, "threads", MOCK_DATA.THREAD_ID, "messages", msgId);
         msgBatch.set(mRef, m);
         msgBatch.set(lineMRef, m);
      }
      await msgBatch.commit();

      setStatus('Success! Click the "Beacon" tab to view messages.');
    } catch (e) {
      console.error('Failed to populate mock data:', e);
      setStatus('Error: ' + (e.code || 'Unknown') + ' - ' + e.message);
    }
  };

  if (!isVisible) return null;

  return (
    <div style={{
      position: 'fixed',
      bottom: 20,
      right: 20,
      background: '#111',
      border: '1px solid #333',
      padding: 16,
      borderRadius: 8,
      zIndex: 9999,
      color: '#fff',
      maxWidth: 300
    }}>
      <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 10}}>
        <strong>Dev Tools</strong>
        <button
          onClick={onClose}
          aria-label="Close DevTools"
          style={{background: 'transparent', border: 'none', color: '#999', cursor: 'pointer'}}
        >
          X
        </button>
      </div>

      <div style={{marginBottom: 10, fontSize: 12, color: '#aaa'}}>
        {auth.currentUser
          ? `Logged in as: ${auth.currentUser.email || auth.currentUser.uid}`
          : 'Please log in to use DevTools'}
      </div>

      <button
        onClick={populateMockData}
        disabled={!auth.currentUser}
        style={{
          width: '100%',
          background: auth.currentUser ? '#0ea5e9' : '#555',
          border: 'none',
          padding: '8px',
          color: 'white',
          borderRadius: 4,
          cursor: auth.currentUser ? 'pointer' : 'not-allowed',
          marginBottom: 8,
          opacity: auth.currentUser ? 1 : 0.5
        }}
      >
        Populate Mock Data
      </button>

      {status && <div style={{fontSize: 12, color: '#aaa'}} role="alert" aria-live="polite">{status}</div>}
    </div>
  );
};

export default DevTools;
