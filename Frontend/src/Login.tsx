import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';

const API_URL = import.meta.env.VITE_API_URL;

const Login = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [step, setStep] = useState<'email' | 'otp'>('email');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSendOtp() {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) { setError('Please enter a valid email address.'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_URL}/api/auth/send-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      if (!res.ok) throw new Error();
      setStep('otp');
    } catch {
      setError('Failed to send code. Try again.');
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyOtp() {
    if (code.length !== 6) { setError('Please enter the 6-digit code.'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_URL}/api/auth/verify-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code }),
      });
      if (!res.ok) throw new Error();
      localStorage.setItem('user_email', email);
      navigate('/dashboard');
    } catch {
      setError('Invalid or expired code. Try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
      <div className="auth-container">
        <img src={logoIcon} alt="PricePilot AI Logo" className="auth-logo" />
        <button className="back-button" onClick={() => navigate('/')}>← Back</button>

        {step === 'email' ? (
            <>
              <h2>Welcome Back</h2>
              <p style={{ color: '#666', marginTop: '-10px', marginBottom: '20px' }}>
                Enter your email to receive a login code
              </p>
              {error && <p style={{ color: 'red', fontSize: '14px', marginBottom: '10px' }}>{error}</p>}
              <input
                  type="email"
                  placeholder="Email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleSendOtp()}
              />
              <button onClick={handleSendOtp} disabled={loading}>
                {loading ? 'Sending...' : 'Send Code'}
              </button>
            </>
        ) : (
            <>
              <h2>Check Your Email</h2>
              <p style={{ color: '#666', marginTop: '-10px', marginBottom: '20px' }}>
                Enter the 6-digit code sent to {email}
              </p>
              {error && <p style={{ color: 'red', fontSize: '14px', marginBottom: '10px' }}>{error}</p>}
              <input
                  type="text"
                  maxLength={6}
                  placeholder="000000"
                  value={code}
                  onChange={e => setCode(e.target.value.replace(/\D/g, ''))}
                  onKeyDown={e => e.key === 'Enter' && handleVerifyOtp()}
                  style={{ textAlign: 'center', fontSize: '20px', letterSpacing: '4px' }}
              />
              <button onClick={handleVerifyOtp} disabled={loading}>
                {loading ? 'Verifying...' : 'Verify & Sign In'}
              </button>
              <button className="back-button" onClick={() => { setStep('email'); setError(''); }} style={{ marginTop: '10px' }}>
                ← Use a different email
              </button>
            </>
        )}
      </div>
  );
};

export default Login;
