const API_PREFIX = '/api'

function apiPath(path: string): string {
  return path.startsWith(API_PREFIX) ? path : `${API_PREFIX}${path}`
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
