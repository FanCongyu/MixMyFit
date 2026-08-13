import { afterEach, describe, expect, it, vi } from 'vitest'

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
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
})
