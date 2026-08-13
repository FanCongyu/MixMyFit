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

function sampleClothingItem(clothingId: number, name: string): unknown {
  return {
    clothingId,
    status: 'draft',
    name,
    color: '白色',
    category: null,
    seasons: [],
    tags: [],
    imageUrl: `/api/clothes/${clothingId}/image`,
    originalFilename: `${name}.png`,
    contentType: 'image/png',
    fileSize: 12
  }
}

function stubClothingPage(
  initialItems: unknown[] = [],
  options: { draftCounts?: number[] } = {}
): ReturnType<typeof vi.fn> {
  const draftCounts = [...(options.draftCounts ?? [3])]
  const fetchMock = vi.fn((path: string, init?: RequestInit) => {
    if (path === '/api/clothes/draft-count') {
      return Promise.resolve(jsonResponse({ count: draftCounts.shift() ?? 3 }))
    }
    if (path === '/api/categories') {
      return Promise.resolve(jsonResponse([
        { categoryId: 1, name: '上装', type: 'fixed' }
      ]))
    }
    if (path === '/api/clothing-tags') {
      return Promise.resolve(jsonResponse([
        { tagId: 11, name: '通勤', kind: 'clothing' },
        { tagId: 12, name: '旅行', kind: 'clothing' }
      ]))
    }
    if (path === '/api/clothes/colors') {
      return Promise.resolve(jsonResponse(['白色']))
    }
    if (path === '/api/clothes/batch') {
      return Promise.resolve(jsonResponse({ updated: 2 }))
    }
    if (path === '/api/clothes' && init?.method === 'POST') {
      return Promise.resolve(jsonResponse({ clothingId: 99 }))
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

  it('enables the batch toolbar after selecting multiple clothing items', async () => {
    stubClothingPage([
      sampleClothingItem(41, '白衬衫'),
      sampleClothingItem(42, '黑外套')
    ])
    window.history.pushState({}, '', '/clothes')

    render(App)

    await fireEvent.click(await screen.findByLabelText('选择 白衬衫'))
    await fireEvent.click(await screen.findByLabelText('选择 黑外套'))

    expect(screen.getByRole('toolbar', { name: '批量操作' })).toBeTruthy()
    expect(screen.getByText('已选择 2 件')).toBeTruthy()
    expect((screen.getByLabelText('批量品类') as HTMLSelectElement).disabled).toBe(false)
  })

  it('sends selected clothing ids when batch category is applied', async () => {
    const fetchMock = stubClothingPage([
      sampleClothingItem(41, '白衬衫'),
      sampleClothingItem(42, '黑外套')
    ])
    window.history.pushState({}, '', '/clothes')

    render(App)

    await fireEvent.click(await screen.findByLabelText('选择 白衬衫'))
    await fireEvent.click(await screen.findByLabelText('选择 黑外套'))
    await fireEvent.update(screen.getByLabelText('批量品类'), '1')
    await fireEvent.click(screen.getByRole('button', { name: '批量设置品类' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/api/clothes/batch', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          clothingIds: [41, 42],
          categoryId: 1
        })
      }))
    })
  })

  it('sends selected clothing ids when batch color and season are applied', async () => {
    const fetchMock = stubClothingPage([
      sampleClothingItem(41, '白衬衫'),
      sampleClothingItem(42, '黑外套')
    ])
    window.history.pushState({}, '', '/clothes')

    render(App)

    await fireEvent.click(await screen.findByLabelText('选择 白衬衫'))
    await fireEvent.click(await screen.findByLabelText('选择 黑外套'))
    await fireEvent.update(screen.getByLabelText('批量颜色'), '白色')
    await fireEvent.update(screen.getByLabelText('批量季节'), 'spring')
    await fireEvent.click(screen.getByRole('button', { name: '批量设置属性' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/api/clothes/batch', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          clothingIds: [41, 42],
          color: '白色',
          seasons: ['spring']
        })
      }))
    })
  })

  it('sends selected clothing ids when batch tags are added and removed', async () => {
    const fetchMock = stubClothingPage([
      sampleClothingItem(41, '白衬衫'),
      sampleClothingItem(42, '黑外套')
    ])
    window.history.pushState({}, '', '/clothes')

    render(App)

    await fireEvent.click(await screen.findByLabelText('选择 白衬衫'))
    await fireEvent.click(await screen.findByLabelText('选择 黑外套'))
    await fireEvent.update(screen.getByLabelText('批量添加标签'), '11')
    await fireEvent.update(screen.getByLabelText('批量移除标签'), '12')
    await fireEvent.click(screen.getByRole('button', { name: '批量更新标签' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/api/clothes/batch', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          clothingIds: [41, 42],
          addTagIds: [11],
          removeTagIds: [12]
        })
      }))
    })
  })

  it('shows created count and refreshes draft count after batch upload succeeds', async () => {
    stubClothingPage([], { draftCounts: [3, 5] })
    window.history.pushState({}, '', '/clothes')

    render(App)

    const files = [
      new File(['shirt'], 'shirt.png', { type: 'image/png' }),
      new File(['coat'], 'coat.png', { type: 'image/png' })
    ]
    const input = await screen.findByLabelText('批量上传图片')
    Object.defineProperty(input, 'files', {
      configurable: true,
      value: files
    })
    await fireEvent(input, new Event('change', { bubbles: true }))

    expect(await screen.findByText('已创建 2 件衣物')).toBeTruthy()
    expect(await screen.findByText('5 件待完善')).toBeTruthy()
  })
})
