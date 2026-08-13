import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import RegisterView from '../RegisterView.vue'

describe('RegisterView', () => {
  afterEach(() => {
    cleanup()
    window.history.pushState({}, '', '/register')
    vi.unstubAllGlobals()
  })

  it('calls the register API and redirects to the app view after success', async () => {
    window.history.pushState({}, '', '/register')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ userId: 8, username: 'nina', nickname: 'Nina' }),
        { status: 201, headers: { 'Content-Type': 'application/json' } }
      )
    )
    vi.stubGlobal('fetch', fetchMock)

    render(RegisterView)

    await fireEvent.update(screen.getByLabelText('Username'), 'nina')
    await fireEvent.update(screen.getByLabelText('Nickname'), 'Nina')
    await fireEvent.update(screen.getByLabelText('Password'), 'quiet-password')
    await fireEvent.update(screen.getByLabelText('Confirm password'), 'quiet-password')
    await fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/register', {
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      method: 'POST',
      body: JSON.stringify({
        username: 'nina',
        nickname: 'Nina',
        password: 'quiet-password',
        confirmPassword: 'quiet-password'
      })
    })
    await waitFor(() => {
      expect(window.location.pathname).toBe('/app')
    })
  })
})
