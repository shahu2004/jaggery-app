const API_BASE = (import.meta.env?.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(message, { status = null, code = 'API_ERROR' } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

const backendStartupHelp =
  'Start local MySQL, then run Discovery Server, Auth Service, Product Service, Order Service and API Gateway in IntelliJ IDEA. ' +
  'Then start the React frontend from the frontend folder with: npm run dev. See INTELLIJ_LOCAL_SETUP.md in the repository root.';

export async function api(path, { token, method = 'GET', body, headers = {} } = {}) {
  const requestHeaders = { ...headers };
  const options = { method, headers: requestHeaders };

  if (token) requestHeaders.Authorization = `Bearer ${token}`;
  if (body instanceof FormData) {
    options.body = body;
  } else if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, options);
  } catch {
    throw new ApiError(
      `The Jaggery backend is not reachable. ${backendStartupHelp}`,
      { code: 'API_UNREACHABLE' },
    );
  }

  const raw = await response.text();
  let data = null;
  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = { message: raw };
    }
  }

  const proxyUnavailable =
    [502, 503, 504].includes(response.status) ||
    (response.status >= 500 && /ECONNREFUSED|proxy error|socket hang up|failed to proxy/i.test(raw));

  if (proxyUnavailable) {
    throw new ApiError(
      `The Jaggery API gateway or one of its services is unavailable. ${backendStartupHelp}`,
      { status: response.status, code: 'API_UNREACHABLE' },
    );
  }

  if (!response.ok) {
    const message = data?.message || data?.detail || data?.title || `Request failed (${response.status})`;
    throw new ApiError(message, {
      status: response.status,
      code: response.status === 401 ? 'UNAUTHORIZED' : 'API_ERROR',
    });
  }

  return response.status === 204 ? { ok: true } : data;
}

export function imageUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_BASE}${path.startsWith('/') ? '' : '/'}${path}`;
}

export function getApiBaseUrl() {
  return API_BASE || 'same-origin /api proxy';
}
