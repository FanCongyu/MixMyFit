import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from '../../../App.vue'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

function clothingList(items: unknown[] = []): unknown {
  return {
    items,
    page: 0,
    size: 24,
    total: items.length
  }
}

function stubClothingPage(initialItems: unknown[] = []): ReturnType<typeof vi.fn> {
  const fetchMock = vi.fn((path: string) => {
    if (path === '/api/clothes/draft-count') {
      return Promise.resolve(jsonResponse({ count: 3 }))
    }
    if (path === '/api/categories') {
      return Promise.resolve(jsonResponse([
        { categoryId: 1, name: '上装', type: 'fixed' }
      ]))
    }
    if (path === '/api/clothing-tags') {
      return Promise.resolve(jsonResponse([
        { tagId: 11, name: '通勤', kind: 'clothing' }
      ]))
    }
    if (path === '/api/clothes/colors') {
      return Promise.resolve(jsonResponse(['白色']))
    }
    if (path.startsWith('/api/clothes')) {
      return Promise.resolve(jsonResponse(clothingList(initialItems)))
    }
    return Promise.resolve(jsonResponse({}))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('ClothingListView', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    window.history.pushState({}, '', '/')
  })

  it('renders the draft clothing count returned by the API', async () => {
    stubClothingPage()
    window.history.pushState({}, '', '/clothes')

    render(App)

    expect(await screen.findByText('3 件待完善')).toBeTruthy()
  })

  it('sends the draft status query when the draft filter is applied', async () => {
    const fetchMock = stubClothingPage()
    window.history.pushState({}, '', '/clothes')

    render(App)

    await fireEvent.update(await screen.findByLabelText('状态'), 'draft')

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('status=draft'),
        expect.any(Object)
      )
    })
  })

  it('shows a fallback label when clothing name is empty', async () => {
    stubClothingPage([
      {
        clothingId: 42,
        status: 'ready',
        name: '',
        color: '白色',
        category: { categoryId: 1, name: '上装', type: 'fixed' },
        seasons: ['spring'],
        tags: [{ tagId: 11, name: '通勤' }],
        imageUrl: '/api/clothes/42/image',
        originalFilename: 'shirt.png',
        contentType: 'image/png',
        fileSize: 12
      }
    ])
    window.history.pushState({}, '', '/clothes')

    render(App)

    expect(await screen.findByText('未命名衣物')).toBeTruthy()
  })
})
