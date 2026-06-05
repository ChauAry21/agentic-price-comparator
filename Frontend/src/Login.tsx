import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg'; 

const Login = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [twoFactorCode, setTwoFactorCode] = useState(''); // State for 2FA token
  const [is2FARequired, setIs2FARequired] = useState(false); // Flag to show 2FA input screen
  const [error, setError] = useState('');

  const handleLogin = async () => {
    // 1. Validation Checks
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError('Please enter a valid email address.');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setError('');

    try {
      // 2. Phase 1: Verify Username and Password with your Spring Backend
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      const data = await response.json();

      // 3. Check if the backend says "Hey, password is good, but I need 2FA!"
      if (data.status === 'STEP_2_REQUIRED') {
        setIs2FARequired(true); 
        return;
      }

      // If no 2FA was required (or fallback), handle direct token storage
      if (data.accessToken) {
        localStorage.setItem('token', data.accessToken);
        console.log("Login successful!");
        navigate('/dashboard');
      }

    } catch (err) {
      setError('Invalid credentials or server connection failed.');
    }
  };

  const handleVerify2FA = async () => {
    if (twoFactorCode.length !== 6) {
      setError('Please enter a valid 6-digit code.');
      return;
    }

    try {
      // 4. Phase 2: Submit the 6-Digit Code along with the email context
      const response = await fetch('http://localhost:8080/api/auth/verify-2fa', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code: twoFactorCode })
      });

      const data = await response.json();

      if (response.ok && data.accessToken) {
        // Safe and authenticated! Store the OAuth2 access token
        localStorage.setItem('token', data.accessToken);
        setError('');
        console.log("2FA Verification successful!");
        navigate('/dashboard');
      } else {
        setError('Invalid verification code. Please try again.');
      }
    } catch (err) {
      setError('2FA verification failed. Please check your network.');
    }
  };

  return (
    <div className="auth-container">
      <img src={logoIcon} alt="PricePilot AI Logo" className="auth-logo" />
      
      {!is2FARequired ? (
        // Standard Username & Password View
        <>
          <h2>Welcome Back</h2>
          <p style={{ color: '#666', marginTop: '-10px', marginBottom: '20px' }}>Sign in to continue</p>
          
          {error && <p style={{ color: 'red', fontSize: '14px', marginBottom: '10px' }}>{error}</p>}
          
          <button className="back-button" onClick={() => navigate('/')}>← Back</button>
          
          <input 
            type="email" 
            placeholder="Email" 
            value={email} 
            onChange={(e) => setEmail(e.target.value)} 
          />
          <input 
            type="password" 
            placeholder="Password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
          />
          <button onClick={handleLogin}>Submit</button>
        </>
      ) : (
        // Dynamically Swapped 2FA Verification View
        <>
          <h2>Two-Factor Verification</h2>
          <p style={{ color: '#666', marginTop: '-10px', marginBottom: '20px' }}>
            Enter the 6-digit code from your authenticator app.
          </p>
          
          {error && <p style={{ color: 'red', fontSize: '14px', marginBottom: '10px' }}>{error}</p>}
          
          <input 
            type="text" 
            maxLength={6}
            placeholder="000000" 
            value={twoFactorCode} 
            onChange={(e) => setTwoFactorCode(e.target.value.replace(/\D/g, ''))} // only numbers
            style={{ textAlign: 'center', fontSize: '20px', letterSpacing: '4px' }}
          />
          <button onClick={handleVerify2FA}>Verify & Sign In</button>
          <button className="back-button" onClick={() => setIs2FARequired(false)} style={{ marginTop: '10px' }}>
            ← Back to Login
          </button>
        </>
      )}
    </div>
  );
};

export default Login;