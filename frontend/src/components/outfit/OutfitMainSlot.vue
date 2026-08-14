<template>
  <article class="outfit-slot" role="region" :aria-label="`${label}槽位`">
    <header class="outfit-slot__header">
      <h2>{{ label }}</h2>
      <button type="button" :aria-label="`清空${label}`" @click="emit('clear')">
        清空
      </button>
    </header>

    <div class="outfit-slot__preview">
      <img
        v-if="selected"
        class="outfit-slot__image"
        :src="selected.imageUrl"
        :alt="displayName"
        loading="lazy"
      />
      <p v-else>未选择衣物</p>
    </div>

    <div v-if="selected" class="outfit-slot__body">
      <strong>{{ displayName }}</strong>
      <p v-if="selected.color">{{ selected.color }}</p>
    </div>

    <div class="outfit-slot__actions">
      <button
        type="button"
        :aria-label="`上一件${label}`"
        :disabled="!candidates.length"
        @click="emit('previous')"
      >
        上一件
      </button>
      <button
        type="button"
        :aria-label="`下一件${label}`"
        :disabled="!candidates.length"
        @click="emit('next')"
      >
        下一件
      </button>
      <button type="button" :aria-label="`选择${label}`" @click="emit('open-selector')">
        选择
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ClothingItem } from '../../api/clothing'

const props = defineProps<{
  label: string
  candidates: ClothingItem[]
  selected: ClothingItem | null
}>()

const emit = defineEmits<{
  clear: []
  next: []
  previous: []
  'open-selector': []
}>()

const displayName = computed(() => props.selected?.name?.trim() || '未选择衣物')
</script>
