<template>
  <section class="profile-panel" aria-labelledby="profile-title">
    <h1 id="profile-title">Profile</h1>

    <p v-if="loading">Loading profile...</p>
    <p v-else-if="loadError" class="form-message form-message--error" role="alert">
      {{ loadError }}
    </p>

    <div v-else-if="authStore.user.value" class="profile-panel__content">
      <dl>
        <div>
          <dt>Username</dt>
          <dd>{{ authStore.user.value.username }}</dd>
        </div>
      </dl>

      <form class="form-stack" @submit.prevent="saveNickname">
        <label>
          Nickname
          <input v-model="nickname" name="nickname" />
        </label>

        <p v-if="nicknameMessage" class="form-message">{{ nicknameMessage }}</p>
        <p v-if="nicknameError" class="form-message form-message--error" role="alert">
          {{ nicknameError }}
        </p>
        <button type="submit">Save nickname</button>
      </form>

      <form class="form-stack" @submit.prevent="savePassword">
        <label>
          Old password
          <input
            v-model="oldPassword"
            autocomplete="current-password"
            name="oldPassword"
            type="password"
          />
        </label>
        <p v-if="oldPasswordError" class="form-message form-message--error">
          {{ oldPasswordError }}
        </p>

        <label>
          New password
          <input
            v-model="newPassword"
            autocomplete="new-password"
            name="newPassword"
            type="password"
          />
        </label>
        <p v-if="newPasswordError" class="form-message form-message--error">
          {{ newPasswordError }}
        </p>

        <p v-if="passwordMessage" class="form-message">{{ passwordMessage }}</p>
        <p v-if="passwordError" class="form-message form-message--error" role="alert">
          {{ passwordError }}
        </p>
        <button type="submit">Change password</button>
      </form>

      <button class="button-secondary" type="button" @click="logout">Log out</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { authStore } from '../../stores/auth'

const loading = ref(true)
const loadError = ref('')
const nickname = ref('')
const nicknameMessage = ref('')
const nicknameError = ref('')
const oldPassword = ref('')
const newPassword = ref('')
const oldPasswordError = ref('')
const newPasswordError = ref('')
const passwordMessage = ref('')
const passwordError = ref('')

onMounted(async () => {
  try {
    const profile = await authStore.loadProfile()
    nickname.value = profile.nickname || ''
  } catch (cause) {
    loadError.value = cause instanceof Error ? cause.message : 'Could not load profile.'
  } finally {
    loading.value = false
  }
})

async function saveNickname(): Promise<void> {
  nicknameMessage.value = ''
  nicknameError.value = ''

  try {
    const profile = await authStore.updateNickname(nickname.value)
    nickname.value = profile.nickname || ''
    nicknameMessage.value = 'Profile updated.'
  } catch (cause) {
    nicknameError.value = cause instanceof Error ? cause.message : 'Could not update profile.'
  }
}

async function savePassword(): Promise<void> {
  oldPasswordError.value = oldPassword.value ? '' : 'Old password is required.'
  newPasswordError.value = newPassword.value ? '' : 'New password is required.'
  passwordMessage.value = ''
  passwordError.value = ''

  if (oldPasswordError.value || newPasswordError.value) {
    return
  }

  try {
    await authStore.changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    oldPassword.value = ''
    newPassword.value = ''
    passwordMessage.value = 'Password changed.'
  } catch (cause) {
    passwordError.value = cause instanceof Error ? cause.message : 'Could not change password.'
  }
}

async function logout(): Promise<void> {
  await authStore.logout()
  window.history.pushState({}, '', '/login')
  window.dispatchEvent(new PopStateEvent('popstate'))
}
</script>
