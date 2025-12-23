import { useState, useEffect, useRef, memo } from 'react';
import { auth, db } from './firebase';
import {
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  sendPasswordResetEmail
} from "firebase/auth";
import { collection, query, orderBy, onSnapshot, addDoc, serverTimestamp } from "firebase/firestore";
import './App.css';
import logo from './assets/pulselink-pro-logo.png';

// Bolt: Optimized ThreadItem with memo to prevent unnecessary re-renders of the entire list
// when only the selection state changes.
const ThreadItem = memo(({ thread, isActive, onSelect, showPreviews }) => (
  <button
    className={`thread-item ${isActive ? 'active' : ''}`}
    onClick={() => onSelect(thread)}
    aria-current={isActive ? 'true' : undefined}
    aria-label={`Select conversation with ${thread.address}`}
  >
    <div className="thread-name">{thread.address}</div>
    <div className="thread-snippet">{showPreviews ? thread.snippet : '••••••'}</div>
  </button>
));

ThreadItem.displayName = 'ThreadItem';

function App() {
  const [user, setUser] = useState(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [threads, setThreads] = useState([]);
  const [selectedThread, setSelectedThread] = useState(null);
  const [messages, setMessages] = useState([]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [composeAddress, setComposeAddress] = useState('');
  const [composeBody, setComposeBody] = useState('');
  const [sendStatus, setSendStatus] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [activePanel, setActivePanel] = useState('inbox');
  const [settingsStatus, setSettingsStatus] = useState('');
  const [showPreviews, setShowPreviews] = useState(true);
  const [autoScroll, setAutoScroll] = useState(true);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setIsLoggingIn(false);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (user) {
      // Listen to threads
      // Assuming structure: users/{uid}/synced_threads/{threadId}
      const threadsRef = collection(db, "users", user.uid, "synced_threads");
      const q = query(threadsRef, orderBy("date", "desc"));
      const unsubscribe = onSnapshot(q, (snapshot) => {
        const threadsData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setThreads(threadsData);
      });
      return () => unsubscribe();
    } else {
      setThreads([]);
    }
  }, [user]);

  useEffect(() => {
    if (user && selectedThread) {
      // Listen to messages
      const messagesRef = collection(db, "users", user.uid, "synced_threads", selectedThread.id, "messages");
      const q = query(messagesRef, orderBy("date", "asc"));
      const unsubscribe = onSnapshot(q, (snapshot) => {
        const messagesData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setMessages(messagesData);
      });
      return () => unsubscribe();
    } else {
      setMessages([]);
    }
  }, [user, selectedThread]);

  useEffect(() => {
    if (selectedThread?.address) {
      setComposeAddress(selectedThread.address);
    }
    setComposeBody('');
    setSendStatus('');
  }, [selectedThread?.address]);

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    if (autoScroll) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, autoScroll]);

  const handleLogin = async () => {
    setIsLoggingIn(true);
    setAuthError('');
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
    } catch (error) {
      console.error("Login failed", error);
      setAuthError(error?.message ?? "Google sign-in failed.");
      setIsLoggingIn(false);
    }
  };

  const handleEmailAuth = async (mode) => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail || !password) {
      setAuthError("Enter email and password.");
      return;
    }
    setIsLoggingIn(true);
    setAuthError('');
    try {
      if (mode === 'signup') {
        await createUserWithEmailAndPassword(auth, trimmedEmail, password);
      } else {
        await signInWithEmailAndPassword(auth, trimmedEmail, password);
      }
    } catch (error) {
      console.error("Email auth failed", error);
      setAuthError(error?.message ?? "Email sign-in failed.");
      setIsLoggingIn(false);
    }
  };

  const handlePasswordReset = async () => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setAuthError("Enter your email to reset your password.");
      return;
    }
    setIsLoggingIn(true);
    setAuthError('');
    try {
      await sendPasswordResetEmail(auth, trimmedEmail);
      setAuthError("Password reset email sent.");
    } catch (error) {
      console.error("Password reset failed", error);
      setAuthError(error?.message ?? "Password reset failed.");
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handlePasswordResetForUser = async () => {
    if (!user?.email) {
      setSettingsStatus("No email address on file.");
      return;
    }
    setSettingsStatus("Sending password reset email...");
    try {
      await sendPasswordResetEmail(auth, user.email);
      setSettingsStatus(`Password reset sent to ${user.email}.`);
    } catch (error) {
      console.error("Password reset failed", error);
      setSettingsStatus(error?.message ?? "Password reset failed.");
    }
  };

  const handleLogout = async () => {
    await signOut(auth);
    setSelectedThread(null);
    setComposeAddress('');
    setComposeBody('');
    setSendStatus('');
    setActivePanel('inbox');
  };

  const handleSendMessage = async () => {
    if (!user) return;
    const address = composeAddress.trim();
    const body = composeBody.trim();
    if (!address || !body) {
      setSendStatus("Add a phone number and message.");
      return;
    }
    setIsSending(true);
    setSendStatus('');
    try {
      await addDoc(collection(db, "users", user.uid, "outbox"), {
        address,
        body,
        createdAt: serverTimestamp(),
        source: "web"
      });
      setComposeBody('');
      setSendStatus("Queued for sending from your device.");
    } catch (error) {
      console.error("Send failed", error);
      setSendStatus("Send failed. Try again.");
    } finally {
      setIsSending(false);
    }
  };

  if (!user) {
    return (
      <div className="container login-container">
        <div className="login-card">
          <img src={logo} alt="PulseLink Pro" className="brand-logo" />
          <h1>PulseLink Web</h1>
          <p>Login to access your messages</p>
          <div className="login-form">
            <label className="login-field">
              Email
              <input
                className="login-input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                autoComplete="email"
              />
            </label>
            <label className="login-field">
              Password
              <input
                className="login-input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="password"
                autoComplete="current-password"
              />
            </label>
            {authError && <div className="auth-error">{authError}</div>}
            <div className="login-actions">
              <button
                onClick={() => handleEmailAuth('signin')}
                disabled={isLoggingIn}
                aria-busy={isLoggingIn}
                className="primary-btn"
              >
                {isLoggingIn ? 'Signing in...' : 'Sign in'}
              </button>
              <button
                onClick={() => handleEmailAuth('signup')}
                disabled={isLoggingIn}
                className="secondary-btn"
              >
                Create account
              </button>
            </div>
            <button
              type="button"
              className="link-button"
              onClick={handlePasswordReset}
              disabled={isLoggingIn}
            >
              Forgot password?
            </button>
            <div className="login-divider">or</div>
            <button
              onClick={handleLogin}
              disabled={isLoggingIn}
              aria-busy={isLoggingIn}
              className="primary-btn"
            >
              {isLoggingIn ? 'Signing in...' : 'Sign in with Google'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container">
      <div className="sidebar">
        <div className="sidebar-header">
          <div className="sidebar-brand">
            <img src={logo} alt="PulseLink Pro" className="brand-logo small" />
            <div>
              <div className="brand-title">PulseLink Pro</div>
              <div className="brand-subtitle">Web access</div>
            </div>
          </div>
          <div className="sidebar-actions">
            {activePanel === 'inbox' && (
              <button
                onClick={() => {
                  setSelectedThread(null);
                  setComposeAddress('');
                  setComposeBody('');
                  setSendStatus('');
                }}
                className="secondary-btn"
              >
                New
              </button>
            )}
            <button onClick={handleLogout} className="ghost-btn">Logout</button>
          </div>
        </div>
        <div className="sidebar-nav">
          <button
            className={`nav-item ${activePanel === 'inbox' ? 'active' : ''}`}
            onClick={() => setActivePanel('inbox')}
          >
            Inbox
          </button>
          <button
            className={`nav-item ${activePanel === 'settings' ? 'active' : ''}`}
            onClick={() => setActivePanel('settings')}
          >
            Settings
          </button>
        </div>
        <div className="thread-list">
          {threads.map(thread => (
            <ThreadItem
              key={thread.id}
              thread={thread}
              isActive={selectedThread?.id === thread.id}
              onSelect={setSelectedThread}
              showPreviews={showPreviews}
            />
          ))}
        </div>
      </div>
      <div className="main-content">
        {activePanel === 'settings' ? (
          <div className="settings-panel">
            <div className="settings-header">
              <h3>Settings</h3>
              <p>Manage your account and web access preferences.</p>
            </div>
            <div className="settings-grid">
              <div className="settings-card">
                <h4>Account</h4>
                <div className="settings-row">
                  <span className="settings-label">Signed in as</span>
                  <span className="settings-value">{user.email || 'Unknown'}</span>
                </div>
                <div className="settings-row">
                  <span className="settings-label">User ID</span>
                  <span className="settings-value mono">{user.uid}</span>
                </div>
                <button className="secondary-btn" type="button" onClick={handlePasswordResetForUser}>
                  Send password reset email
                </button>
                {settingsStatus && <div className="settings-status">{settingsStatus}</div>}
              </div>
              <div className="settings-card">
                <h4>Web preferences</h4>
                <label className="settings-toggle">
                  <input
                    type="checkbox"
                    checked={showPreviews}
                    onChange={(e) => setShowPreviews(e.target.checked)}
                  />
                  Show message previews
                </label>
                <label className="settings-toggle">
                  <input
                    type="checkbox"
                    checked={autoScroll}
                    onChange={(e) => setAutoScroll(e.target.checked)}
                  />
                  Auto-scroll to latest message
                </label>
                <p className="settings-note">
                  Preferences apply to this browser only.
                </p>
              </div>
              <div className="settings-card">
                <h4>Device sync</h4>
                <p className="settings-note">
                  Messages arrive from your phone when it’s online and signed in.
                  Keep PulseLink running on your device for the fastest sync.
                </p>
                <button className="primary-btn" type="button" onClick={() => setActivePanel('inbox')}>
                  Back to inbox
                </button>
              </div>
            </div>
          </div>
        ) : (
          <>
            {selectedThread ? (
              <>
                <div className="chat-header">
                  <h3>{selectedThread.address}</h3>
                </div>
                <div className="messages-list">
                  {messages.map(msg => (
                    <div key={msg.id} className={`message ${msg.type === 1 ? 'received' : 'sent'}`}>
                      <div className="message-bubble">
                        {showPreviews ? msg.body : '••••••'}
                      </div>
                      <div className="message-time">
                        {new Date(msg.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </div>
                  ))}
                  <div ref={messagesEndRef} />
                </div>
              </>
            ) : (
              <div className="empty-state">
                <img src={logo} alt="PulseLink Pro" className="empty-logo" />
                <div>Select a thread or start a new message</div>
              </div>
            )}
            <div className="composer">
              <div className="composer-row">
                <label className="composer-label" htmlFor="compose-address">To</label>
                <input
                  id="compose-address"
                  className="composer-input"
                  type="tel"
                  placeholder="Phone number"
                  value={composeAddress}
                  onChange={(e) => setComposeAddress(e.target.value)}
                />
              </div>
              <div className="composer-row composer-actions">
                <textarea
                  className="composer-textarea"
                  placeholder="Type a message..."
                  value={composeBody}
                  onChange={(e) => setComposeBody(e.target.value)}
                />
                <button
                  onClick={handleSendMessage}
                  disabled={isSending || isLoggingIn}
                  className="primary-btn"
                >
                  {isSending ? "Sending..." : "Send"}
                </button>
              </div>
              {sendStatus && <div className="compose-status">{sendStatus}</div>}
              <div className="compose-hint">
                Messages are sent from your phone when it's online and signed in.
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default App;
