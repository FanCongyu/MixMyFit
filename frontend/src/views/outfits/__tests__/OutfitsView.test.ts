import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from '../../../App.vue'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

function stubOutfits(): ReturnType<typeof vi.fn> {
  const fetchMock = vi.fn((path: string, init?: RequestInit) => {
    if (path === '/api/outfit-tags') {
      return Promise.resolve(jsonResponse([
        { tagId: 301, name: '通勤', kind: 'outfit' },
        { tagId: 302, name: '旅行', kind: 'outfit' }
      ]))
    }
    if (path.startsWith('/api/outfits/1') && init?.method === 'DELETE') {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    if (path.startsWith('/api/outfits/1') && init?.method === 'PATCH') {
      return Promise.resolve(jsonResponse({
        outfitId: 1,
        title: '周一通勤',
        note: '办公室',
        seasons: ['spring'],
        tags: [{ tagId: 301, name: '通勤' }],
        items: [
          {
            clothingId: 501,
            role: 'accessory_overlay',
            slot: null,
            positionX: 10,
            positionY: 0,
            size: 'medium',
            zIndex: 2
          }
        ]
      }))
    }
    if (path.startsWith('/api/outfits/1')) {
      return Promise.resolve(jsonResponse({
        outfitId: 1,
        title: '周一通勤',
        note: '办公室',
        seasons: ['spring'],
        tags: [{ tagId: 301, name: '通勤' }],
        items: [
          {
            clothingId: 101,
            role: 'main_slot',
            slot: 'top',
            positionX: null,
            positionY: null,
            size: null,
            zIndex: null
          },
          {
            clothingId: 501,
            role: 'accessory_overlay',
            slot: null,
            positionX: 10,
            positionY: 0,
            size: 'medium',
            zIndex: 2
          }
        ]
      }))
    }
    if (path.startsWith('/api/outfits')) {
      return Promise.resolve(jsonResponse({
        items: [
          { outfitId: 1, title: '周一通勤', note: '办公室' },
          { outfitId: 2, title: '周末旅行', note: '短途' }
        ],
        page: 0,
        size: 20,
        total: 2
      }))
    }

    return Promise.resolve(jsonResponse({}))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('OutfitsView', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    window.history.pushState({}, '', '/')
  })

  it('sends tag and season query parameters when filtering outfits', async () => {
    const fetchMock = stubOutfits()
    window.history.pushState({}, '', '/outfits')

    render(App)

    expect(await screen.findByRole('heading', { name: '搭配方案' })).toBeTruthy()
    expect(await screen.findByRole('option', { name: '通勤' })).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('筛选季节'), 'spring')
    await fireEvent.update(screen.getByLabelText('筛选标签'), '301')

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([path]) => {
        const url = new URL(String(path), 'http://local.test')

        return url.pathname === '/api/outfits'
          && url.searchParams.get('season') === 'spring'
          && url.searchParams.get('tagIds') === '301'
      })).toBe(true)
    })
  })

  it('requires confirmation before deleting an outfit', async () => {
    const fetchMock = stubOutfits()
    vi.stubGlobal('confirm', vi.fn()
      .mockReturnValueOnce(false)
      .mockReturnValueOnce(true))
    window.history.pushState({}, '', '/outfits')

    render(App)

    const list = await screen.findByRole('list', { name: '搭配方案列表' })
    const item = within(list).getByRole('listitem', { name: '周一通勤' })

    await fireEvent.click(within(item).getByRole('button', { name: '删除周一通勤' }))
    expect(fetchMock.mock.calls.some(([path, init]) =>
      path === '/api/outfits/1' && init?.method === 'DELETE'
    )).toBe(false)

    await fireEvent.click(within(item).getByRole('button', { name: '删除周一通勤' }))

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([path, init]) =>
        path === '/api/outfits/1' && init?.method === 'DELETE'
      )).toBe(true)
    })
  })

  it('allows editing outfit content by removing an item before saving', async () => {
    const fetchMock = stubOutfits()
    window.history.pushState({}, '', '/outfits')

    render(App)

    const list = await screen.findByRole('list', { name: '搭配方案列表' })
    const item = within(list).getByRole('listitem', { name: '周一通勤' })
    await fireEvent.click(within(item).getByRole('button', { name: '查看周一通勤' }))

    const detail = await screen.findByRole('region', { name: '搭配详情' })
    expect(within(detail).getByRole('list', { name: '搭配内容' })).toBeTruthy()
    await fireEvent.click(within(detail).getByRole('button', { name: '移除衣物101' }))
    await fireEvent.click(within(detail).getByRole('button', { name: '保存修改' }))

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(([path, init]) =>
        path === '/api/outfits/1' && init?.method === 'PATCH'
      )
      expect(updateCall).toBeTruthy()
      expect(JSON.parse(String(updateCall?.[1]?.body)).items).toEqual([
        {
          clothingId: 501,
          role: 'accessory_overlay',
          slot: null,
          positionX: 10,
          positionY: 0,
          size: 'medium',
          zIndex: 2
        }
      ])
    })
  })
})
