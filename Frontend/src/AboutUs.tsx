import React from 'react'
import { useNavigate } from 'react-router-dom'
import logoIcon from './assets/logo.jpg'
import './AboutUs.css' 

const AboutUs: React.FC = () => {
  const navigate = useNavigate()

  return (
    <main className="about-page">
      {/* Header Navigation */}
      <header className="header">
        <div className="logo" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
          <img src={logoIcon} alt="PricePilot AI Logo" />
        </div>
        <nav className="nav-links">
          <button className="nav-btn active">About Us</button>
          <button className="nav-btn">Contact Us</button>
          <button className="nav-btn get-started" onClick={() => navigate('/login')}>
            Get Started
          </button>
        </nav>
      </header>

      {/* Main Content Sections */}
      <div className="about-container">
        
        {/* Welcome Section */}
        <section className="welcome-section">
          <h2>Welcome to PricePilot ✈️</h2>
          <p>
            At PricePilot, we believe that finding the best deal shouldn’t feel like a second job. 
            You shouldn't have to open twenty different tabs, track changing prices, or wonder if 
            you're actually getting the lowest price. We built PricePilot to take the turbulence 
            out of online shopping and put you firmly in the captain's seat. 🧭✨
          </p>
        </section>

        {/* Split Content Grid Section */}
        <section className="content-grid">
          
          {/* Left Column: What We Do */}
          <div className="grid-col left-col">
            <h3>What We Do 🌟</h3>
            <p>
              PricePilot is an intelligent, autonomous shopping assistant designed to do the heavy 
              lifting for you. Powered by advanced agentic technology, our platform automatically 
              navigates the web to hunt down, organize, and contrast product prices in the blink 
              of an eye. Whether you are looking for the absolute lowest price or just trying to 
              find out who has your favorite item in stock, PricePilot navigates the chaotic world 
              of e-commerce so you don't have to. 🛒🛍️
            </p>
            
            <button className="meet-team-btn" onClick={() => navigate('/team')}>
              Meet Team
            </button>
          </div>

          {/* Right Column: Our Mission */}
          <div className="grid-col right-col">
            <h3>Our Mission 🎯</h3>
            <p>
              Our mission is to bring absolute transparency and effortless ease to digital retail. 
              We are dedicated to building smart, user-centered digital solutions that empower 
              shoppers to make confident, data-backed decisions every time they hit "Add to Cart."🛒💡
            </p>
          </div>

        </section>
      </div>
    </main>
  )
}

export default AboutUs