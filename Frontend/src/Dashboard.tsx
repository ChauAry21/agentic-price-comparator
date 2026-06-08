import { useMemo, useState } from 'react';
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

type SortOption = 'price-asc' | 'price-desc' | 'savings-desc';
type DashboardView = 'search' | 'history' | 'settings';

interface DashboardSettings {
  defaultSort: SortOption;
  minSavingsPercent: number;
  demoFallback: boolean;
}

type RetailerResponse = PriceComparisonResponse & {
  retailersWithResults?: string[];
  retailerWithResults?: string[];
};

const SETTINGS_KEY = 'pricepilot-dashboard-settings';
const RECENT_SEARCHES_KEY = 'pricepilot-recent-searches';
const DEMO_FALLBACK_KEY = 'pricepilot-demo-fallback';

const DEFAULT_SETTINGS: DashboardSettings = {
  defaultSort: 'price-asc',
  minSavingsPercent: 0,
  demoFallback: true,
};

const parsePrice = (price: string) =>
  parseFloat(price.replace(/[^0-9.]/g, '')) || 0;

const getBestDeal = (results: PriceResult[]) =>
  results.length > 0
    ? results.reduce((a, b) =>
        parsePrice(a.price) < parsePrice(b.price) ? a : b
      )
    : null;

const getHighestPrice = (results: PriceResult[]) =>
  results.length > 0
    ? results.reduce((a, b) =>
        parsePrice(a.price) > parsePrice(b.price) ? a : b
      )
    : null;

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
  const retailerResponse = response as RetailerResponse;
  return retailerResponse.retailersWithResults || retailerResponse.retailerWithResults || [];
};

const loadSettings = (): DashboardSettings => {
  try {
    const saved = localStorage.getItem(SETTINGS_KEY);
    return saved ? { ...DEFAULT_SETTINGS, ...JSON.parse(saved) } : DEFAULT_SETTINGS;
  } catch {
    return DEFAULT_SETTINGS;
  }
};

const loadRecentSearches = () => {
  try {
    return JSON.parse(localStorage.getItem(RECENT_SEARCHES_KEY) || '[]') as string[];
  } catch {
    return [];
  }
};

