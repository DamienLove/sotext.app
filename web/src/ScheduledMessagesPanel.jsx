import { useState, useEffect, useMemo } from 'react';
import {
  collection,
  query,
  orderBy,
  onSnapshot,
  doc,
  updateDoc
} from 'firebase/firestore';

const FREQUENCIES = [
  { value: '', label: "Doesn't repeat" },
  { value: 'DAILY', label: 'Daily' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'YEARLY', label: 'Yearly' }
];

function toLocalDateTimeInputValue(millis) {
  const d = new Date(millis);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function dateSectionLabel(dateKey) {
  const today = new Date();
  const todayKey = today.toDateString();
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const tomorrowKey = tomorrow.toDateString();
  if (dateKey === todayKey) return 'Today';
  if (dateKey === tomorrowKey) return 'Tomorrow';
  return dateKey;
}

/**
 * The web-portal half of Scheduled Messages sync (Premium, remoteWebAccessEnabled) - mirrors
 * the Android "Scheduled" hub. Reads/writes `users/{uid}/scheduledMessages/{occurrenceKey}`
 * directly; the phone's ScheduledMessageSyncService applies the same last-write-wins-on-updatedAt
 * policy this panel does, and is the only thing that ever actually sends anything - this panel
 * only edits Firestore state. Split out of App.jsx (already ~5700 lines) rather than growing it
 * further, per the same self-contained-panel pattern as Toast/CommandPalette/DevTools.
 */
export default function ScheduledMessagesPanel({ db, user }) {
  const [scheduledMessages, setScheduledMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editBody, setEditBody] = useState('');
  const [editWhen, setEditWhen] = useState('');
  const [editFrequency, setEditFrequency] = useState('');

  useEffect(() => {
    if (!user) {
      setScheduledMessages([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    const q = query(
      collection(db, 'users', user.uid, 'scheduledMessages'),
      orderBy('scheduledForUtcMillis', 'asc')
    );
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const rows = snapshot.docs
        .map((d) => ({ id: d.id, ...d.data() }))
        .filter((row) => row.status === 'SCHEDULED' || row.status === 'FAILED' || row.status === 'PROCESSING');
      setScheduledMessages(rows);
      setLoading(false);
    }, (error) => {
      console.error('Failed to load scheduled messages', error);
      setLoading(false);
    });
    return () => unsubscribe();
  }, [db, user]);

  const grouped = useMemo(() => {
    const map = new Map();
    scheduledMessages.forEach((row) => {
      const key = new Date(row.scheduledForUtcMillis).toDateString();
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(row);
    });
    return Array.from(map.entries());
  }, [scheduledMessages]);

  const docRef = (id) => doc(db, 'users', user.uid, 'scheduledMessages', id);

  const handleCancel = async (id) => {
    await updateDoc(docRef(id), { status: 'CANCELLED', updatedAt: Date.now() });
  };

  const handleSendNow = async (id) => {
    // Sentinel the phone's ScheduledMessageSyncService special-cases to bypass the alarm and
    // dispatch immediately, rather than reinterpreting scheduledForUtcMillis (which would race
    // with the real alarm and complicate the status semantics for no benefit).
    await updateDoc(docRef(id), { status: 'SEND_NOW_REQUESTED', updatedAt: Date.now() });
  };

  const startEdit = (row) => {
    setEditingId(row.id);
    setEditBody(row.body || '');
    setEditWhen(toLocalDateTimeInputValue(row.scheduledForUtcMillis));
    setEditFrequency(row.recurrenceRule?.frequency || '');
  };

  const cancelEdit = () => setEditingId(null);

  const saveEdit = async (row) => {
    const scheduledForUtcMillis = new Date(editWhen).getTime();
    if (Number.isNaN(scheduledForUtcMillis) || !editBody.trim()) return;
    const recurrenceRule = editFrequency
      ? {
          frequency: editFrequency,
          interval: 1,
          daysOfWeek: [],
          dayOfMonth: editFrequency === 'MONTHLY' ? new Date(scheduledForUtcMillis).getDate() : null,
          endDateUtcMillis: null,
          occurrenceCount: null
        }
      : null;
    await updateDoc(docRef(row.id), {
      body: editBody.trim(),
      scheduledForUtcMillis,
      recurrenceRule,
      updatedAt: Date.now()
    });
    setEditingId(null);
  };

  if (!user) {
    return (
      <div className="scheduled-panel">
        <div className="panel-header">
          <h3>Scheduled</h3>
          <p>Messages you&apos;ve scheduled to send later.</p>
        </div>
        <div className="settings-card">
          <h4>Sign in to view scheduled messages</h4>
          <p className="settings-note">Scheduled-message sync is a Premium, sign-in-required feature, matching remote SMS access.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="scheduled-panel">
      <div className="panel-header">
        <h3>Scheduled</h3>
        <p>Messages scheduled to send later from this account, grouped by date.</p>
      </div>

      {loading && <p className="settings-note">Loading…</p>}

      {!loading && scheduledMessages.length === 0 && (
        <div className="settings-card">
          <h4>No scheduled messages</h4>
          <p className="settings-note">
            Schedule a message on your phone (long-press Send, or the clock icon in the composer) - it&apos;ll
            appear here once cloud sync is on.
          </p>
        </div>
      )}

      {grouped.map(([dateKey, rows]) => (
        <div key={dateKey} className="scheduled-date-group">
          <h4 className="scheduled-date-header">{dateSectionLabel(dateKey)}</h4>
          {rows.map((row) => (
            <div key={row.id} className="settings-card scheduled-message-card">
              {editingId === row.id ? (
                <>
                  <textarea
                    className="settings-search-input"
                    style={{ width: '100%', minHeight: 60, marginBottom: 8 }}
                    value={editBody}
                    onChange={(e) => setEditBody(e.target.value)}
                  />
                  <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
                    <input
                      type="datetime-local"
                      className="settings-search-input"
                      value={editWhen}
                      onChange={(e) => setEditWhen(e.target.value)}
                    />
                    <select
                      className="settings-search-input"
                      value={editFrequency}
                      onChange={(e) => setEditFrequency(e.target.value)}
                    >
                      {FREQUENCIES.map((f) => (
                        <option key={f.value} value={f.value}>{f.label}</option>
                      ))}
                    </select>
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button className="primary-btn" onClick={() => saveEdit(row)}>Save</button>
                    <button className="ghost-btn" onClick={cancelEdit}>Cancel</button>
                  </div>
                </>
              ) : (
                <>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                    <div>
                      <strong>{row.address}</strong>
                      <p style={{ margin: '4px 0' }}>{row.body}</p>
                      <p className="settings-note" style={{ margin: 0 }}>
                        {row.status === 'FAILED' ? 'Failed - ' : ''}
                        {new Date(row.scheduledForUtcMillis).toLocaleString()}
                        {row.recurrenceRule ? ` · repeats ${row.recurrenceRule.frequency.toLowerCase()}` : ''}
                      </p>
                      {row.status === 'FAILED' && row.lastError && (
                        <p className="settings-note" style={{ color: 'var(--danger, #e05252)' }}>{row.lastError}</p>
                      )}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
                    <button className="ghost-btn" onClick={() => startEdit(row)}>Edit</button>
                    <button className="ghost-btn" onClick={() => handleSendNow(row.id)}>Send now</button>
                    <button className="ghost-btn" onClick={() => handleCancel(row.id)}>Cancel</button>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
