import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import ProfileView from '../ProfileView.vue'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

describe('ProfileView', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('renders the username loaded from the profile API', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      userId: 7,
      username: 'mia',
      nickname: 'Mia'
    })))

    render(ProfileView)

    expect(await screen.findByText('mia')).toBeTruthy()
    expect(screen.getByDisplayValue('Mia')).toBeTruthy()
  })

  it('shows success and error states when updating nickname', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ userId: 7, username: 'mia', nickname: 'Mia' }))
      .mockResolvedValueOnce(jsonResponse({ userId: 7, username: 'mia', nickname: 'Mimi' }))
      .mockResolvedValueOnce(jsonResponse({ code: 'INVALID_INPUT', message: 'Nickname is required' }, 400))
    vi.stubGlobal('fetch', fetchMock)

    render(ProfileView)

    await fireEvent.update(await screen.findByLabelText('Nickname'), 'Mimi')
    await fireEvent.click(screen.getByRole('button', { name: 'Save nickname' }))

    expect(await screen.findByText('Profile updated.')).toBeTruthy()
    expect(fetchMock).toHaveBeenCalledWith('/api/profile', {
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      method: 'PATCH',
      body: JSON.stringify({ nickname: 'Mimi' })
    })

    await fireEvent.update(screen.getByLabelText('Nickname'), '')
    await fireEvent.click(screen.getByRole('button', { name: 'Save nickname' }))

    expect(await screen.findByText('Nickname is required')).toBeTruthy()
  })

  it('requires old and new password before submitting password changes', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      userId: 7,
      username: 'mia',
      nickname: 'Mia'
    }))
    vi.stubGlobal('fetch', fetchMock)

    render(ProfileView)

    await screen.findByText('mia')
    await fireEvent.click(screen.getByRole('button', { name: 'Change password' }))

    expect(await screen.findByText('Old password is required.')).toBeTruthy()
    expect(screen.getByText('New password is required.')).toBeTruthy()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('shows success and error states when changing password without logging password values', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    const consoleLog = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ userId: 7, username: 'mia', nickname: 'Mia' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse({ code: 'BAD_PASSWORD', message: 'Old password is incorrect' }, 400))
    vi.stubGlobal('fetch', fetchMock)

    render(ProfileView)

    await fireEvent.update(await screen.findByLabelText('Old password'), 'old-secret')
    await fireEvent.update(screen.getByLabelText('New password'), 'new-secret')
    await fireEvent.click(screen.getByRole('button', { name: 'Change password' }))

    expect(await screen.findByText('Password changed.')).toBeTruthy()
    expect(fetchMock).toHaveBeenCalledWith('/api/profile/password', {
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      method: 'POST',
      body: JSON.stringify({ oldPassword: 'old-secret', newPassword: 'new-secret' })
    })

    await fireEvent.update(screen.getByLabelText('Old password'), 'bad-secret')
    await fireEvent.update(screen.getByLabelText('New password'), 'next-secret')
    await fireEvent.click(screen.getByRole('button', { name: 'Change password' }))

    expect(await screen.findByText('Old password is incorrect')).toBeTruthy()
    const logged = [...consoleError.mock.calls, ...consoleLog.mock.calls].flat().join(' ')
    expect(logged).not.toContain('old-secret')
    expect(logged).not.toContain('new-secret')
    expect(logged).not.toContain('bad-secret')
    expect(logged).not.toContain('next-secret')

    consoleError.mockRestore()
    consoleLog.mockRestore()
  })
})
