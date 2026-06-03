const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

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
    retailerWithResults: string[];
    results: PriceResult[];
}

export async function searchPrices(query: string): Promise<PriceComparisonResponse> {
    const response = await fetch(`${BASE_URL}/api/prices/search?query=${encodeURIComponent(query)}`);
    if (!response.ok) throw new Error(`Search failed: ${response.statusText}`);
    return response.json();
}