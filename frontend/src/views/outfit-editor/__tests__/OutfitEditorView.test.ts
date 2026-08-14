import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from '../../../App.vue'

type CategoryFixture = {
  categoryId: number
  name: string
  type: string
}

const fixedCategories: CategoryFixture[] = [
  { categoryId: 1, name: '上装', type: 'fixed' },
  { categoryId: 2, name: '下装', type: 'fixed' },
  { categoryId: 3, name: '鞋子', type: 'fixed' },
  { categoryId: 4, name: '帽子', type: 'fixed' }
]
const customCategories: CategoryFixture[] = [
  { categoryId: 5, name: '项链', type: 'custom' },
  { categoryId: 6, name: '包', type: 'custom' }
]
const categories = [...fixedCategories, ...customCategories]

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

function clothingItem(clothingId: number, name: string, category: CategoryFixture): unknown {
  return {
    clothingId,
    status: 'ready',
    name,
    color: '白色',
    category,
    seasons: ['spring'],
    tags: [{ tagId: 11, name: '通勤', kind: 'clothing' }],
    imageUrl: `/api/clothes/${clothingId}/image`,
    originalFilename: `${name}.png`,
    contentType: 'image/png',
    fileSize: 12
  }
}

function stubOutfitEditor(): ReturnType<typeof vi.fn> {
  const fetchMock = vi.fn((path: string) => {
    if (path === '/api/categories') {
      return Promise.resolve(jsonResponse(categories))
    }
    if (path === '/api/clothing-tags') {
      return Promise.resolve(jsonResponse([
        { tagId: 11, name: '通勤', kind: 'clothing' },
        { tagId: 12, name: '旅行', kind: 'clothing' }
      ]))
    }
    if (path === '/api/clothes/colors') {
      return Promise.resolve(jsonResponse(['白色', '黑色']))
    }
    if (path.startsWith('/api/clothes')) {
      const url = new URL(path, 'http://local.test')
      const categoryId = url.searchParams.get('categoryId')
      const itemsByCategory: Record<string, unknown[]> = {
        '1': [
          clothingItem(101, '白衬衫', fixedCategories[0]),
          clothingItem(102, '蓝衬衫', fixedCategories[0])
        ],
        '2': [clothingItem(201, '黑长裤', fixedCategories[1])],
        '3': [],
        '4': [],
        '5': [clothingItem(501, '珍珠项链', customCategories[0])],
        '6': [clothingItem(601, '托特包', customCategories[1])]
      }

      return Promise.resolve(jsonResponse(clothingList(itemsByCategory[categoryId ?? ''] ?? [])))
    }

    return Promise.resolve(jsonResponse({}))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('OutfitEditorView', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    window.history.pushState({}, '', '/')
  })

  it('renders exactly the four fixed main slots', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    expect(await screen.findByRole('heading', { name: '搭配编辑器' })).toBeTruthy()
    expect(await screen.findAllByRole('region', { name: /槽位$/ })).toHaveLength(4)
    expect(screen.getByRole('region', { name: '上装槽位' })).toBeTruthy()
    expect(screen.getByRole('region', { name: '下装槽位' })).toBeTruthy()
    expect(screen.getByRole('region', { name: '鞋子槽位' })).toBeTruthy()
    expect(screen.getByRole('region', { name: '帽子槽位' })).toBeTruthy()
  })

  it('selects the next ready clothing item for the matching category', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const topSlot = await screen.findByRole('region', { name: '上装槽位' })
    expect(within(topSlot).getByText('白衬衫')).toBeTruthy()

    await fireEvent.click(within(topSlot).getByRole('button', { name: '下一件上装' }))

    expect(within(topSlot).getByText('蓝衬衫')).toBeTruthy()
  })

  it('sends category, color, season, and tags when filtering candidates', async () => {
    const fetchMock = stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const topSlot = await screen.findByRole('region', { name: '上装槽位' })
    await fireEvent.click(within(topSlot).getByRole('button', { name: '选择上装' }))
    await fireEvent.update(await screen.findByLabelText('候选颜色'), '白色')
    await fireEvent.update(screen.getByLabelText('候选季节'), 'spring')
    const tagSelect = screen.getByLabelText('候选标签') as HTMLSelectElement
    Array.from(tagSelect.options).forEach((option) => {
      option.selected = ['11', '12'].includes(option.value)
    })
    await fireEvent.change(tagSelect)

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([path]) => {
        const url = new URL(String(path), 'http://local.test')

        return url.pathname === '/api/clothes'
          && url.searchParams.get('categoryId') === '1'
          && url.searchParams.get('status') === 'ready'
          && url.searchParams.get('color') === '白色'
          && url.searchParams.get('season') === 'spring'
          && url.searchParams.get('tagIds') === '11,12'
      })).toBe(true)
    })
  })

  it('clears only the active slot', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const topSlot = await screen.findByRole('region', { name: '上装槽位' })
    const bottomSlot = await screen.findByRole('region', { name: '下装槽位' })
    expect(within(topSlot).getByText('白衬衫')).toBeTruthy()
    expect(within(bottomSlot).getByText('黑长裤')).toBeTruthy()

    await fireEvent.click(within(topSlot).getByRole('button', { name: '清空上装' }))

    expect(within(topSlot).getByText('未选择衣物')).toBeTruthy()
    expect(within(bottomSlot).getByText('黑长裤')).toBeTruthy()
  })

  it('lists only custom categories as accessory candidates', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const accessoryCandidates = await screen.findByRole('region', { name: '配饰候选' })

    expect(within(accessoryCandidates).getByRole('button', { name: '添加珍珠项链' })).toBeTruthy()
    expect(within(accessoryCandidates).getByRole('button', { name: '添加托特包' })).toBeTruthy()
    expect(within(accessoryCandidates).queryByRole('button', { name: '添加白衬衫' })).toBeNull()
  })

  it('keeps accessory position, size, and z-index in the save payload state after adding', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const accessoryCandidates = await screen.findByRole('region', { name: '配饰候选' })
    await fireEvent.click(within(accessoryCandidates).getByRole('button', { name: '添加珍珠项链' }))

    const accessoryLayer = screen.getByRole('region', { name: '配饰层' })
    const accessory = within(accessoryLayer).getByRole('article', { name: '配饰 珍珠项链' })
    await fireEvent.click(within(accessory).getByRole('button', { name: '右移珍珠项链' }))
    await fireEvent.click(within(accessory).getByRole('button', { name: '下移珍珠项链' }))
    await fireEvent.click(within(accessory).getByRole('button', { name: '上移一层珍珠项链' }))
    await fireEvent.update(within(accessory).getByLabelText('珍珠项链尺寸'), 'large')

    const payload = JSON.parse(screen.getByLabelText('配饰保存数据').textContent ?? '[]')
    expect(payload).toEqual([
      {
        clothingId: 501,
        role: 'accessory_overlay',
        positionX: 10,
        positionY: 10,
        size: 'large',
        zIndex: 2
      }
    ])
  })

  it('allows only small, medium, and large accessory sizes', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const accessoryCandidates = await screen.findByRole('region', { name: '配饰候选' })
    await fireEvent.click(within(accessoryCandidates).getByRole('button', { name: '添加珍珠项链' }))

    const sizeSelect = screen.getByLabelText('珍珠项链尺寸') as HTMLSelectElement

    expect(Array.from(sizeSelect.options).map((option) => option.value)).toEqual([
      'small',
      'medium',
      'large'
    ])
  })

  it('removes an added accessory from the layer and payload state', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const accessoryCandidates = await screen.findByRole('region', { name: '配饰候选' })
    await fireEvent.click(within(accessoryCandidates).getByRole('button', { name: '添加珍珠项链' }))

    const accessoryLayer = screen.getByRole('region', { name: '配饰层' })
    const accessory = within(accessoryLayer).getByRole('article', { name: '配饰 珍珠项链' })
    await fireEvent.click(within(accessory).getByRole('button', { name: '移除珍珠项链' }))

    expect(within(accessoryLayer).queryByRole('article', { name: '配饰 珍珠项链' })).toBeNull()
    expect(JSON.parse(screen.getByLabelText('配饰保存数据').textContent ?? '[]')).toEqual([])
  })

  it('updates accessory position when dragging inside the accessory layer', async () => {
    stubOutfitEditor()
    window.history.pushState({}, '', '/outfit-editor')

    render(App)

    const accessoryCandidates = await screen.findByRole('region', { name: '配饰候选' })
    await fireEvent.click(within(accessoryCandidates).getByRole('button', { name: '添加珍珠项链' }))

    const accessoryLayer = screen.getByRole('region', { name: '配饰层' })
    const accessory = within(accessoryLayer).getByRole('article', { name: '配饰 珍珠项链' })
    await fireEvent.dragStart(accessory)
    const dropEvent = new Event('drop', { bubbles: true, cancelable: true })
    Object.defineProperty(dropEvent, 'clientX', { value: 30 })
    Object.defineProperty(dropEvent, 'clientY', { value: 20 })
    await fireEvent(accessoryLayer, dropEvent)

    const payload = JSON.parse(screen.getByLabelText('配饰保存数据').textContent ?? '[]')
    expect(payload[0]).toEqual(expect.objectContaining({
      positionX: 30,
      positionY: 20
    }))
  })
})
