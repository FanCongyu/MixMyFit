<template>
  <article class="clothing-card">
    <div class="clothing-card__image-frame">
      <label class="clothing-card__select">
        <input
          type="checkbox"
          :aria-label="`选择 ${displayName}`"
          :checked="selected"
          @change="emit('toggle-selected', item.clothingId)"
        />
      </label>
      <img
        class="clothing-card__image"
        :alt="displayName"
        :src="item.imageUrl"
        loading="lazy"
      />
      <span v-if="item.status === 'draft'" class="clothing-card__status">待完善</span>
    </div>

    <div class="clothing-card__body">
      <h2>{{ displayName }}</h2>
      <p v-if="item.category">{{ item.category.name }}</p>
      <p v-else>未设置品类</p>
      <p v-if="item.color">{{ item.color }}</p>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ClothingItem } from '../../api/clothing'

const props = defineProps<{
  item: ClothingItem
  selected?: boolean
}>()

const emit = defineEmits<{
  'toggle-selected': [clothingId: number]
}>()

const displayName = computed(() => props.item.name?.trim() || '未命名衣物')
</script>
