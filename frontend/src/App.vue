<template>
  <main class="app-shell">
    <section class="app-shell__workspace" aria-label="MixMyFit application shell">
      <h1>{{ route.title }}</h1>
    </section>
  </main>
</template>

<script setup lang="ts">
import { resolveRoute } from './router/routes'

const route = resolveRoute()
</script>
