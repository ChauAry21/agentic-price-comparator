// Props: points from history api.
// simple recharts LineChart with bestMatchedPrice over scrapedAt. Use connectNulls={false} for gaps

import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import type { PriceHistoryPoint } from '../api/trackingApi';

interface Props {
    points: PriceHistoryPoint[];
}

function formatAxisDate(value: string) {
    return new Date(value).toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric' 
    });
}

function formatTooltipDate(value: number) {
    return new Date(value).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

export default function PriceHistoryChart({ points }: Props) {
    if (points.length === 0) {
        return <div className="price-history-chart">No price history available</div>;
    }

    return (
        <ResponsiveContainer width="100%" height={300}>
            <LineChart data={points} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="scrapedAt" tickFormatter={formatAxisDate} minTickGap={24} />
                <YAxis tickFormatter={(value: number) => `$${value.toFixed(2)}`} />
                <Tooltip
                    labelFormatter={formatTooltipDate}
                    formatter={(value: number | null) => 
                        value == null ? ['No match', 'Best Price'] : [`$${value.toFixed(2)}`, 'Best Price']
                    }
                />
                <Legend />
                <Line 
                    type="monotone" 
                    dataKey="bestMatchedPrice" 
                    stroke="#8884d8" 
                    connectNulls={false}
                />
            </LineChart>
        </ResponsiveContainer>
    );
}