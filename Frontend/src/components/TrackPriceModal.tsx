/**
 * WORKFLOW
 * open with query prop
 * getCandidates(query) on mount
 * If autoSelected or 1 candidate -> pre-select
 * Else show radio list of candidates
 * Confirm --> createTrackedQuery(...) --> close modal
 */

import { useState, useEffect } from 'react';
import { createTrackedQuery, getCandidates, type TrackingCandidate } from '../api/trackingApi';

interface Props {
    query: string;
    onClose: () => void;
}

export default function TrackPriceModal({ query, onClose }: Props) {
    const [candidates, setCandidates] = useState<TrackingCandidate[]>([]);
    const [autoSelected, setAutoSelected] = useState(false);
    const [selectedCandidate, setSelectedCandidate] = useState<TrackingCandidate | null>(null);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        let cancelled = false;

        async function load() {
            setLoading(true);
            setError('');
            setCandidates([]);
            setSelectedCandidate(null);
            setAutoSelected(false);

            try{
                const res = await getCandidates(query);
                if (cancelled) return;
                setCandidates(res.candidates);
                setAutoSelected(res.autoSelected);

                if (res.autoSelected || res.candidates.length === 1) {
                    setSelectedCandidate(res.candidates[0] ?? null);
                }
            }catch(e){
                if (!cancelled) setError('Failed to load product option');
            } finally {
                if (!cancelled) setLoading(false);
            }
        }
        load();
        return () => { cancelled = true; };
    }, [query]);

    async function handleConfirm() {
        if (!selectedCandidate) {
            setError('Please select a product option to track');
            return;
        }

        const email = localStorage.getItem('user_email');
        if (!email) {
            setError('Please login to track prices');
            return;
        }

        setSubmitting(true);
        setError('');
        try{
            await createTrackedQuery({
                rawQuery: query,
                canonicalProductKey: selectedCandidate.productKey,
                canonicalProductName: selectedCandidate.productName,
                referenceUrl: selectedCandidate.sampleUrl
            });
            setSuccess(true);
        } catch {
            setError('Failed to track price. Try again.');
        } finally {
            setSubmitting(false);
        }
    }

    const showRadioList = !loading && candidates.length > 1 && !autoSelected;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-box" onClick={(e) => e.stopPropagation()}>
                <h2>Track Price</h2>
                <p className="modal-query">{query}</p>
                {success ? (
                    <div className="modal-success">
                        <p>
                            Now tracking <strong>{selectedCandidate?.productName}</strong>.
                            Prices will be recorded on the next scrape run.
                        </p>
                        <button className="btn-primary" type="button" onClick={onClose}>Close</button>
                    </div>
                ) : (
                    <>
                        {loading && <p>Finding products...</p>}
                        {!loading && candidates.length === 0 && (
                            <p>No matching products found for this search.</p>
                        )}
                        {!loading && candidates.length > 0 && !showRadioList && selectedCandidate && (
                            <div>
                                <p><strong>{selectedCandidate.productName}</strong></p>
                                <p>From ${selectedCandidate.lowestPrice.toFixed(2)} · {selectedCandidate.retailers.join(', ')}</p>
                            </div>
                        )}
                        {showRadioList && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                                <label>Which product do you want to track?</label>
                                {candidates.map((candidate) => (
                                    <label key={candidate.productKey} style={{ display: 'flex', gap: '0.5rem', cursor: 'pointer' }}>
                                        <input
                                            type="radio"
                                            name="tracking-candidate"
                                            checked={selectedCandidate?.productKey === candidate.productKey}
                                            onChange={() => setSelectedCandidate(candidate)}
                                        />
                                        <span>
                                            {candidate.productName} — ${candidate.lowestPrice.toFixed(2)} ({candidate.retailers.join(', ')})
                                        </span>
                                    </label>
                                ))}
                            </div>
                        )}
                        {error && <p className="modal-error">{error}</p>}
                        <div className="modal-actions">
                            <button className="btn-secondary" type="button" onClick={onClose}>Cancel</button>
                            <button
                                className="btn-primary"
                                type="button"
                                onClick={handleConfirm}
                                disabled={loading || submitting || !selectedCandidate}
                            >
                                {submitting ? 'Tracking...' : 'Start Tracking'}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}