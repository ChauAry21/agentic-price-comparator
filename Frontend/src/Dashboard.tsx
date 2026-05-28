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