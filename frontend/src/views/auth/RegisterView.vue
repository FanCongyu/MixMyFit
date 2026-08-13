<template>
  <section class="auth-panel" aria-labelledby="register-title">
    <h1 id="register-title">Create account</h1>

    <form class="form-stack" @submit.prevent="submit">
      <label>
        Username
        <input v-model="username" autocomplete="username" name="username" required />
      </label>

      <label>
        Nickname
        <input v-model="nickname" autocomplete="nickname" name="nickname" />
      </label>

      <label>
        Password
        <input
          v-model="password"
          autocomplete="new-password"
          name="password"
          required
          type="password"
        />
      </label>

      <label>
        Confirm password
        <input
          v-model="confirmPassword"
          autocomplete="new-password"
          name="confirmPassword"
          required
          type="password"
        />
      </label>

      <p v-if="error" class="form-message form-message--error" role="alert">{{ error }}</p>
      <button type="submit">Create account</button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import { authStore } from '../../stores/auth'

const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')

async function submit(): Promise<void> {
  error.value = ''

  try {
    await authStore.register({
      username: username.value,
      nickname: nickname.value || undefined,
      password: password.value,
      confirmPassword: confirmPassword.value
    })
    window.history.pushState({}, '', '/app')
    window.dispatchEvent(new PopStateEvent('popstate'))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Registration failed.'
  }
}
</script>
