import { ref } from 'vue'

import { apiRequest } from '../api/client'

export type AuthUser = {
  userId: number
  username: string
  nickname?: string
}

type ApiError = {
  message?: string
}

async function readError(response: Response): Promise<string> {
  try {
    const body = await response.json() as ApiError
    return body.message || 'Request failed.'
  } catch {
    return 'Request failed.'
  }
}

async function requestJson<T>(path: string, body: unknown, method = 'POST'): Promise<T> {
  const response = await apiRequest(path, {
    method,
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  if (!response.ok) {
    throw new Error(await readError(response))
  }

  return await response.json() as T
}

async function requestNoContent(path: string, body: unknown): Promise<void> {
  const response = await apiRequest(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  if (!response.ok) {
    throw new Error(await readError(response))
  }
}

const user = ref<AuthUser | null>(null)

export const authStore = {
  user,

  setUser(nextUser: AuthUser): void {
    user.value = nextUser
  },

  clear(): void {
    user.value = null
  },

  async register(payload: {
    username: string
    password: string
    confirmPassword: string
    nickname?: string
  }): Promise<AuthUser> {
    const nextUser = await requestJson<AuthUser>('/auth/register', payload)
    user.value = nextUser
    return nextUser
  },

  async login(payload: { username: string; password: string }): Promise<AuthUser> {
    const nextUser = await requestJson<AuthUser>('/auth/login', payload)
    user.value = nextUser
    return nextUser
  },

  async logout(): Promise<void> {
    const response = await apiRequest('/auth/logout', { method: 'POST' })

    if (!response.ok) {
      throw new Error(await readError(response))
    }

    user.value = null
  },

  async loadProfile(): Promise<AuthUser> {
    const response = await apiRequest('/profile')

    if (!response.ok) {
      throw new Error(await readError(response))
    }

    const nextUser = await response.json() as AuthUser
    user.value = nextUser
    return nextUser
  },

  async updateNickname(nickname: string): Promise<AuthUser> {
    const nextUser = await requestJson<AuthUser>('/profile', { nickname }, 'PATCH')
    user.value = nextUser
    return nextUser
  },

  async changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
    await requestNoContent('/profile/password', payload)
  }
}
