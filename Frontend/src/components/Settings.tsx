import { useState, useEffect } from 'react';
import { getAlerts, deleteAlert, type Alert } from '../api/alertApi';

export default function Settings() {
    const [tab, setTab] = useState<'account' | 'alerts' | 'notifications'>('account');
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const [loadingAlerts, setLoadingAlerts] = useState(false);
    const [notificationsEnabled, setNotificationsEnabled] = useState(
        localStorage.getItem('notifications_enabled') !== 'false'
    );

    async function loadAlerts() {
        setLoadingAlerts(true);
        try {
            const data = await getAlerts();
            setAlerts(data);
        } catch {
            setAlerts([]);
        } finally {
            setLoadingAlerts(false);
        }
    }

    useEffect(() => {
        if (tab === 'alerts') loadAlerts();
    }, [tab]);

    async function handleDelete(id: string) {
        await deleteAlert(id);
        setAlerts(prev => prev.filter(a => a.id !== id));
    }

    function toggleNotifications() {
        const next = !notificationsEnabled;
        setNotificationsEnabled(next);
        localStorage.setItem('notifications_enabled', String(next));
    }

    return (
        <div className="settings-panel">
            <h2>Settings</h2>
            <div className="settings-tabs">
                <button className={tab === 'account' ? 'active' : ''} onClick={() => setTab('account')}>Account</button>
                <button className={tab === 'alerts' ? 'active' : ''} onClick={() => setTab('alerts')}>My Alerts</button>
                <button className={tab === 'notifications' ? 'active' : ''} onClick={() => setTab('notifications')}>Notifications</button>
            </div>

            {tab === 'account' && (
                <div className="settings-content">
                    <p className="settings-label">Email</p>
                    <p className="settings-value">{localStorage.getItem('user_email') || 'Not logged in'}</p>
                    <p className="settings-label" style={{ marginTop: '16px' }}>App Version</p>
                    <p className="settings-value">PriceHawk AI v1.0</p>
                </div>
            )}

            {tab === 'alerts' && (
                <div className="settings-content">
                    {loadingAlerts ? (
                        <p className="settings-empty">Loading...</p>
                    ) : alerts.length === 0 ? (
                        <p className="settings-empty">No alerts set. Search for a product and click "Set Alert".</p>
                    ) : (
                        <table className="alerts-table">
                            <thead>
                            <tr>
                                <th>Product</th>
                                <th>Threshold</th>
                                <th>Email</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            {alerts.map(alert => (
                                <tr key={alert.id}>
                                    <td>{alert.productQuery}</td>
                                    <td>${alert.thresholdPrice}</td>
                                    <td>{alert.email}</td>
                                    <td>
                                            <span className={`alert-status ${alert.active ? 'active' : 'triggered'}`}>
                                                {alert.active ? 'Active' : 'Triggered'}
                                            </span>
                                    </td>
                                    <td>
                                        <button className="btn-delete" onClick={() => handleDelete(alert.id)}>Delete</button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {tab === 'notifications' && (
                <div className="settings-content">
                    <div className="notification-toggle">
                        <div>
                            <p className="settings-label">Email Notifications</p>
                            <p className="settings-hint">Receive an email when a price alert is triggered</p>
                        </div>
                        <button
                            className={`toggle-btn ${notificationsEnabled ? 'on' : 'off'}`}
                            onClick={toggleNotifications}
                        >
                            {notificationsEnabled ? 'ON' : 'OFF'}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}