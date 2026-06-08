const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const DEMO_FALLBACK_KEY = 'pricepilot-demo-fallback';

export interface PriceResult {
    retailerName: string;
    productName: string;
    price: string;
    currency: string;
    url: string;
}

export interface PriceComparisonResponse {
    query: string;
    resultCount: number;
    retailersQueried: string[];
    retailersWithResults: string[];
    bestRetailer: string | null;
    lowestPrice: number;
    highestPrice: number;
    averagePrice: number;
    potentialSavings: number;
    results: PriceResult[];
    demoMode?: boolean;
}

const DEMO_RETAILERS = ['Amazon', 'Walmart', 'Newegg', 'eBay'];

function isDemoFallbackEnabled() {
    return localStorage.getItem(DEMO_FALLBACK_KEY) !== 'false';
}

function buildDemoResponse(query: string): PriceComparisonResponse {
    const seed = Array.from(query).reduce((total, char) => total + char.charCodeAt(0), 0);
    const basePrice = Math.max(149, (seed % 900) + 99);
    const results = DEMO_RETAILERS.map((retailerName, index) => {
        const price = basePrice + index * 24 + ((seed + index * 13) % 17);

        return {
            retailerName,
            productName: `${query} - ${retailerName} listing`,
            price: `$${price.toFixed(2)}`,
            currency: 'USD',
            url: `https://www.google.com/search?q=${encodeURIComponent(`${query} ${retailerName}`)}`,
        };
    });

    const prices = results.map((result) => Number(result.price.replace(/[^0-9.]/g, '')));
    const lowestPrice = Math.min(...prices);
    const highestPrice = Math.max(...prices);
    const bestRetailer = results[prices.indexOf(lowestPrice)].retailerName;

    return {
        query,
        resultCount: results.length,
        retailersQueried: DEMO_RETAILERS,
        retailersWithResults: DEMO_RETAILERS,
        bestRetailer,
        lowestPrice,
        highestPrice,
        averagePrice: Number((prices.reduce((sum, price) => sum + price, 0) / prices.length).toFixed(2)),
        potentialSavings: Number((highestPrice - lowestPrice).toFixed(2)),
        results,
        demoMode: true,
    };
}

export async function searchPrices(query: string): Promise<PriceComparisonResponse> {
    try {
        const response = await fetch(`${BASE_URL}/api/prices/search?query=${encodeURIComponent(query)}`);
        if (!response.ok) {
            throw new Error(`Search failed: ${response.statusText || response.status}`);
        }
        return response.json();
    } catch (error) {
        if (import.meta.env.DEV && isDemoFallbackEnabled()) {
            return buildDemoResponse(query);
        }

        throw error instanceof Error
            ? error
            : new Error('Unable to reach the price comparison API');
    }
}
