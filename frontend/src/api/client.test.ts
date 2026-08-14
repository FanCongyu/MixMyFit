import { afterEach, describe, expect, it, vi } from 'vitest'

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
    vi.resetModules()
  })

  it('sends requests with cookie credentials', async () => {
    const client = await import('./client').catch(() => undefined)
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    expect(client?.apiRequest).toEqual(expect.any(Function))

    await client?.apiRequest('/profile')

    expect(fetchMock).toHaveBeenCalledWith('/api/profile', {
      credentials: 'include',
      headers: {
        Accept: 'application/json'
      }
    })
  })

  it('uses VITE_API_BASE_URL for deployed backends', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://backend.example.com/')
    const client = await import('./client')
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await client.apiRequest('/profile')

    expect(fetchMock).toHaveBeenCalledWith('https://backend.example.com/api/profile', {
      credentials: 'include',
      headers: {
        Accept: 'application/json'
      }
    })
  })
})
