import { useState, useEffect, memo } from 'react';
import { auth, db } from './firebase';
import { GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged } from "firebase/auth";
import { collection, query, orderBy, onSnapshot } from "firebase/firestore";
import './App.css';

// ⚡ Bolt: Optimized ThreadItem with memo to prevent unnecessary re-renders of the entire list
// when only the selection state changes.
const ThreadItem = memo(({ thread, isActive, onSelect }) => (
  <button
    role="button"
    tabIndex={0}
    className={`thread-item ${isActive ? 'active' : ''}`}
    onClick={() => onSelect(thread)}
    onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        onSelect(thread);
      }
    }}
    aria-current={isActive ? 'true' : undefined}
    aria-label={`Select conversation with ${thread.address}`}
  >
    <div className="thread-name">{thread.address}</div>
    <div className="thread-snippet">{thread.snippet}</div>
  </button>
));

ThreadItem.displayName = 'ThreadItem';

function App() {
  const [user, setUser] = useState(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [threads, setThreads] = useState([]);
  const [selectedThread, setSelectedThread] = useState(null);
  const [messages, setMessages] = useState([]);
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

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleLogin = async () => {
    setIsLoggingIn(true);
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
    } catch (error) {
      console.error("Login failed", error);
      setIsLoggingIn(false);
    }
  };

  const handleLogout = async () => {
    await signOut(auth);
    setSelectedThread(null);
  };

  if (!user) {
    return (
      <div className="container login-container">
        <h1>PulseLink Web</h1>
        <p>Login to access your messages</p>
        <button
          onClick={handleLogin}
          disabled={isLoggingIn}
          aria-busy={isLoggingIn}
          style={isLoggingIn ? { opacity: 0.7, cursor: 'not-allowed' } : {}}
        >
          {isLoggingIn ? 'Signing in...' : 'Sign in with Google'}
        </button>
      </div>
    );
  }

  return (
    <div className="app-container">
      <div className="sidebar">
        <div className="sidebar-header">
          <h2>Messages</h2>
          <button onClick={handleLogout} className="logout-btn">Logout</button>
        </div>
        <div className="thread-list">
          {threads.map(thread => (
            <ThreadItem
              key={thread.id}
              thread={thread}
              isActive={selectedThread?.id === thread.id}
              onSelect={setSelectedThread}
            />
          ))}
        </div>
      </div>
      <div className="main-content">
        {selectedThread ? (
          <>
            <div className="chat-header">
              <h3>{selectedThread.address}</h3>
            </div>
            <div className="messages-list">
              {messages.map(msg => (
                <div key={msg.id} className={`message ${msg.type === 1 ? 'received' : 'sent'}`}>
                  <div className="message-bubble">
                    {msg.body}
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
            <span role="img" aria-label="chat bubble" style={{ fontSize: '2rem', marginRight: '8px' }}>💬</span>
            Select a thread to view messages
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
