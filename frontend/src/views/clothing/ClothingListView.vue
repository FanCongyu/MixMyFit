<template>
  <section class="clothing-view" aria-labelledby="clothing-title">
    <header class="clothing-view__header">
      <div>
        <h1 id="clothing-title">衣物库</h1>
        <p>{{ list.total }} 件衣物</p>
      </div>
      <strong class="clothing-view__draft-count">{{ draftCount }} 件待完善</strong>
    </header>

    <p v-if="error" class="form-message form-message--error" role="alert">{{ error }}</p>

    <ClothingFilters
      v-model="filters"
      :categories="categories"
      :colors="colors"
      :tags="tags"
    />

    <p v-if="loading">正在加载衣物...</p>

    <div v-else-if="list.items.length" class="clothing-grid" aria-label="衣物列表">
      <ClothingCard v-for="item in list.items" :key="item.clothingId" :item="item" />
    </div>

    <p v-else class="clothing-view__empty">暂无衣物</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

import {
  getDraftCount,
  listCategories,
  listClothes,
  listClothingColors,
  listClothingTags,
  type ClothingCategory,
  type ClothingListFilters,
  type ClothingListResponse,
  type ClothingTag
} from '../../api/clothing'
import ClothingCard from '../../components/clothing/ClothingCard.vue'
import ClothingFilters from '../../components/clothing/ClothingFilters.vue'

const filters = ref<ClothingListFilters>({
  page: 0,
  size: 24,
  categoryId: '',
  status: '',
  color: '',
  season: '',
  tagIds: []
})
const list = ref<ClothingListResponse>({
  items: [],
  page: 0,
  size: 24,
  total: 0
})
const draftCount = ref(0)
const categories = ref<ClothingCategory[]>([])
const colors = ref<string[]>([])
const tags = ref<ClothingTag[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  await Promise.all([
    loadClothes(),
    loadDraftCount(),
    loadFilterOptions()
  ])
})

watch(filters, () => {
  void loadClothes()
}, { deep: true })

async function loadClothes(): Promise<void> {
  loading.value = true
  error.value = ''

  try {
    list.value = await listClothes(filters.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法加载衣物。'
  } finally {
    loading.value = false
  }
}

async function loadDraftCount(): Promise<void> {
  const response = await getDraftCount()
  draftCount.value = response.count
}

async function loadFilterOptions(): Promise<void> {
  const [nextCategories, nextColors, nextTags] = await Promise.all([
    listCategories(),
    listClothingColors(),
    listClothingTags()
  ])
  categories.value = nextCategories
  colors.value = nextColors
  tags.value = nextTags
}
</script>
