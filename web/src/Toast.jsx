import { useEffect } from 'react';

const getToastType = (msg) => {
  if (!msg) return 'default';
  const lower = msg.toLowerCase();
  if (lower.includes('fail') || lower.includes('error') || lower.includes('missing')) return 'error';
  if (lower.includes('success') || lower.includes('saved') || lower.includes('updated') || lower.includes('sent') || lower.includes('published') || lower.includes('imported') || lower.includes('cleared')) return 'success';
  return 'default';
};

const Toast = ({ message, onDismiss }) => {
  const type = getToastType(message);
  const typeClass = type === 'default' ? '' : type;

  useEffect(() => {
    if (type === 'success') {
      const timer = setTimeout(onDismiss, 4000);
      return () => clearTimeout(timer);
    }
  }, [message, type, onDismiss]);

  if (!message) return null;

  return (
    <div className={`toast ${typeClass}`} role="status" aria-live="polite">
      <span>{message}</span>
      <button
        onClick={onDismiss}
        aria-label="Dismiss"
        style={{
          background: 'transparent',
          border: 'none',
          color: 'currentColor',
          marginLeft: '12px',
          cursor: 'pointer',
          padding: '4px',
          display: 'flex',
          alignItems: 'center',
          opacity: 0.8
        }}
        onMouseOver={(e) => e.currentTarget.style.opacity = 1}
        onMouseOut={(e) => e.currentTarget.style.opacity = 0.8}
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>
  );
};

export default Toast;
