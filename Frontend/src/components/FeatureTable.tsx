import { useState } from 'react';
import type { FeatureExtractionResponse, FeatureRow } from '../api/featureApi';
interface Props {
    response: FeatureExtractionResponse;
    onClose: () => void;
}

const RETAILER_COLORS: Record<string, string> = {
    Amazon: '#FF9900',
    Walmart: '#0071CE',
    Newegg: '#E2231A',
    eBay: '#86B817',
    Etsy: '#F1641E',
};

function reorderFeatures(features: FeatureRow[]): FeatureRow[] {
    const price = features.find(f => f.name.toLowerCase() === 'price');
    const condition = features.find(f => f.name.toLowerCase() === 'condition');
    const rest = features.filter(f => {
        const n = f.name.toLowerCase();
        return n !== 'price' && n !== 'condition';
    });
    return [...(price ? [price] : []), ...(condition ? [condition] : []), ...rest];
}

function isAllSame(row: FeatureRow): boolean {
    // A row is "all same" when at least two columns reported a value
    // and every reported value is identical. A row with zero or one
    // reported value is not "all same" — it has no signal to compare
    // against, and treating it as redundant hides potentially useful
    // information (a single column reporting a value the others didn't
    // mention is still informative for that column).
    const present = Object.values(row.values).filter(v => v != null && v !== '');
    if (present.length < 2) return false;
    return present.every(v => v === present[0]);
}

function shortProductName(name: string, maxLength: number = 50): string {
    if (!name) return '';
    if (name.length <= maxLength) return name;
    return name.slice(0, maxLength - 1) + '…';
}

function exportCsv(response: FeatureExtractionResponse): void {
    const rows = reorderFeatures(response.features);
    const header = ['Feature', ...response.columns.map(c => `${c.retailerName}: ${c.productName}`)];
    const lines = [header.map(quoteCsv).join(',')];
    for (const row of rows) {
        const cells = [quoteCsv(row.name)];
        for (const column of response.columns) {
            cells.push(quoteCsv(row.values[column.key] ?? ''));
        }
        lines.push(cells.join(','));
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'feature-comparison.csv';
    a.click();
    URL.revokeObjectURL(url);
}

function quoteCsv(value: string): string {
    if (value.includes(',') || value.includes('"') || value.includes('\n')) {
        return `"${value.replace(/"/g, '""')}"`;
    }
    return value;
}

export default function FeatureTable({ response, onClose }: Props) {
    const [hideAllSame, setHideAllSame] = useState(false);

    if (response.columns.length === 0) {
        return (
            <div className="compare-modal-overlay" onClick={onClose}>
                <div className="compare-modal-box" onClick={e => e.stopPropagation()}>
                    <h2>Feature comparison</h2>
                    <p className="compare-empty">No products to compare.</p>
                    <div className="compare-modal-actions">
                        <button type="button" className="btn-primary" onClick={onClose}>Close</button>
                    </div>
                </div>
            </div>
        );
    }

    const ordered = reorderFeatures(response.features);
    const visible = ordered.filter(row => {
        if (hideAllSame && isAllSame(row)) return false;
        return true;
    });

    return (
        <div className="compare-modal-overlay" onClick={onClose}>
            <div className="compare-modal-box compare-modal-box-wide" onClick={e => e.stopPropagation()}>
                <div className="compare-modal-header">
                    <div>
                        <h2>Feature comparison</h2>
                        <p className="compare-subtitle">
                            {response.columns.length} {response.columns.length === 1 ? 'product' : 'products'},
                            {' '}{visible.length} of {ordered.length} features
                        </p>
                    </div>
                    <div className="compare-modal-actions">
                        <button type="button" className="btn-secondary" onClick={() => exportCsv(response)}>
                            Export CSV
                        </button>
                        <button type="button" className="btn-primary" onClick={onClose}>
                            Close
                        </button>
                    </div>
                </div>

                {response.warnings.length > 0 && (
                    <ul className="compare-warnings">
                        {response.warnings.map((w, i) => (
                            <li key={i}>{w}</li>
                        ))}
                    </ul>
                )}

                <div className="compare-toolbar">
                    <label className="compare-toggle">
                        <input
                            type="checkbox"
                            checked={hideAllSame}
                            onChange={e => setHideAllSame(e.target.checked)}
                        />
                        <span>Hide identical rows</span>
                    </label>
                </div>

                <div className="compare-table-scroll">
                    <table className="compare-table">
                        <thead>
                            <tr>
                                <th scope="col" className="compare-feature-name-col">Feature</th>
                                {response.columns.map(column => (
                                    <th key={column.key} scope="col" className="compare-column-header">
                                        <span
                                            className="compare-column-retailer"
                                            style={{ color: RETAILER_COLORS[column.retailerName] || '#aaa' }}
                                        >
                                            {column.retailerName}
                                        </span>
                                        <span className="compare-column-product" title={column.productName}>
                                            {shortProductName(column.productName, 40)}
                                        </span>
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {visible.length === 0 ? (
                                <tr>
                                    <td colSpan={response.columns.length + 1} className="compare-empty-row">
                                        No features match the current filters.
                                    </td>
                                </tr>
                            ) : (
                                visible.map(row => {
                                    const isPrice = row.name.toLowerCase() === 'price';
                                    const isCondition = row.name.toLowerCase() === 'condition';
                                    const allSame = isAllSame(row);
                                    return (
                                        <tr
                                            key={row.name}
                                            className={[
                                                isPrice ? 'compare-row-price' : '',
                                                isCondition ? 'compare-row-condition' : '',
                                                allSame ? 'compare-row-allsame' : '',
                                            ].filter(Boolean).join(' ')}
                                        >
                                            <th scope="row" className="compare-feature-name-col">
                                                <span className="compare-feature-name">{row.name}</span>
                                                {allSame && !isPrice && !isCondition && (
                                                    <span className="compare-allsame-badge" title="All columns report the same value">
                                                        all same
                                                    </span>
                                                )}
                                            </th>
                                            {response.columns.map(column => {
                                                const value = row.values[column.key];
                                                const empty = value == null || value === '';
                                                return (
                                                    <td
                                                        key={column.key}
                                                        className={empty ? 'compare-cell-empty' : ''}
                                                    >
                                                        {empty ? '—' : value}
                                                        {isPrice && column.financed && (
                                                            <span className="financed-badge financed-badge-inline">Financed</span>
                                                        )}
                                                    </td>
                                                );
                                            })}
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
