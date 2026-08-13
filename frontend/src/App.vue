<template>
  <main class="app-shell">
    <section class="app-shell__workspace" aria-label="MixMyFit application shell">
      <LoginView v-if="route.path === '/login'" />
      <RegisterView v-else-if="route.path === '/register'" />
      <ProfileView v-else-if="route.path === '/profile'" />
      <h1 v-else>{{ route.title }}</h1>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { resolveRoute } from './router/routes'
import LoginView from './views/auth/LoginView.vue'
import RegisterView from './views/auth/RegisterView.vue'
import ProfileView from './views/profile/ProfileView.vue'

const pathname = ref(window.location.pathname)
const route = computed(() => resolveRoute(pathname.value))

function syncPathname(): void {
  pathname.value = window.location.pathname
}

onMounted(() => {
  window.addEventListener('popstate', syncPathname)
})

onUnmounted(() => {
  window.removeEventListener('popstate', syncPathname)
})
</script>
