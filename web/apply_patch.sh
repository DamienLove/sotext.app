#!/bin/bash
cat << 'PATCH_EOF' > web/App.patch
--- src/App.jsx
+++ src/App.jsx
@@ -2526,6 +2526,7 @@
   const [email, setEmail] = useState('');
   const [password, setPassword] = useState('');
   const [showPassword, setShowPassword] = useState(false);
+  const passwordInputRef = useRef(null);
   const [authError, setAuthError] = useState('');
   const [isSavingProfile, setIsSavingProfile] = useState(false);
   const [activePanel, setActivePanel] = useState('inbox');
@@ -4314,8 +4315,13 @@
               </label>
               <div className="login-field">
                 <label htmlFor="login-password">Password</label>
-                <div className="password-input-wrapper">
+                <div
+                  className="password-input-wrapper"
+                  onClick={() => passwordInputRef.current?.focus()}
+                  role="presentation"
+                >
                   <input
+                    ref={passwordInputRef}
                     id="login-password"
                     className="login-input"
                     type={showPassword ? "text" : "password"}
@@ -4326,8 +4332,12 @@
                   <button
                     type="button"
                     className="password-toggle-btn"
-                    onClick={() => setShowPassword(!showPassword)}
-                    aria-label={showPassword ? "Hide password" : "Show password"}
+                    onClick={(e) => {
+                      e.stopPropagation();
+                      setShowPassword(!showPassword);
+                    }}
+                    aria-label={showPassword ? "Hide password" : "Show password"}
+                    aria-pressed={showPassword}
                     title={showPassword ? "Hide password" : "Show password"}
                   >
                     {showPassword ? (
PATCH_EOF

cd web && patch -p0 < App.patch
