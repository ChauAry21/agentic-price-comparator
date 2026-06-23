import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import logoIcon from './assets/logo.jpg'
import './ContactUs.css'
import { ThemeToggle } from './ThemeContext.tsx'

const ContactUs: React.FC = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({ name: '', email: '', message: '' })
  const [submitted, setSubmitted] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // Handle message submission logic here
    setSubmitted(true)
    setFormData({ name: '', email: '', message: '' })
  }

  return (
    <main className="about-page">
      <header className="header">
        <div className="logo" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
          <img src={logoIcon} alt="PricePilot AI Logo" />
        </div>
        <nav className="nav-links">
          <button className="nav-btn" onClick={() => navigate('/about')}>About Us</button>
          <button className="nav-btn active">Contact Us</button>
          <ThemeToggle />
          <button className="nav-btn get-started" onClick={() => navigate('/login')}>
            Get Started
          </button>
        </nav>
      </header>

      <div className="about-container">
        <section className="contact-section">
          <h2>Get in Touch 📬</h2>
          <p className="contact-subtitle">
            Have questions about PricePilot AI or want to share feedback? Drop us a line!
          </p>

          <div className="contact-grid-layout">
            {/* Left: Contact Info Channels */}
            <div className="contact-info-panel">
              <div className="info-card">
                <div className="info-icon">📧</div>
                <h4>Email Us</h4>
                <p><a href="mailto:pricepilotai@gmail.com" className="email-link">pricepilotai@gmail.com</a></p>
                <p className="info-subtext">We look to reply within 24 hours.</p>
              </div>

              <div className="info-card">
                <div className="info-icon">🏢</div>
                <h4>Headquarters</h4>
                <p>Mutiple Locaations in USA </p>
                <p className="info-subtext">Built proud.</p>
              </div>
            </div>

            {/* Right: Interactive Message Form */}
            <div className="contact-form-panel">
              {submitted ? (
                <div className="submission-success">
                  <h3>Message Sent! 🎉</h3>
                  <p>Thanks for reaching out. The PricePilot team will get back to you shortly.</p>
                  <button className="meet-team-btn" onClick={() => setSubmitted(false)}>
                    Send Another Message
                  </button>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="contact-form">
                  <div className="form-group">
                    <label htmlFor="name">Your Name</label>
                    <input
                      type="text"
                      id="name"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="Enter your name"
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="email">Email Address</label>
                    <input
                      type="email"
                      id="email"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      placeholder="you@example.com"
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="message">Message</label>
                    <textarea
                      id="message"
                      required
                      rows={5}
                      value={formData.message}
                      onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                      placeholder="How can our shopping helper guide you today?"
                    />
                  </div>

                  <button type="submit" className="meet-team-btn contact-submit-btn">
                    Send Message 🚀
                  </button>
                </form>
              )}
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}

export default ContactUs