const Dashboard = () => {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [response, setResponse] = useState<PriceComparisonResponse | null>(null);
  const [settings, setSettings] = useState<DashboardSettings>(() => loadSettings());
  const [sortOption, setSortOption] = useState<SortOption>(() => loadSettings().defaultSort);
  const [retailerFilter, setRetailerFilter] = useState('all');
  const [activeView, setActiveView] = useState<DashboardView>('search');
  const [recentSearches, setRecentSearches] = useState<string[]>(() => loadRecentSearches());

  const retailersWithResults = response ? getRetailersWithResults(response) : [];

  const availableRetailers = useMemo(
    () =>
      response
        ? Array.from(new Set(response.results.map((result) => result.retailerName))).sort()
        : [],
    [response]
  );

  const visibleResults = useMemo(() => {
    if (!response) return [];

    return response.results
      .filter((result) =>
        retailerFilter === 'all' ? true : result.retailerName === retailerFilter
      )
      .filter((result) =>
        calculateSavingsPercent(result, response.results) >= settings.minSavingsPercent
      )
      .sort((a, b) => {
        if (sortOption === 'price-desc') return parsePrice(b.price) - parsePrice(a.price);
        if (sortOption === 'savings-desc') {
          return (
            calculateSavingsPercent(b, response.results) -
            calculateSavingsPercent(a, response.results)
          );
        }
        return parsePrice(a.price) - parsePrice(b.price);
      });
  }, [response, retailerFilter, settings.minSavingsPercent, sortOption]);

  const updateSettings = (nextSettings: DashboardSettings) => {
    setSettings(nextSettings);
    setSortOption(nextSettings.defaultSort);
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(nextSettings));
    localStorage.setItem(DEMO_FALLBACK_KEY, String(nextSettings.demoFallback));
  };

  const saveRecentSearch = (nextQuery: string) => {
    setRecentSearches((current) => {
      const updated = [nextQuery, ...current.filter((item) => item !== nextQuery)].slice(0, 6);
      localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const handleSearch = async (nextQuery = query) => {
    const trimmedQuery = nextQuery.trim();

    if (!trimmedQuery) return;

    setLoading(true);
    setError('');
    setResponse(null);
    setQuery(trimmedQuery);
    setActiveView('search');

    try {
      const data = await searchPrices(trimmedQuery);
      setResponse(data);
      setSortOption(settings.defaultSort);
      setRetailerFilter('all');
      saveRecentSearch(trimmedQuery);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const clearHistory = () => {
    setRecentSearches([]);
    localStorage.removeItem(RECENT_SEARCHES_KEY);
  };

  const bestDeal = response ? getBestDeal(response.results) : null;

  const clearFilters = () => {
    setSortOption(settings.defaultSort);
    setRetailerFilter('all');
  };

  return (
    <div className="dashboard-wrapper">
      <aside className="dashboard-sidebar">
        <div className="sidebar-logo">
          <img src={logoIcon} alt="Logo" />
          <p>
            PricePilot <span>AI</span>
          </p>
        </div>

        <nav className="sidebar-nav">
          <button
            className={activeView === 'search' ? 'active' : ''}
            onClick={() => setActiveView('search')}
          >
            Search
          </button>
          <button
            className={activeView === 'history' ? 'active' : ''}
            onClick={() => setActiveView('history')}
          >
            History
          </button>
          <button
            className={activeView === 'settings' ? 'active' : ''}
            onClick={() => setActiveView('settings')}
          >
            Settings
          </button>
        </nav>

        <div className="sidebar-footer">
          <div className="user-profile">
            <span onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
              -&gt; Logout
            </span>
          </div>
        </div>
      </aside>

      <main className="dashboard-main">
        {activeView === 'search' && (
          <div className="search-section">
            <h2>Find the Best Price</h2>
            <p className="search-subtitle">
              We search Amazon, Walmart, Newegg and more in real time
            </p>

            <div className="search-row">
              <input
                className="search-input"
                type="text"
                placeholder="Search for a product e.g. MacBook Pro, RTX 4090..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />

              <button className="search-btn" onClick={() => handleSearch()} disabled={loading}>
                {loading ? 'Searching...' : 'Search'}
              </button>
            </div>
          </div>
        )}

        {activeView === 'history' && (
          <section className="settings-section">
            <div className="section-heading">
              <div>
                <h2>Search History</h2>
                <p className="search-subtitle">Run a previous comparison again with one click.</p>
              </div>

              {recentSearches.length > 0 && (
                <button className="clear-filters-btn" type="button" onClick={clearHistory}>
                  Clear history
                </button>
              )}
            </div>

            {recentSearches.length === 0 ? (
              <div className="no-results">No recent searches yet.</div>
            ) : (
              <div className="history-list">
                {recentSearches.map((item) => (
                  <button key={item} type="button" onClick={() => handleSearch(item)}>
                    <span>{item}</span>
                    <strong>Search again</strong>
                  </button>
                ))}
              </div>
            )}
          </section>
        )}

        {activeView === 'settings' && (
          <section className="settings-section">
            <div>
              <h2>Settings</h2>
              <p className="search-subtitle">Tune defaults for repeated price checks.</p>
            </div>

            <div className="settings-grid">
              <label className="settings-field">
                <span>Default sort</span>
                <select
                  value={settings.defaultSort}
                  onChange={(e) =>
                    updateSettings({ ...settings, defaultSort: e.target.value as SortOption })
                  }
                >
                  <option value="price-asc">Cheapest first</option>
                  <option value="price-desc">Highest price</option>
                  <option value="savings-desc">Highest savings</option>
                </select>
              </label>

              <label className="settings-field">
                <span>Minimum savings percent</span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={settings.minSavingsPercent}
                  onChange={(e) =>
                    updateSettings({
                      ...settings,
                      minSavingsPercent: Math.min(100, Math.max(0, Number(e.target.value))),
                    })
                  }
                />
              </label>

              <label className="settings-toggle">
                <input
                  type="checkbox"
                  checked={settings.demoFallback}
                  onChange={(e) =>
                    updateSettings({ ...settings, demoFallback: e.target.checked })
                  }
                />
                <span>
                  Use demo results when backend is offline
                  <small>Keeps the dashboard testable during frontend work.</small>
                </span>
              </label>
            </div>
          </section>
        )}

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
              <span>
                {response.resultCount} results for <strong>"{response.query}"</strong>
              </span>

              <div className="retailer-badges">
                {response.retailersQueried.map((r) => (
                  <span
                    key={r}
                    className={`retailer-badge ${
                      retailersWithResults.includes(r) ? 'active' : 'inactive'
                    }`}
                    style={{
                      borderColor: retailersWithResults.includes(r)
                        ? RETAILER_COLORS[r] || '#555'
                        : '#333',
                    }}
                  >
                    {r}
                  </span>
                ))}
              </div>
            </div>

            {response.results.length > 0 && (
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

            {response.results.length === 0 ? (
              <div className="no-results">
                No results found. Try a different search term.
              </div>
            ) : (
              <>
                <div className="results-toolbar">
                  <div className="filter-group">
                    <label htmlFor="sort-results">Sort</label>
                    <select
                      id="sort-results"
                      value={sortOption}
                      onChange={(e) => setSortOption(e.target.value as SortOption)}
                    >
                      <option value="price-asc">Cheapest first</option>
                      <option value="price-desc">Highest price</option>
                      <option value="savings-desc">Highest savings</option>
                    </select>
                  </div>

                  <div className="filter-group">
                    <label htmlFor="retailer-filter">Retailer</label>
                    <select
                      id="retailer-filter"
                      value={retailerFilter}
                      onChange={(e) => setRetailerFilter(e.target.value)}
                    >
                      <option value="all">All retailers</option>
                      {availableRetailers.map((retailer) => (
                        <option key={retailer} value={retailer}>
                          {retailer}
                        </option>
                      ))}
                    </select>
                  </div>

                  <span className="visible-count">
                    Showing {visibleResults.length} of {response.results.length}
                  </span>

                  <button className="clear-filters-btn" type="button" onClick={clearFilters}>
                    Clear filters
                  </button>
                </div>

                {visibleResults.length === 0 ? (
                  <div className="no-results">
                    No results match your filters. Try clearing filters.
                  </div>
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
                              <span
                                className="retailer-tag"
                                style={{
                                  color: RETAILER_COLORS[result.retailerName] || '#aaa',
                                }}
                              >
                                {result.retailerName}
                              </span>
                            </td>

                            <td className="price">{result.price}</td>

                            <td className="savings">
                              {calculateSavingsPercent(result, response.results).toFixed(0)}%
                            </td>

                            <td>
                              <a
                                href={result.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="view-btn"
                              >
                                View -&gt;
                              </a>
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
      </main>
    </div>
  );
};

export default Dashboard;
