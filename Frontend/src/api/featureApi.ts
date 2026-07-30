import { BASE_URL } from './priceApi';
import type { PriceResult } from './priceApi';

export type { PriceResult } from './priceApi';

export interface Column {
    key: string;
    retailerName: string;
    productName: string;
    financed?: boolean;
}

export interface FeatureRow {
    name: string;
    values: Record<string, string | null>;
}

export interface FeatureExtractionResponse {
    columns: Column[];
    features: FeatureRow[];
    warnings: string[];
}

export async function extractFeatures(products: PriceResult[]): Promise<FeatureExtractionResponse> {
    const response = await fetch(`${BASE_URL}/api/prices/extract-features`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ products }),
    });
    if (!response.ok) {
        throw new Error(`Feature extraction failed: ${response.statusText}`);
    }
    return response.json();
}
