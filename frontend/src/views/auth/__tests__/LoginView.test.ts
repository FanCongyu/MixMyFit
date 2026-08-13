import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import LoginView from '../LoginView.vue'

describe('LoginView', () => {
  afterEach(() => {
    cleanup()
    window.history.pushState({}, '', '/login')
    vi.unstubAllGlobals()
  })

  it('calls the login API and redirects to the app view after success', async () => {
    window.history.pushState({}, '', '/login')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ userId: 7, username: 'mia', nickname: 'Mia' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    )
    vi.stubGlobal('fetch', fetchMock)

    render(LoginView)

    await fireEvent.update(screen.getByLabelText('Username'), 'mia')
    await fireEvent.update(screen.getByLabelText('Password'), 'quiet-password')
    await fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', {
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      method: 'POST',
      body: JSON.stringify({ username: 'mia', password: 'quiet-password' })
    })
    await waitFor(() => {
      expect(window.location.pathname).toBe('/app')
    })
  })
})
