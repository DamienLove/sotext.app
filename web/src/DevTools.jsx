import { useState } from 'react';
import { db, auth } from './firebase';
import { collection, doc, setDoc, serverTimestamp, addDoc, writeBatch } from "firebase/firestore";
import { signInWithEmailAndPassword, updateProfile } from "firebase/auth";

const DevTools = ({ isVisible, onClose }) => {
  const [status, setStatus] = useState('');
  const [userId, setUserId] = useState('test_user_123');

  const populateMockData = async () => {
    try {
      setStatus('Populating...');

      // Use current user if logged in, otherwise use test user ID
      const uid = auth.currentUser ? auth.currentUser.uid : userId;

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
      const lineId = "line_mock_1";
      const lineRef = doc(db, "users", uid, "lines", lineId);
      batch.set(lineRef, {
        label: "Pixel 7 Pro",
        phoneNumber: "+15550199",
        updatedAt: serverTimestamp()
      });

      // 3. Create Threads (Legacy & Line)
      const threads = [
        {
          id: "thread_1",
          address: "+15551234567",
          display_name: "Alice Wonderland",
          snippet: "Hey, are we still on for dinner?",
          date: Date.now() - 1000 * 60 * 5, // 5 mins ago
          unread: true,
          unreadCount: 2
        },
        {
          id: "thread_2",
          address: "28400",
          display_name: "Bank Alert",
          snippet: "Your verification code is 849201. Do not share this code.",
          date: Date.now() - 1000 * 60 * 60 * 2, // 2 hours ago
          unread: false
        }
      ];

      for (const t of threads) {
        // Legacy path
        const tRef = doc(db, "users", uid, "synced_threads", t.id);
        batch.set(tRef, t);

        // Line path
        const lineTRef = doc(db, "users", uid, "lines", lineId, "threads", t.id);
        batch.set(lineTRef, t);
      }

      await batch.commit();

      // 4. Create Messages for Thread 1
      const msgs = [
        { body: "Hello!", date: Date.now() - 1000 * 60 * 60, type: 2 }, // Sent
        { body: "Hi there!", date: Date.now() - 1000 * 60 * 55, type: 1 }, // Received
        { body: "Are we still on for dinner?", date: Date.now() - 1000 * 60 * 5, type: 1 } // Received
      ];

      // We can't batch subcollections easily with one batch object if we want to ensure atomicity across parents?
      // Actually batch works fine with any ref.

      // Using a new batch for messages to keep it simple
      const msgBatch = writeBatch(db);
      for (const m of msgs) {
         const msgId = `msg_${Date.now()}_${Math.random()}`;
         const mRef = doc(db, "users", uid, "synced_threads", "thread_1", "messages", msgId);
         const lineMRef = doc(db, "users", uid, "lines", lineId, "threads", "thread_1", "messages", msgId);
         msgBatch.set(mRef, m);
         msgBatch.set(lineMRef, m);
      }
      await msgBatch.commit();

      setStatus('Success! Reload or check Beacon tab.');
    } catch (e) {
      console.error(e);
      setStatus('Error: ' + e.message);
    }
  };

  const loginTestUser = async () => {
    // This requires a real account if connecting to real firebase,
    // or if we are mocking, we might need a custom auth provider.
    // For now, let's just log "Not implemented" as we can't create real users easily without backend.
    setStatus('Use the main login form with test credentials if available.');
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
        <button onClick={onClose} style={{background: 'transparent', border: 'none', color: '#999', cursor: 'pointer'}}>X</button>
      </div>

      <div style={{marginBottom: 10}}>
         <label style={{display:'block', fontSize: 12, marginBottom: 4}}>Target User ID (if not logged in)</label>
         <input
           value={userId}
           onChange={e => setUserId(e.target.value)}
           style={{width: '100%', background: '#222', border: '1px solid #444', color: '#fff', padding: 4}}
         />
      </div>

      <button
        onClick={populateMockData}
        style={{
          width: '100%',
          background: '#0ea5e9',
          border: 'none',
          padding: '8px',
          color: 'white',
          borderRadius: 4,
          cursor: 'pointer',
          marginBottom: 8
        }}
      >
        Populate Mock Data
      </button>

      {status && <div style={{fontSize: 12, color: '#aaa'}}>{status}</div>}
    </div>
  );
};

export default DevTools;
