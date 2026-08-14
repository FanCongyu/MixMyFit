import { apiRequest } from './client'

export type OutfitTag = {
  tagId: number
  name: string
  kind?: string
}

export type OutfitItemPayload = {
  clothingId: number
  role: string
  slot: string | null
  positionX: number | null
  positionY: number | null
  size: string | null
  zIndex: number | null
}

export type OutfitSaveRequest = {
  title?: string
  note?: string
  seasons?: string[]
  tagIds?: number[]
  items: OutfitItemPayload[]
}

export type OutfitCreateResponse = {
  outfitId: number
  title: string
}

export type OutfitSummary = {
  outfitId: number
  title: string
  note?: string | null
}

export type OutfitListResponse = {
  items: OutfitSummary[]
  page: number
  size: number
  total: number
}

export type OutfitDetail = {
  outfitId: number
  title: string
  note?: string | null
  seasons: string[]
  tags: OutfitTag[]
  items: OutfitItemPayload[]
}

export type OutfitListFilters = {
  page?: number
  size?: number
  season?: string
  tagIds?: string[]
}

async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await apiRequest(path, init)

  if (!response.ok) {
    throw new Error('Request failed.')
  }

  return await response.json() as T
}

export function listOutfitTags(): Promise<OutfitTag[]> {
  return requestJson<OutfitTag[]>('/outfit-tags')
}

export function createOutfit(request: OutfitSaveRequest): Promise<OutfitCreateResponse> {
  return requestJson<OutfitCreateResponse>('/outfits', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
}

export function listOutfits(filters: OutfitListFilters = {}): Promise<OutfitListResponse> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))

  if (filters.season) {
    params.set('season', filters.season)
  }
  if (filters.tagIds?.length) {
    params.set('tagIds', filters.tagIds.join(','))
  }

  return requestJson<OutfitListResponse>(`/outfits?${params}`)
}

export function getOutfit(outfitId: number): Promise<OutfitDetail> {
  return requestJson<OutfitDetail>(`/outfits/${outfitId}`)
}

export function updateOutfit(outfitId: number, request: OutfitSaveRequest): Promise<OutfitDetail> {
  return requestJson<OutfitDetail>(`/outfits/${outfitId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
}

export async function deleteOutfit(outfitId: number): Promise<void> {
  const response = await apiRequest(`/outfits/${outfitId}`, {
    method: 'DELETE'
  })

  if (!response.ok) {
    throw new Error('Request failed.')
  }
}
