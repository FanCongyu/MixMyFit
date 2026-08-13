import { apiRequest } from './client'

export type ClothingStatus = 'draft' | 'ready'

export type ClothingCategory = {
  categoryId: number
  name: string
  type: string
}

export type ClothingTag = {
  tagId: number
  name: string
  kind?: string
}

export type ClothingItem = {
  clothingId: number
  status: ClothingStatus
  name?: string | null
  color?: string | null
  category?: ClothingCategory | null
  seasons: string[]
  tags: ClothingTag[]
  imageUrl: string
  originalFilename: string
  contentType: string
  fileSize: number
}

export type ClothingListFilters = {
  page?: number
  size?: number
  categoryId?: string
  status?: string
  color?: string
  season?: string
  tagIds?: string[]
}

export type ClothingListResponse = {
  items: ClothingItem[]
  page: number
  size: number
  total: number
}

export type DraftCountResponse = {
  count: number
}

export type ClothingUploadResponse = {
  clothingId: number
  status?: ClothingStatus
  imageUrl?: string
  originalFilename?: string
  contentType?: string
  fileSize?: number
}

export type ClothingBatchUpdateRequest = {
  clothingIds: number[]
  categoryId?: number
  color?: string
  seasons?: string[]
  addTagIds?: number[]
  removeTagIds?: number[]
}

export type ClothingBatchResponse = {
  updated: number
}

async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await apiRequest(path, init)

  if (!response.ok) {
    throw new Error('Request failed.')
  }

  return await response.json() as T
}

export function listClothes(filters: ClothingListFilters = {}): Promise<ClothingListResponse> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 24))

  if (filters.categoryId) {
    params.set('categoryId', filters.categoryId)
  }
  if (filters.status) {
    params.set('status', filters.status)
  }
  if (filters.color) {
    params.set('color', filters.color)
  }
  if (filters.season) {
    params.set('season', filters.season)
  }
  if (filters.tagIds?.length) {
    params.set('tagIds', filters.tagIds.join(','))
  }

  return requestJson<ClothingListResponse>(`/clothes?${params}`)
}

export function getDraftCount(): Promise<DraftCountResponse> {
  return requestJson<DraftCountResponse>('/clothes/draft-count')
}

export function listCategories(): Promise<ClothingCategory[]> {
  return requestJson<ClothingCategory[]>('/categories')
}

export function listClothingTags(): Promise<ClothingTag[]> {
  return requestJson<ClothingTag[]>('/clothing-tags')
}

export function listClothingColors(): Promise<string[]> {
  return requestJson<string[]>('/clothes/colors')
}

export function uploadClothing(file: File): Promise<ClothingUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)

  return requestJson<ClothingUploadResponse>('/clothes', {
    method: 'POST',
    body: formData
  })
}

export function batchUpdateClothes(
  request: ClothingBatchUpdateRequest
): Promise<ClothingBatchResponse> {
  return requestJson<ClothingBatchResponse>('/clothes/batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
}
