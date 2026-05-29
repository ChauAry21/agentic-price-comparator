import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';

const Signup = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState(''); // New state for error messages

  const handleSignup = () => {
    // 1. Email check (simple regex)
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    if (!emailRegex.test(email)) {
      setError('Please enter a valid email address.');
      return;
    }

    // 2. Password match check
    if (password !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    // If all checks pass
    setError('');
    console.log("Signup successful!");
    navigate('/dashboard');
  };

  return (
    <div className="auth-container">
      <img src={logoIcon} alt="PricePilot AI Logo" className="auth-logo" />
      <button className="back-button" onClick={() => navigate('/')}>← Back</button>
      <h2>Create Account</h2>
      
      {/* Display error message if it exists */}
      {error && <p style={{ color: 'red', fontSize: '14px' }}>{error}</p>}

      <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <input type="password" placeholder="Retype password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
      
      <button onClick={handleSignup}>Sign Up</button>
    </div>
  );
};

export default Signup;