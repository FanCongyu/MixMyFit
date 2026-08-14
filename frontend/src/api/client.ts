const API_PREFIX = '/api'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

function apiPath(path: string): string {
  const prefixedPath = path.startsWith(API_PREFIX) ? path : `${API_PREFIX}${path}`
  return API_BASE_URL ? `${API_BASE_URL}${prefixedPath}` : prefixedPath
}

export function apiRequest(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = {
    Accept: 'application/json',
    ...init.headers
  }

  return fetch(apiPath(path), {
    ...init,
    credentials: 'include',
    headers
  })
}
