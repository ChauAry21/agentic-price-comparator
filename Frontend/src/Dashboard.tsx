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

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';
import { searchPrices, type PriceComparisonResponse, type PriceResult } from './api/priceApi';
import './dashboard.css';

const RETAILER_COLORS: Record<string, string> = {
  Amazon: '#FF9900',
  Walmart: '#0071CE',
  Newegg: '#E2231A',
  eBay: '#86B817',
};

const Dashboard = () => {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [response, setResponse] = useState<PriceComparisonResponse | null>(null);

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError('');
    setResponse(null);
    try {
      const data = await searchPrices(query.trim());
      const sorted = { ...data, results: [...data.results].sort((a, b) => parsePrice(a.price) - parsePrice(b.price)) };
      setResponse(sorted);
    } catch (e: any) {
      setError(e.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const parsePrice = (price: string) => parseFloat(price.replace(/[^0-9.]/g, '')) || 0;

  const getBestDeal = (results: PriceResult[]) =>
      results.length > 0 ? results.reduce((a, b) => parsePrice(a.price) < parsePrice(b.price) ? a : b) : null;

  return (
      <div className="dashboard-wrapper">
        <aside className="dashboard-sidebar">
          <div className="sidebar-logo">
            <img src={logoIcon} alt="Logo" />
            <p>PriceHawk <span>AI</span></p>
          </div>
          <nav className="sidebar-nav">
            <button className="active">Search</button>
            <button>History</button>
            <button>Settings</button>
          </nav>
          <div className="sidebar-footer">
            <div className="user-profile">
              <span onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>→ Logout</span>
            </div>
          </div>
        </aside>

        <main className="dashboard-main">
          <div className="search-section">
            <h2>Find the Best Price</h2>
            <p className="search-subtitle">We search Amazon, Walmart, Newegg and more in real time</p>
            <div className="search-row">
              <input
                  className="search-input"
                  type="text"
                  placeholder="Search for a product e.g. MacBook Pro, RTX 4090..."
                  value={query}
                  onChange={e => setQuery(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleSearch()}
              />
              <button className="search-btn" onClick={handleSearch} disabled={loading}>
                {loading ? 'Searching...' : 'Search'}
              </button>
            </div>
          </div>

          {loading && (
              <div className="loading-state">
                <div className="spinner" />
                <p>Searching across retailers...</p>
              </div>
          )}

          {error && <div className="error-banner">{error}</div>}

          {response && !loading && (
              <div className="results-section">
                <div className="results-meta">
                  <span>{response.resultCount} results for <strong>"{response.query}"</strong></span>
                  <div className="retailer-badges">
                    {response.retailersQueried.map(r => (
                        <span
                            key={r}
                            className={`retailer-badge ${response.retailersWithResults.includes(r) ? 'active' : 'inactive'}`}
                            style={{ borderColor: response.retailersWithResults.includes(r) ? RETAILER_COLORS[r] || '#555' : '#333' }}
                        >
                    {r}
                  </span>
                    ))}
                  </div>
                </div>

                {response.results.length === 0 ? (
                    <div className="no-results">No results found. Try a different search term.</div>
                ) : (
                    <table className="results-table">
                      <thead>
                      <tr>
                        <th>#</th>
                        <th>Product</th>
                        <th>Retailer</th>
                        <th>Price</th>
                        <th></th>
                      </tr>
                      </thead>
                      <tbody>
                      {response.results.map((result, i) => {
                        const best = getBestDeal(response.results);
                        const isBest = best?.url === result.url;
                        return (
                            <tr key={i} className={isBest ? 'best-deal' : ''}>
                              <td className="rank">{i + 1}</td>
                              <td className="product-name">
                                {isBest && <span className="best-badge">Best Deal</span>}
                                {result.productName}
                              </td>
                              <td>
                          <span
                              className="retailer-tag"
                              style={{ color: RETAILER_COLORS[result.retailerName] || '#aaa' }}
                          >
                            {result.retailerName}
                          </span>
                              </td>
                              <td className="price">{result.price}</td>
                              <td>
                                <a href={result.url} target="_blank" rel="noopener noreferrer" className="view-btn">
                                  View →
                                </a>
                              </td>
                            </tr>
                        );
                      })}
                      </tbody>
                    </table>
                )}
              </div>
          )}
        </main>
      </div>
  );
};

export default Dashboard;