import { useState } from 'react';
import { createAlert } from '../api/alertApi';

interface Props {
    productQuery: string;
    onClose: () => void;
}

export default function AlertModal({ productQuery, onClose }: Props) {
    const [email, setEmail] = useState('');
    const [threshold, setThreshold] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState('');

    async function handleSubmit() {
        if (!email || !threshold) { setError('Both fields are required.'); return; }
        setLoading(true);
        setError('');
        try {
            await createAlert({ productQuery, thresholdPrice: parseFloat(threshold), email });
            setSuccess(true);
        } catch {
            setError('Failed to set alert. Try again.');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-box" onClick={(e) => e.stopPropagation()}>
                <h2>Set Price Alert</h2>
                <p className="modal-query">{productQuery}</p>
                {success ? (
                    <div className="modal-success">
                        <p>Alert set! You'll get an email when the price drops to your threshold.</p>
                        <button className="btn-primary" onClick={onClose}>Close</button>
                    </div>
                ) : (
                    <>
                        <label>Email</label>
                        <input type="email" placeholder="you@email.com" value={email} onChange={(e) => setEmail(e.target.value)} />
                        <label>Alert me when price drops below ($)</label>
                        <input type="number" placeholder="249.99" value={threshold} onChange={(e) => setThreshold(e.target.value)} />
                        {error && <p className="modal-error">{error}</p>}
                        <div className="modal-actions">
                            <button className="btn-secondary" onClick={onClose}>Cancel</button>
                            <button className="btn-primary" onClick={handleSubmit} disabled={loading}>
                                {loading ? 'Setting...' : 'Set Alert'}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}