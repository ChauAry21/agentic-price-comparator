import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import './index.css'
import { ThemeProvider } from './ThemeContext.tsx'
import Splash from './Splash.tsx'
import Login from './Login.tsx'
import Dashboard from './Dashboard.tsx'
import AboutUs from './AboutUs.tsx'
import TeamPage from './TeamPage.tsx'
import ContactUs from './ContactUs.tsx'
import NotFound from './NotFound.tsx'
import OAuthCallback from './OAuthCallback.tsx'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isLoggedIn = !!localStorage.getItem('user_email')
  return isLoggedIn ? children : <Navigate to="/login" replace />
}

createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <ThemeProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Splash />} />
            <Route path="/about" element={<AboutUs />} />
            <Route path="/team" element={<TeamPage />} />
            <Route path="/contact" element={<ContactUs />} />
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
            <Route path="/oauth-callback" element={<OAuthCallback />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </ThemeProvider>
    </StrictMode>
)