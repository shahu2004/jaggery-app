import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';

export default function BackendStatus() {
  const [status, setStatus] = useState('checking');
  const [message, setMessage] = useState('');
  const [checking, setChecking] = useState(false);

  const checkConnection = useCallback(async () => {
    setChecking(true);
    try {
      const [auth, catalog] = await Promise.all([
        api('/api/auth/ping'),
        api('/api/products?page=0&size=1'),
      ]);

      if (auth?.status !== 'running') {
        throw new Error('The authentication service did not return a healthy status.');
      }
      if (!Array.isArray(catalog?.content)) {
        throw new Error('The product catalogue API did not return a valid page.');
      }

      // The cart route is protected. A 401 means the Order Service route is
      // reachable and correctly enforcing authentication for signed-out users.
      try {
        await api('/api/cart');
      } catch (error) {
        if (error?.code !== 'UNAUTHORIZED') throw error;
      }

      setStatus('online');
      setMessage('Authentication, catalogue and order APIs are reachable.');
    } catch (error) {
      setStatus('offline');
      setMessage(error?.message || 'The backend services could not be reached.');
    } finally {
      setChecking(false);
    }
  }, []);

  useEffect(() => {
    checkConnection();
  }, [checkConnection]);

  if (status === 'online') {
    return (
      <div className="backend-status backend-status-online" role="status" aria-live="polite">
        <span className="backend-status-dot" aria-hidden="true" />
        <span>{message}</span>
      </div>
    );
  }

  if (status === 'checking') {
    return (
      <div className="backend-status backend-status-checking" role="status" aria-live="polite">
        <span className="backend-status-spinner" aria-hidden="true" />
        <span>Connecting to the Jaggery marketplace…</span>
      </div>
    );
  }

  return (
    <section className="backend-status backend-status-offline" role="alert" aria-live="assertive">
      <div className="backend-status-copy">
        <strong>Marketplace services are not connected</strong>
        <p>{message}</p>
        <p className="backend-status-help">
          For local development, open a terminal in the project root and run:
          <code>docker compose up --build</code>
        </p>
        <p className="backend-status-help">
          Keep the terminal running, then retry. On a hosted deployment, configure the frontend API URL or its server-side /api proxy to the public API Gateway.
        </p>
      </div>
      <button className="button button-secondary backend-retry" type="button" onClick={checkConnection} disabled={checking}>
        {checking ? 'Checking…' : 'Retry connection'}
      </button>
    </section>
  );
}
