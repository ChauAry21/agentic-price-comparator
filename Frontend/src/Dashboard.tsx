import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logoIcon from './assets/logo.jpg';
import { searchPrices, type PriceComparisonResponse, type PriceResult } from './api/priceApi';
import AlertModal from './components/AlertModal';
import Settings from './components/Settings';
import './dashboard.css';

const RETAILER_COLORS: Record<string, string> = {
  Amazon: '#FF9900',
  Walmart: '#0071CE',
  Newegg: '#E2231A',
  eBay: '#86B817',
};

type SortOption = 'ranking' | 'price-asc' | 'price-desc' | 'savings-desc';
type DashboardView = 'search' | 'saved' | 'history' | 'settings';

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

const Dashboard = () => {
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState<DashboardView>('search');
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

  return (
    <div className="dashboard-wrapper">
      <aside className="dashboard-sidebar">
        <div className="sidebar-logo">
          <img src={logoIcon} alt="Logo" />
          <p>PricePilot <span>AI</span></p>
        </div>
        <nav className="sidebar-nav">
          <button className={activeView === 'search' ? 'active' : ''} onClick={() => setActiveView('search')}>Search</button>
          <button className={activeView === 'saved' ? 'active' : ''} onClick={() => setActiveView('saved')}>
            Saved Products {savedProducts.length > 0 ? `(${savedProducts.length})` : ''}
          </button>
          <button className={activeView === 'history' ? 'active' : ''} onClick={() => setActiveView('history')}>History</button>
          <button className={activeView === 'settings' ? 'active' : ''} onClick={() => setActiveView('settings')}>Settings</button>
        </nav>
        <div className="sidebar-footer">
          <div className="user-profile">
            <span onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>Logout</span>
          </div>
        </div>
      </aside>

      <main className="dashboard-main">
        {activeView === 'settings' && <Settings />}

        {activeView === 'saved' && (
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
        )}

        {activeView === 'history' && (
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
        )}

        {activeView === 'search' && (
          <>
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
                <button className="search-btn" onClick={() => handleSearch()} disabled={loading}>
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
          </>
        )}
      </main>

      {alertQuery && (
        <AlertModal productQuery={alertQuery} onClose={() => setAlertQuery(null)} />
      )}
    </div>
  );
};

export default Dashboard;
