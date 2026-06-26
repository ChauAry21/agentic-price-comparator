import { useMemo, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';
import { searchPrices, type PriceComparisonResponse, type PriceResult } from './api/priceApi';
import AlertModal from './components/AlertModal';
import Settings from './components/Settings';
import { ThemeToggle } from './ThemeContext';
import './dashboard.css';

const RETAILER_COLORS: Record<string, string> = {
  Amazon: '#FF9900',
  Walmart: '#0071CE',
  Newegg: '#E2231A',
  eBay: '#86B817',
};

type SortOption = 'ranking' | 'price-asc' | 'price-desc' | 'savings-desc';
type DashboardView = 'home' | 'search' | 'saved' | 'history' | 'settings' | 'alerts';

const SETTINGS_KEY = 'pricepilot-dashboard-settings';
const RECENT_SEARCHES_KEY = 'pricepilot-recent-searches';
const SAVED_PRODUCTS_KEY = 'pricepilot-saved-products';

type SavedProduct = PriceResult & {
  savedAt: string;
  sourceQuery: string;
};

type RetailerResponse = PriceComparisonResponse & {
  retailersWithResults?: string[];
  retailerWithResults?: string[];
};

const parsePrice = (price: string) => parseFloat(price.replace(/[^0-9.]/g, '')) || 0;

const getBestDeal = (results: PriceResult[]) =>
  results.length > 0 ? results.reduce((a, b) => parsePrice(a.price) < parsePrice(b.price) ? a : b) : null;

const getHighestPrice = (results: PriceResult[]) =>
  results.length > 0 ? results.reduce((a, b) => parsePrice(a.price) > parsePrice(b.price) ? a : b) : null;

const calculateSavings = (results: PriceResult[]) => {
  const best = getBestDeal(results);
  const highest = getHighestPrice(results);
  if (!best || !highest) return '0.00';
  return (parsePrice(highest.price) - parsePrice(best.price)).toFixed(2);
};

const calculateSavingsPercent = (result: PriceResult, results: PriceResult[]) => {
  const highest = getHighestPrice(results);
  const highestPrice = highest ? parsePrice(highest.price) : 0;
  if (highestPrice <= 0) return 0;
  return ((highestPrice - parsePrice(result.price)) / highestPrice) * 100;
};

const getRetailersWithResults = (response: PriceComparisonResponse) => {
  const r = response as RetailerResponse;
  return r.retailersWithResults || r.retailerWithResults || [];
};

const loadSavedSettings = () => {
  try {
    const saved = localStorage.getItem(SETTINGS_KEY);
    return saved ? JSON.parse(saved) : { defaultSort: 'ranking', minSavingsPercent: 0 };
  } catch {
    return { defaultSort: 'ranking', minSavingsPercent: 0 };
  }
};

const loadRecentSearches = () => {
  try {
    return JSON.parse(localStorage.getItem(RECENT_SEARCHES_KEY) || '[]') as string[];
  } catch {
    return [];
  }
};

const loadSavedProducts = () => {
  try {
    return JSON.parse(localStorage.getItem(SAVED_PRODUCTS_KEY) || '[]') as SavedProduct[];
  } catch {
    return [];
  }
};

const getProductKey = (product: PriceResult) =>
  product.url || `${product.retailerName}-${product.productName}-${product.price}`;

const getErrorMessage = (error: unknown) =>
  error instanceof Error ? error.message : 'Something went wrong';

const getBestSavingsFromSaved = (savedProducts: SavedProduct[]): string => {
  if (savedProducts.length === 0) return '$0';
  const prices = savedProducts.map(p => parsePrice(p.price)).filter(p => p > 0);
  if (prices.length < 2) return '$0';
  const max = Math.max(...prices);
  const min = Math.min(...prices);
  return `$${(max - min).toFixed(0)}`;
};

const NAV_ITEMS: { view: DashboardView; emoji: string; label: string }[] = [
  { view: 'home', emoji: '🏠', label: 'Dashboard' },
  { view: 'search', emoji: '🔍', label: 'Search' },
  { view: 'alerts', emoji: '🔔', label: 'Alerts' },
  { view: 'saved', emoji: '🔖', label: 'Saved Products' },
  { view: 'history', emoji: '📋', label: 'History' },
  { view: 'settings', emoji: '⚙️', label: 'Settings' },
];

const Dashboard = () => {
  const navigate = useNavigate();

  // Handle OAuth redirect — backend sends ?oauth_email=user@gmail.com after Google/GitHub login
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const oauthEmail = params.get('oauth_email');
    if (oauthEmail) {
      localStorage.setItem('user_email', oauthEmail);
      window.history.replaceState({}, '', '/dashboard');
    }
  }, []);

  const userEmail = localStorage.getItem('user_email') || 'User';
  const userDisplayName = userEmail.includes('@') ? userEmail.split('@')[0] : userEmail;

  const [activeView, setActiveView] = useState<DashboardView>('home');
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [response, setResponse] = useState<PriceComparisonResponse | null>(null);
  const [alertQuery, setAlertQuery] = useState<string | null>(null);
  const [sortOption, setSortOption] = useState<SortOption>(() => loadSavedSettings().defaultSort);
  const [retailerFilter, setRetailerFilter] = useState('all');
  const [recentSearches, setRecentSearches] = useState<string[]>(() => loadRecentSearches());
  const [savedProducts, setSavedProducts] = useState<SavedProduct[]>(() => loadSavedProducts());

  const retailersWithResults = response ? getRetailersWithResults(response) : [];

  const availableRetailers = useMemo(
    () => response ? Array.from(new Set(response.results.map(r => r.retailerName))).sort() : [],
    [response]
  );

  const visibleResults = useMemo(() => {
    if (!response) return [];
    const savedPrefs = loadSavedSettings();
    const filtered = response.results
      .filter(r => retailerFilter === 'all' ? true : r.retailerName === retailerFilter)
      .filter(r => calculateSavingsPercent(r, response.results) >= savedPrefs.minSavingsPercent);
    if (sortOption === 'ranking') {
      return filtered;
    }
    return filtered.sort((a, b) => {
      if (sortOption === 'price-desc') return parsePrice(b.price) - parsePrice(a.price);
      if (sortOption === 'savings-desc') {
        return calculateSavingsPercent(b, response.results) - calculateSavingsPercent(a, response.results);
      }
      return parsePrice(a.price) - parsePrice(b.price);
    });
  }, [response, retailerFilter, sortOption]);

  const saveRecentSearch = (q: string) => {
    setRecentSearches(current => {
      const updated = [q, ...current.filter(item => item !== q)].slice(0, 6);
      localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const handleSearch = async (nextQuery = query) => {
    const trimmed = nextQuery.trim();
    if (!trimmed) return;
    setLoading(true);
    setError('');
    setResponse(null);
    setQuery(trimmed);
    setActiveView('search');
    const savedPrefs = loadSavedSettings();
    setSortOption(savedPrefs.defaultSort);
    try {
      const data = await searchPrices(trimmed);
      setResponse(data);
      setRetailerFilter('all');
      saveRecentSearch(trimmed);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  const clearHistory = () => {
    setRecentSearches([]);
    localStorage.removeItem(RECENT_SEARCHES_KEY);
  };

  const isProductSaved = (product: PriceResult) =>
    savedProducts.some(saved => getProductKey(saved) === getProductKey(product));

  const saveProduct = (product: PriceResult) => {
    setSavedProducts(current => {
      const key = getProductKey(product);
      if (current.some(saved => getProductKey(saved) === key)) return current;
      const updated = [
        {
          ...product,
          savedAt: new Date().toISOString(),
          sourceQuery: response?.query || query,
        },
        ...current,
      ];
      localStorage.setItem(SAVED_PRODUCTS_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const removeSavedProduct = (product: PriceResult) => {
    setSavedProducts(current => {
      const key = getProductKey(product);
      const updated = current.filter(saved => getProductKey(saved) !== key);
      localStorage.setItem(SAVED_PRODUCTS_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const clearSavedProducts = () => {
    setSavedProducts([]);
    localStorage.removeItem(SAVED_PRODUCTS_KEY);
  };

  const clearFilters = () => {
    setSortOption(loadSavedSettings().defaultSort);
    setRetailerFilter('all');
  };

  const bestDeal = response ? getBestDeal(response.results) : null;

  const getSmartRecommendation = (results: PriceResult[]): string => {
    if (!results || results.length === 0) return '';
    const best = getBestDeal(results);
    const highest = getHighestPrice(results);
    if (!best || !highest) return '';
    const savings = parsePrice(highest.price) - parsePrice(best.price);
    const savingsPct = ((savings / parsePrice(highest.price)) * 100).toFixed(0);
    const retailers = Array.from(new Set(results.map(r => r.retailerName)));
    const cheapestRetailer = best.retailerName;
    if (savings < 1) {
      return `Prices are consistent across ${retailers.length} retailer${retailers.length > 1 ? 's' : ''}. Any listing is a fair deal — pick based on shipping speed or seller trust.`;
    }
    if (Number(savingsPct) >= 30) {
      return `Big price gap detected. ${cheapestRetailer} has the best deal at ${best.price}, saving you $${savings.toFixed(2)} (${savingsPct}%) compared to the highest listing. We strongly recommend going with ${cheapestRetailer}.`;
    }
    return `${cheapestRetailer} offers the best price at ${best.price}, saving you $${savings.toFixed(2)} (${savingsPct}%) across ${retailers.length} retailer${retailers.length > 1 ? 's' : ''}. A solid pick if price is your priority.`;
  };

  const LOADING_STEPS = [
    'Launching AI agent...',
    'Searching Amazon...',
    'Searching Walmart...',
    'Searching Newegg...',
    'Ranking results...',
  ];
  const [loadingStep, setLoadingStep] = useState(0);

  useEffect(() => {
    if (!loading) { setLoadingStep(0); return; }
    setLoadingStep(0);
    const intervals = LOADING_STEPS.map((_, i) =>
      setTimeout(() => setLoadingStep(i), i * 2500)
    );
    return () => intervals.forEach(clearTimeout);
  }, [loading]);

  return (
    <div className="dashboard-wrapper">
      {/* Sidebar */}
      <aside className="dashboard-sidebar">
        <div className="sidebar-logo">
          <img src={logoIcon} alt="Logo" />
          <p>PricePilot <span>AI</span></p>
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map(({ view, emoji, label }) => (
            <button
              key={view}
              className={activeView === view ? 'active' : ''}
              onClick={() => setActiveView(view)}
            >
              <span className="nav-emoji">{emoji}</span>
              <span>{label}</span>
              {view === 'saved' && savedProducts.length > 0 && (
                <span className="nav-badge">{savedProducts.length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-promo">
          <div className="promo-icon">🚀</div>
          <p className="promo-title">Upgrade to Pro</p>
          <p className="promo-desc">Get unlimited searches and real-time alerts.</p>
          <button className="promo-btn">Upgrade Now</button>
        </div>

        <div className="sidebar-footer">
          <div className="sidebar-theme-toggle">
            <ThemeToggle />
          </div>
          <div className="sidebar-user">
            <div className="sidebar-user-avatar">{userDisplayName.charAt(0).toUpperCase()}</div>
            <div className="sidebar-user-info">
              <span className="sidebar-user-email">{userEmail}</span>
            </div>
          </div>
          <button className="sidebar-logout" onClick={() => navigate('/')}>Logout</button>
        </div>
      </aside>

      {/* Main content */}
      <main className="dashboard-main">
        {/* Top header with search bar */}
        <header className="dashboard-header">
          <div className="header-search-wrapper">
            <span className="header-search-icon">🔍</span>
            <input
              className="header-search-input"
              type="text"
              placeholder="Search for a product e.g. MacBook Pro, RTX 4090..."
              value={query}
              onChange={e => setQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
            />
            <button
              className="header-search-btn"
              onClick={() => handleSearch()}
              disabled={loading}
            >
              {loading ? 'Searching...' : 'Search'}
            </button>
          </div>
        </header>

        {/* Home view */}
        {activeView === 'home' && (
          <div className="home-view">
            <div className="welcome-section">
              <h1 className="welcome-title">Welcome back, {userDisplayName}! 👋</h1>
              <p className="welcome-subtitle">Here's what's happening with your price comparisons today.</p>
            </div>

            {/* Stats row */}
            <div className="stats-grid">
              <div className="stats-card">
                <div className="stats-icon stats-icon--purple">🔖</div>
                <div className="stats-info">
                  <span className="stats-label">Saved Products</span>
                  <strong className="stats-value">{savedProducts.length}</strong>
                </div>
              </div>
              <div className="stats-card">
                <div className="stats-icon stats-icon--orange">🔍</div>
                <div className="stats-info">
                  <span className="stats-label">Recent Searches</span>
                  <strong className="stats-value">{recentSearches.length}</strong>
                </div>
              </div>
              <div className="stats-card">
                <div className="stats-icon stats-icon--blue">🔔</div>
                <div className="stats-info">
                  <span className="stats-label">Active Alerts</span>
                  <strong className="stats-value">0</strong>
                </div>
              </div>
              <div className="stats-card">
                <div className="stats-icon stats-icon--green">💰</div>
                <div className="stats-info">
                  <span className="stats-label">Best Savings</span>
                  <strong className="stats-value stats-value--green">{getBestSavingsFromSaved(savedProducts)}</strong>
                </div>
              </div>
            </div>

            {/* Recent Searches */}
            <section className="home-section">
              <div className="home-section-header">
                <h2 className="home-section-title">Recent Searches</h2>
                {recentSearches.length > 0 && (
                  <button className="home-section-action" onClick={clearHistory}>Clear all</button>
                )}
              </div>
              {recentSearches.length === 0 ? (
                <p className="home-empty">No recent searches yet. Use the search bar above to get started.</p>
              ) : (
                <div className="recent-chips">
                  {recentSearches.map(item => (
                    <button
                      key={item}
                      className="recent-chip"
                      onClick={() => handleSearch(item)}
                    >
                      🔍 {item}
                    </button>
                  ))}
                </div>
              )}
            </section>

            {/* Saved Products */}
            <section className="home-section">
              <div className="home-section-header">
                <h2 className="home-section-title">Saved Products</h2>
                {savedProducts.length > 0 && (
                  <button className="home-section-action" onClick={clearSavedProducts}>Clear all</button>
                )}
              </div>
              {savedProducts.length === 0 ? (
                <p className="home-empty">No saved products yet. Search and save products to track them here.</p>
              ) : (
                <div className="home-products-grid">
                  {savedProducts.map(product => (
                    <div className="home-product-card" key={getProductKey(product)}>
                      <div className="home-product-retailer" style={{ color: RETAILER_COLORS[product.retailerName] || '#aaa' }}>
                        {product.retailerName}
                      </div>
                      <h3 className="home-product-name">{product.productName}</h3>
                      <div className="home-product-footer">
                        <span className="home-product-price">{product.price}</span>
                        <button
                          className="home-product-remove"
                          onClick={() => removeSavedProduct(product)}
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}

        {/* Settings view */}
        {activeView === 'settings' && (
          <div className="content-section">
            <Settings />
          </div>
        )}

        {/* Alerts view */}
        {activeView === 'alerts' && (
          <div className="content-section">
            <section className="settings-section">
              <div className="section-heading">
                <div>
                  <h2>Price Alerts</h2>
                  <p className="search-subtitle">Get notified when prices drop on products you're watching.</p>
                </div>
              </div>
              <div className="no-results">No active alerts. Set an alert from any search result.</div>
            </section>
          </div>
        )}

        {/* Saved Products view */}
        {activeView === 'saved' && (
          <div className="content-section">
            <section className="settings-section">
              <div className="section-heading">
                <div>
                  <h2>Saved Products</h2>
                  <p className="search-subtitle">Keep products you want to revisit without running the same search again.</p>
                </div>
                {savedProducts.length > 0 && (
                  <button className="clear-filters-btn" type="button" onClick={clearSavedProducts}>Clear saved</button>
                )}
              </div>
              {savedProducts.length === 0 ? (
                <div className="no-results">
                  No saved products yet. Search for a product and click "Save" on anything you want to track.
                </div>
              ) : (
                <div className="saved-products-list">
                  {savedProducts.map(product => (
                    <article className="saved-product-card" key={getProductKey(product)}>
                      <div>
                        <div className="saved-product-meta">
                          <span className="retailer-tag" style={{ color: RETAILER_COLORS[product.retailerName] || '#aaa' }}>
                            {product.retailerName}
                          </span>
                          <span>Saved from "{product.sourceQuery}"</span>
                        </div>
                        <h3>{product.productName}</h3>
                        <p className="saved-date">Saved {new Date(product.savedAt).toLocaleDateString()}</p>
                      </div>
                      <div className="saved-product-side">
                        <strong>{product.price}</strong>
                        <div className="saved-product-actions">
                          <a href={product.url} target="_blank" rel="noopener noreferrer" className="view-btn">View</a>
                          <button className="btn-alert" type="button" onClick={() => setAlertQuery(product.productName)}>Set Alert</button>
                          <button className="btn-remove" type="button" onClick={() => removeSavedProduct(product)}>Remove</button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}

        {/* History view */}
        {activeView === 'history' && (
          <div className="content-section">
            <section className="settings-section">
              <div className="section-heading">
                <div>
                  <h2>Search History</h2>
                  <p className="search-subtitle">Run a previous comparison again with one click.</p>
                </div>
                {recentSearches.length > 0 && (
                  <button className="clear-filters-btn" type="button" onClick={clearHistory}>Clear history</button>
                )}
              </div>
              {recentSearches.length === 0 ? (
                <div className="no-results">No recent searches yet.</div>
              ) : (
                <div className="history-list">
                  {recentSearches.map(item => (
                    <button key={item} type="button" onClick={() => handleSearch(item)}>
                      <span>{item}</span>
                      <strong>Search again</strong>
                    </button>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}

        {/* Search / results view */}
        {activeView === 'search' && (
          <div className="content-section">
            {loading && (
              <div className="loading-state">
                <div className="spinner" />
                <div className="loading-steps">
                  {LOADING_STEPS.map((step, i) => (
                    <div
                      key={step}
                      className={`loading-step ${i === loadingStep ? 'active' : i < loadingStep ? 'done' : ''}`}
                    >
                      <span className="step-dot" />
                      {step}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {error && <div className="error-banner">{error}</div>}

            {!loading && !response && !error && (
              <div className="search-prompt">
                <div className="search-prompt-icon">🔍</div>
                <h2>Search for a product</h2>
                <p>Use the search bar above to compare prices across Amazon, Walmart, Newegg and more.</p>
              </div>
            )}

            {response && !loading && (
              <div className="results-section">
                <div className="results-meta">
                  <span>{response.resultCount} results for <strong>"{response.query}"</strong></span>
                  <div className="retailer-badges">
                    {(response.retailersQueried || []).map(r => (
                      <span
                        key={r}
                        className={`retailer-badge ${retailersWithResults.includes(r) ? 'active' : 'inactive'}`}
                        style={{ borderColor: retailersWithResults.includes(r) ? RETAILER_COLORS[r] || '#555' : '#333' }}
                      >
                        {r}
                      </span>
                    ))}
                  </div>
                </div>

                {(response.results || []).length > 0 && (
                  <div className="recommendation-banner">
                    <span className="recommendation-label">🤖 PricePilot Recommendation</span>
                    <p>{getSmartRecommendation(response.results)}</p>
                    {bestDeal && (
                      <a href={bestDeal.url} target="_blank" rel="noopener noreferrer" className="recommendation-cta">
                        View Best Deal → {bestDeal.price} on {bestDeal.retailerName}
                      </a>
                    )}
                  </div>
                )}

                {(response.results || []).length > 0 && (
                  <div className="analytics-panel">
                    <div className="analytics-card">
                      <span>Best Price</span>
                      <strong>{bestDeal?.price}</strong>
                    </div>
                    <div className="analytics-card">
                      <span>Highest Price</span>
                      <strong>{getHighestPrice(response.results)?.price}</strong>
                    </div>
                    <div className="analytics-card">
                      <span>Potential Savings</span>
                      <strong>${calculateSavings(response.results)}</strong>
                    </div>
                    <div className="analytics-card">
                      <span>Retailers</span>
                      <strong>{retailersWithResults.length}</strong>
                    </div>
                  </div>
                )}

                {(response.results || []).length === 0 ? (
                  <div className="no-results">No results found. Try a different search term.</div>
                ) : (
                  <>
                    <div className="results-toolbar">
                      <div className="filter-group">
                        <label htmlFor="sort-results">Sort</label>
                        <select id="sort-results" value={sortOption} onChange={e => setSortOption(e.target.value as SortOption)}>
                          <option value="ranking">Most Relevant</option>
                          <option value="price-asc">Cheapest first</option>
                          <option value="price-desc">Highest price</option>
                          <option value="savings-desc">Highest savings</option>
                        </select>
                      </div>
                      <div className="filter-group">
                        <label htmlFor="retailer-filter">Retailer</label>
                        <select id="retailer-filter" value={retailerFilter} onChange={e => setRetailerFilter(e.target.value)}>
                          <option value="all">All retailers</option>
                          {availableRetailers.map(retailer => (
                            <option key={retailer} value={retailer}>{retailer}</option>
                          ))}
                        </select>
                      </div>
                      <span className="visible-count">Showing {visibleResults.length} of {response.results.length}</span>
                      <button className="clear-filters-btn" type="button" onClick={clearFilters}>Clear filters</button>
                    </div>

                    {visibleResults.length === 0 ? (
                      <div className="no-results">No results match your filters. Try clearing filters.</div>
                    ) : (
                      <table className="results-table">
                        <thead>
                          <tr>
                            <th>#</th>
                            <th>Product</th>
                            <th>Retailer</th>
                            <th>Price</th>
                            <th>Savings</th>
                            <th></th>
                          </tr>
                        </thead>
                        <tbody>
                          {visibleResults.map((result, i) => {
                            const isBest = bestDeal?.url === result.url;
                            return (
                              <tr key={`${result.url}-${i}`} className={isBest ? 'best-deal' : ''}>
                                <td className="rank">{i + 1}</td>
                                <td className="product-name">
                                  {isBest && <span className="best-badge">Best Deal</span>}
                                  {result.productName}
                                </td>
                                <td>
                                  <span className="retailer-tag" style={{ color: RETAILER_COLORS[result.retailerName] || '#aaa' }}>
                                    {result.retailerName}
                                  </span>
                                </td>
                                <td className="price">{result.price}</td>
                                <td className="savings">{calculateSavingsPercent(result, response.results).toFixed(0)}%</td>
                                <td className="actions-cell">
                                  <a href={result.url} target="_blank" rel="noopener noreferrer" className="view-btn">View</a>
                                  <button
                                    className={`btn-save ${isProductSaved(result) ? 'saved' : ''}`}
                                    type="button"
                                    onClick={() => isProductSaved(result) ? removeSavedProduct(result) : saveProduct(result)}
                                  >
                                    {isProductSaved(result) ? 'Saved' : 'Save'}
                                  </button>
                                  <button className="btn-alert" onClick={() => setAlertQuery(result.productName)}>Set Alert</button>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        )}
      </main>

      {alertQuery && (
        <AlertModal productQuery={alertQuery} onClose={() => setAlertQuery(null)} />
      )}
    </div>
  );
};

export default Dashboard;
