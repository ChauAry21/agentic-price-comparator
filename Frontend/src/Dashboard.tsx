import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';
import './dashboard.css'; 

const Dashboard = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('AI Search');

  return (
    <div className="dashboard-wrapper">
      {/* --- SIDEBAR --- */}
      <aside className="dashboard-sidebar">
        <div className="sidebar-top">
          <div className="sidebar-logo">
            <img src={logoIcon} alt="Logo" />
            <p>PRICEPILOT <span>AI</span></p>
          </div>
          
          <nav className="sidebar-nav">
            <button className="active"><span>🏠</span> Dashboard</button>
            <button><span>🔍</span> AI Search</button>
            <button><span>📊</span> Compare</button>
            <button><span>🤍</span> Saved</button>
            <button><span>📍</span> Tracked</button>
            <button><span>⏳</span> History</button>
            <button><span>🔔</span> Alerts</button>
            <button><span>⚙️</span> Settings</button>
          </nav>
        </div>

        <div className="sidebar-bottom">
          <button className="upgrade-btn">📍 Upgrade to Pro</button>
        </div>
      </aside>

      {/* --- MAIN INTERFACE CONTROLLER --- */}
      <main className="dashboard-main">
        
        {/* --- GLOBAL TOP BAR --- */}
        <header className="main-top-bar">
          <div className="global-search-container">
            <span style={{ position: 'absolute', left: '14px', top: '10px', color: '#555' }}>🔍</span>
            <input type="text" placeholder="Search for anything..." />
          </div>
          
          <div className="top-bar-actions">
            <span className="action-icon">🌙</span>
            <span className="action-icon">🔔</span>
            <img 
              src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80" 
              alt="User profile" 
              className="user-avatar"
              onClick={() => navigate('/')} 
            />
          </div>
        </header>

        {/* --- CORE CONTENT FRAME --- */}
        <div className="main-content-body">
          
          {/* Welcome Intro Section */}
          <section className="dashboard-hero-intro">
            <p className="welcome-text">Welcome back, Mohamed 👋</p>
            <h1 className="hero-heading">
              Find the best products<br />
              at the <span>best prices.</span>
            </h1>
          </section>

          {/* Core Central Search Utility */}
          <section className="search-utility-container">
            <div className="search-tabs">
              {['AI Search', 'Products', 'Categories'].map((tab) => (
                <button 
                  key={tab} 
                  className={`tab-item ${activeTab === tab ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab)}
                >
                  {tab}
                </button>
              ))}
            </div>

            <div className="main-search-box">
              <input type="text" placeholder="What are you looking for today?" />
              <button className="search-submit-btn">Search</button>
            </div>
          </section>

          {/* Value Propositions / Features Grid */}
          <section className="features-mini-grid">
            <div className="feature-mini-card">
              <div className="feature-card-icon">🧠</div>
              <div className="feature-card-text">
                <h4>AI-Powered Search</h4>
                <p>Smart product understanding</p>
              </div>
            </div>

            <div className="feature-mini-card">
              <div className="feature-card-icon">🔄</div>
              <div className="feature-card-text">
                <h4>Price Comparison</h4>
                <p>Real-time across stores</p>
              </div>
            </div>

            <div className="feature-mini-card">
              <div className="feature-card-icon">📈</div>
              <div className="feature-card-text">
                <h4>Price Tracking</h4>
                <p>Get price drop alerts</p>
              </div>
            </div>

            <div className="feature-mini-card">
              <div className="feature-card-icon">🛡️</div>
              <div className="feature-card-text">
                <h4>Secure & Private</h4>
                <p>Your data is protected</p>
              </div>
            </div>
          </section>

          {/* Quick-Access Popular Tags */}
          <section className="popular-searches-section">
            <h5>Popular Searches</h5>
            <div className="tags-wrapper">
              {['iPhone 15 Pro', 'MacBook Air M2', 'Sony WH-1000XM5', 'Nintendo Switch OLED', 'iPad Air 5'].map((tag) => (
                <span key={tag} className="search-tag">{tag}</span>
              ))}
            </div>
          </section>

        </div>
      </main>
    </div>
  );
};

export default Dashboard;