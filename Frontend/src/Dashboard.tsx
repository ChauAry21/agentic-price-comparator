import React from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';
import './dashboard.css'; // Ensure you have this file

const Dashboard = () => {
  const navigate = useNavigate();

  return (
    <div className="dashboard-wrapper">
      <aside className="dashboard-sidebar">
        <div className="sidebar-logo">
          <img src={logoIcon} alt="Logo" />
          <p>PricePilot <span>AI</span></p>
        </div>
        
        <nav className="sidebar-nav">
          <button className="active">Mine</button>
          <button>Search</button>
          <button>T-v-o-k</button>
          <button>Home</button>
          <button>Settings</button>
        </nav>

        <div className="sidebar-footer">
          <div className="user-profile">
            <span onClick={() => navigate('/')} style={{cursor: 'pointer'}}>→</span>
          </div>
        </div>
      </aside>

      <main className="dashboard-main">
        <h2>Product Category</h2>
        <div className="category-grid">
          {['Electronics', 'Fashion', 'Home', 'Toys', 'Motors', 'Accessories'].map((cat) => (
            <div key={cat} className="category-card">{cat}</div>
          ))}
        </div>
        
        <h3>Select Interests</h3>
        <div className="interests-grid">
          {['Electronics', 'Fashion', 'Home', 'Persons', 'Permanaters', 'Others', 'Starts', 'Permissicons', 'Verteres'].map((int) => (
            <label key={int} className="interest-item">
              <input type="checkbox" /> {int}
            </label>
          ))}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;