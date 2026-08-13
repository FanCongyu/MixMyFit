<template>
  <section class="auth-panel" aria-labelledby="login-title">
    <h1 id="login-title">Log in</h1>

    <form class="form-stack" @submit.prevent="submit">
      <label>
        Username
        <input v-model="username" autocomplete="username" name="username" required />
      </label>

      <label>
        Password
        <input
          v-model="password"
          autocomplete="current-password"
          name="password"
          required
          type="password"
        />
      </label>

      <p v-if="error" class="form-message form-message--error" role="alert">{{ error }}</p>
      <button type="submit">Log in</button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import { authStore } from '../../stores/auth'

const username = ref('')
const password = ref('')
const error = ref('')

async function submit(): Promise<void> {
  error.value = ''

  try {
    await authStore.login({
      username: username.value,
      password: password.value
    })
    window.history.pushState({}, '', '/app')
    window.dispatchEvent(new PopStateEvent('popstate'))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Login failed.'
  }
}
</script>
