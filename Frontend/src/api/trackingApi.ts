const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function trackingHeaders() {
    const email = localStorage.getItem('user_email');
    return { 'Content-Type': 'application/json', 'X-User-Email': email ?? '' };
}

// Types
export interface TrackingCandidate {
    productKey: string;
    productName: string;
    sampleUrl: string;
    retailers: string[];
    lowestPrice: number;
    resultCount: number;
}

export interface TrackingCandidateResponse {
    query: string;
    autoSelected: boolean;
    candidates: TrackingCandidate[];
}

export interface CreateTrackedQueryRequest {
    rawQuery: string;
    canonicalProductKey: string;
    canonicalProductName: string;
    referenceUrl: string;
}

export interface TrackedQuery {
    id: string;
    rawQuery: string;
    canonicalProductKey: string;
    canonicalProductName: string;
    referenceUrl: string;
    status: 'ACTIVE' | 'PAUSED';
    createdAt: string;
    lastScrapedAt: string | null;
}

export interface RetailerPrice {
    retailerName: string;
    price: number;
    matched: boolean;
}

export interface PriceHistoryPoint {
    scrapedAt: string;
    bestMatchedPrice: number | null;
    bestRetailer: string | null;
    retailerPrices: RetailerPrice[];
}

export interface PriceHistoryResponse {
    trackedQueryId: string;
    canonicalProductName: string;
    points: PriceHistoryPoint[];
}

// API Functions

export async function getCandidates(query: string): Promise<TrackingCandidateResponse> {
    const res = await fetch(
        `${API_URL}/api/tracking/candidates?query=${encodeURIComponent(query)}`
    );
    if (!res.ok) throw new Error('Failed to fetch tracking candidates');
    return res.json();
}

export async function createTrackedQuery(data: CreateTrackedQueryRequest): Promise<TrackedQuery> {
    const res = await fetch(`${API_URL}/api/tracking/queries`, {
        method: 'POST',
        headers: trackingHeaders(),
        body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to create tracked query');
    return res.json();
}

export async function getTrackedQueries(): Promise<TrackedQuery[]> {
    const res = await fetch(`${API_URL}/api/tracking/queries`, {
        headers: trackingHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch tracked queries');
    return res.json();
}

export async function getPriceHistory(id: string, days = 30): Promise<PriceHistoryResponse> {
    const res = await fetch(
        `${API_URL}/api/tracking/queries/${id}/history?days=${days}`,
        { headers: trackingHeaders() }
    );
    if (!res.ok) throw new Error('Failed to fetch price history');
    return res.json();
}

export async function deleteTrackedQuery(id: string): Promise<void> {
    const res = await fetch(`${API_URL}/api/tracking/queries/${id}`, {
        method: 'DELETE',
        headers: trackingHeaders(),
    });
    if (!res.ok) throw new Error('Failed to delete tracked query');
}
