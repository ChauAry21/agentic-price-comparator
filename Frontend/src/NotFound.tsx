import { useNavigate } from 'react-router-dom'
import logoIcon from './assets/logo.jpg'

const NotFound = () => {
  const navigate = useNavigate()

  return (
    <main style={{
      background: '#000',
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      fontFamily: 'system-ui, sans-serif',
      textAlign: 'center',
      padding: '40px 20px',
    }}>
      <img src={logoIcon} alt="PricePilot AI" style={{ width: 56, height: 56, borderRadius: 12, marginBottom: 32 }} />
      <h1 style={{ fontSize: 96, fontWeight: 800, color: '#1a1a1a', margin: '0 0 8px', lineHeight: 1 }}>404</h1>
      <h2 style={{ fontSize: 22, fontWeight: 600, color: '#ffffff', margin: '0 0 12px' }}>Page not found</h2>
      <p style={{ color: '#555', fontSize: 15, marginBottom: 36, maxWidth: 360 }}>
        Looks like this page took a wrong turn. Let's get you back on course.
      </p>
      <div style={{ display: 'flex', gap: 12 }}>
        <button
          onClick={() => navigate(-1)}
          style={{
            background: 'transparent',
            border: '1px solid #333',
            color: '#aaa',
            padding: '10px 20px',
            borderRadius: 8,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 500,
          }}
        >
          ← Go back
        </button>
        <button
          onClick={() => navigate('/')}
          style={{
            background: '#ffffff',
            border: 'none',
            color: '#000',
            padding: '10px 20px',
            borderRadius: 8,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 600,
          }}
        >
          Go home
        </button>
      </div>
    </main>
  )
}

export default NotFound
