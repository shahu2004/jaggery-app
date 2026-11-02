import test, { after } from 'node:test';
import assert from 'node:assert/strict';
import { api, getApiBaseUrl } from './api.js';

const originalFetch = globalThis.fetch;
after(() => {
  globalThis.fetch = originalFetch;
});

test('uses same-origin API requests unless an explicit API URL is configured', () => {
  assert.equal(getApiBaseUrl(), 'same-origin /api proxy');
});

test('returns parsed JSON and attaches a bearer token', async () => {
  let capturedUrl = '';
  let capturedAuthorization = '';
  globalThis.fetch = async (url, options) => {
    capturedUrl = String(url);
    capturedAuthorization = options.headers.Authorization;
    return new Response(JSON.stringify({ id: 7 }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  };

  const result = await api('/api/users/me', { token: 'test-token' });

  assert.equal(capturedUrl, '/api/users/me');
  assert.equal(capturedAuthorization, 'Bearer test-token');
  assert.deepEqual(result, { id: 7 });
});

test('converts network failures into actionable backend setup guidance', async () => {
  globalThis.fetch = async () => {
    throw new TypeError('Failed to fetch');
  };

  await assert.rejects(api('/api/products'), (error) => {
    assert.equal(error.code, 'API_UNREACHABLE');
    assert.match(error.message, /docker compose up --build/);
    return true;
  });
});

test('preserves API validation and authentication errors', async () => {
  globalThis.fetch = async () => new Response(
    JSON.stringify({ message: 'Authentication required' }),
    { status: 401, headers: { 'Content-Type': 'application/json' } },
  );

  await assert.rejects(api('/api/cart'), (error) => {
    assert.equal(error.status, 401);
    assert.equal(error.code, 'UNAUTHORIZED');
    assert.equal(error.message, 'Authentication required');
    return true;
  });
});
