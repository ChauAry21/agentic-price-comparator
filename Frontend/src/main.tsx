import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import Splash from './Splash.tsx'
import Login from './Login.tsx'
import Dashboard from './Dashboard.tsx'
import AboutUs from './AboutUs.tsx' // 1. Import your AboutUs component!

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Splash />} />
        <Route path="/about" element={<AboutUs />} /> 
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>
)