import type { PriceResult } from '../api/featureApi';

interface Props {
    selected: PriceResult[];
    loading: boolean;
    error: string;
    onCompare: () => void;
    onClear: () => void;
    onRemove: (result: PriceResult) => void;
}

const MAX_SELECTION = 6;

export default function CompareTray({ selected, loading, error, onCompare, onClear, onRemove }: Props) {
    if (selected.length === 0) return null;

    const atLimit = selected.length >= MAX_SELECTION;
    const canCompare = selected.length >= 2 && !loading;

    return (
        <div className="compare-tray">
            <div className="compare-tray-summary">
                <div className="compare-tray-count">
                    <span className="compare-tray-count-number">{selected.length}</span>
                    <span className="compare-tray-count-label">
                        {selected.length === 1 ? 'product selected' : 'products selected'}
                    </span>
                    {atLimit && (
                        <span className="compare-tray-limit">limit reached</span>
                    )}
                </div>
                <div className="compare-tray-chips">
                    {selected.map(result => (
                        <span key={`${result.retailerName}-${result.url}`} className="compare-tray-chip">
                            <span className="compare-tray-chip-retailer">{result.retailerName}</span>
                            <span className="compare-tray-chip-name">
                                {result.productName.length > 40
                                    ? result.productName.slice(0, 40) + '…'
                                    : result.productName}
                            </span>
                            <button
                                type="button"
                                className="compare-tray-chip-remove"
                                onClick={() => onRemove(result)}
                                aria-label={`Remove ${result.productName}`}
                            >
                                ×
                            </button>
                        </span>
                    ))}
                </div>
            </div>
            <div className="compare-tray-actions">
                {error && <span className="compare-tray-error">{error}</span>}
                <button type="button" className="btn-secondary" onClick={onClear} disabled={loading}>
                    Clear
                </button>
                <button
                    type="button"
                    className="btn-primary"
                    onClick={onCompare}
                    disabled={!canCompare}
                    title={selected.length < 2 ? 'Select at least 2 products' : undefined}
                >
                    {loading ? 'Comparing…' : `Compare ${selected.length} items`}
                </button>
            </div>
        </div>
    );
}
