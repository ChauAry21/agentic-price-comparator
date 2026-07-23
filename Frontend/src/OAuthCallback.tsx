import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

const OAuthCallback = () => {
    const navigate = useNavigate();
    const handled = useRef(false);

    useEffect(() => {
        if (handled.current) return;
        handled.current = true;

        const params = new URLSearchParams(window.location.search);
        const email = params.get('email');
        if (email) {
            localStorage.setItem('user_email', email);
            navigate('/dashboard', { replace: true });
        } else {
            navigate('/login?error=oauth_failed', { replace: true });
        }
    }, [navigate]);

    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '100vh',
            background: '#0a0a0a',
            color: '#555',
            fontSize: '14px',
            fontFamily: 'Inter, sans-serif'
        }}>
            Signing you in...
        </div>
    );
};

export default OAuthCallback;