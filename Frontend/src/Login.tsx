import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg'; 

const Login = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = () => {
    // 1. Email format check
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError('Please enter a valid email address.');
      return;
    }

    // 2. Password length check
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    // 3. Clear errors and navigate
    setError('');
    console.log("Login successful!");
    navigate('/dashboard');
  };

  return (
    <div className="auth-container">
      <img src={logoIcon} alt="PricePilot AI Logo" className="auth-logo" />
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
    </div>
  );
};

export default Login;