import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import searchIcon from './assets/searchbar.jpg'
import robotIcon from './assets/Robot.jpg'
import tableIcon from './assets/table.jpg'
import logoIcon from './assets/logo.jpg'

const Splash = () => {
  const navigate = useNavigate(); // Called at the top level
  const [isSearching, setIsSearching] = useState(false); // Manages search bar focus state

  return (
    <main>
      {/* Header Navigation with working route handlers */}
      <header>
        <div className="logo">
          <img src={logoIcon} alt="PricePilot AI Logo" />
        </div>
        <nav>
          {/* Added the onClick listener to route to /about */}
          <button onClick={() => navigate('/about')}>About Us</button>
          <button>Contact Us</button>
          <button onClick={() => navigate('/login')}>Get Started</button>
        </nav>
      </header>

      {/* Hero Section */}
      <section className="hero">
        <h1>Smart Shopping, Automated</h1>
        <p>PricePilot AI autonomously hunts, normalizes, and ranks the best deals</p>
        
        <div className="search-bar">
          {isSearching ? (
            <div className="auth-prompt">
              <p>Please log in or sign up to start your search!</p>
              <button className="auth-button" onClick={() => navigate('/login')}>Login</button>
            </div>
          ) : (
            <input 
              type="text" 
              placeholder="Search for a product..." 
              onFocus={() => setIsSearching(true)}
            />
          )}
        </div>
      </section>

      {/* Core Features Overview */}
      <section className="features">
        <div className="feature-item">
          <img src={searchIcon} alt="Search" />
          <p>Search for Product</p>
        </div>
        <div className="feature-item">
          <img src={robotIcon} alt="Agent" />
          <p>Autonomous Agent Deployment</p>
        </div>
        <div className="feature-item">
          <img src={tableIcon} alt="Tables" />
          <p>Ranked Comparison Tables</p>
        </div>
      </section>
    </main>
  )
}

export default Splash