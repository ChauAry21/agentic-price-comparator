const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export interface CreateAlertRequest {
    productQuery: string;
    thresholdPrice: number;
    email: string;
}

export interface Alert {
    id: string;
    productQuery: string;
    thresholdPrice: number;
    email: string;
    active: boolean;
    createdAt: string;
    triggeredAt: string | null;
}

export async function createAlert(data: CreateAlertRequest): Promise<void> {
    const res = await fetch(`${API_URL}/api/alerts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to create alert');
}

export async function getAlerts(): Promise<Alert[]> {
    const res = await fetch(`${API_URL}/api/alerts`);
    if (!res.ok) throw new Error('Failed to fetch alerts');
    return res.json();
}

export async function deleteAlert(id: string): Promise<void> {
    const res = await fetch(`${API_URL}/api/alerts/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Failed to delete alert');
}