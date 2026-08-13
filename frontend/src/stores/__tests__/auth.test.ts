import { afterEach, describe, expect, it, vi } from 'vitest'

import { authStore } from '../auth'

describe('authStore', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    authStore.clear()
  })

  it('logs out through the API and clears local authentication state', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    authStore.setUser({ userId: 7, username: 'mia', nickname: 'Mia' })

    await authStore.logout()

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/logout', {
      credentials: 'include',
      headers: {
        Accept: 'application/json'
      },
      method: 'POST'
    })
    expect(authStore.user.value).toBeNull()
  })
})
