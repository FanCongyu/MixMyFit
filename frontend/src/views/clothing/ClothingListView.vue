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
    <p v-if="uploadMessage" class="form-message">{{ uploadMessage }}</p>

    <label class="clothing-upload">
      批量上传图片
      <input type="file" accept="image/*" multiple @change="uploadSelectedFiles" />
    </label>

    <ClothingFilters
      v-model="filters"
      :categories="categories"
      :colors="colors"
      :tags="tags"
    />

    <div
      v-if="selectedIds.length"
      class="clothing-batch-toolbar"
      role="toolbar"
      aria-label="批量操作"
    >
      <strong>已选择 {{ selectedIds.length }} 件</strong>
      <label>
        批量品类
        <select v-model="batchCategoryId">
          <option value="">选择品类</option>
          <option
            v-for="category in categories"
            :key="category.categoryId"
            :value="String(category.categoryId)"
          >
            {{ category.name }}
          </option>
        </select>
      </label>
      <button
        type="button"
        :disabled="!batchCategoryId || batchSaving"
        @click="applyBatchCategory"
      >
        批量设置品类
      </button>
      <label>
        批量颜色
        <select v-model="batchColor">
          <option value="">选择颜色</option>
          <option v-for="color in colors" :key="color" :value="color">{{ color }}</option>
        </select>
      </label>
      <label>
        批量季节
        <select v-model="batchSeason">
          <option value="">选择季节</option>
          <option value="spring">春</option>
          <option value="summer">夏</option>
          <option value="autumn">秋</option>
          <option value="winter">冬</option>
        </select>
      </label>
      <button
        type="button"
        :disabled="(!batchColor && !batchSeason) || batchSaving"
        @click="applyBatchAttributes"
      >
        批量设置属性
      </button>
      <label>
        批量添加标签
        <select v-model="batchAddTagId">
          <option value="">选择标签</option>
          <option v-for="tag in tags" :key="tag.tagId" :value="String(tag.tagId)">
            {{ tag.name }}
          </option>
        </select>
      </label>
      <label>
        批量移除标签
        <select v-model="batchRemoveTagId">
          <option value="">选择标签</option>
          <option v-for="tag in tags" :key="tag.tagId" :value="String(tag.tagId)">
            {{ tag.name }}
          </option>
        </select>
      </label>
      <button
        type="button"
        :disabled="(!batchAddTagId && !batchRemoveTagId) || batchSaving"
        @click="applyBatchTags"
      >
        批量更新标签
      </button>
    </div>

    <p v-if="loading">正在加载衣物...</p>

    <div v-else-if="list.items.length" class="clothing-grid" aria-label="衣物列表">
      <ClothingCard
        v-for="item in list.items"
        :key="item.clothingId"
        :item="item"
        :selected="selectedIdSet.has(item.clothingId)"
        @toggle-selected="toggleSelected"
      />
    </div>

    <p v-else class="clothing-view__empty">暂无衣物</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import {
  batchUpdateClothes,
  getDraftCount,
  listCategories,
  listClothes,
  listClothingColors,
  listClothingTags,
  uploadClothing,
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
const batchSaving = ref(false)
const error = ref('')
const uploadMessage = ref('')
const selectedIds = ref<number[]>([])
const batchCategoryId = ref('')
const batchColor = ref('')
const batchSeason = ref('')
const batchAddTagId = ref('')
const batchRemoveTagId = ref('')
const selectedIdSet = computed(() => new Set(selectedIds.value))

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
    selectedIds.value = selectedIds.value.filter((id) =>
      list.value.items.some((item) => item.clothingId === id)
    )
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

function toggleSelected(clothingId: number): void {
  if (selectedIds.value.includes(clothingId)) {
    selectedIds.value = selectedIds.value.filter((id) => id !== clothingId)
    return
  }

  selectedIds.value = [...selectedIds.value, clothingId]
}

async function applyBatchCategory(): Promise<void> {
  if (!batchCategoryId.value || !selectedIds.value.length) {
    return
  }

  batchSaving.value = true
  error.value = ''

  try {
    await batchUpdateClothes({
      clothingIds: selectedIds.value,
      categoryId: Number(batchCategoryId.value)
    })
    batchCategoryId.value = ''
    await refreshAfterBatchUpdate()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法批量更新衣物。'
  } finally {
    batchSaving.value = false
  }
}

async function applyBatchAttributes(): Promise<void> {
  if ((!batchColor.value && !batchSeason.value) || !selectedIds.value.length) {
    return
  }

  batchSaving.value = true
  error.value = ''

  try {
    await batchUpdateClothes({
      clothingIds: selectedIds.value,
      ...(batchColor.value ? { color: batchColor.value } : {}),
      ...(batchSeason.value ? { seasons: [batchSeason.value] } : {})
    })
    batchColor.value = ''
    batchSeason.value = ''
    await refreshAfterBatchUpdate()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法批量更新衣物。'
  } finally {
    batchSaving.value = false
  }
}

async function applyBatchTags(): Promise<void> {
  if ((!batchAddTagId.value && !batchRemoveTagId.value) || !selectedIds.value.length) {
    return
  }

  batchSaving.value = true
  error.value = ''

  try {
    await batchUpdateClothes({
      clothingIds: selectedIds.value,
      ...(batchAddTagId.value ? { addTagIds: [Number(batchAddTagId.value)] } : {}),
      ...(batchRemoveTagId.value ? { removeTagIds: [Number(batchRemoveTagId.value)] } : {})
    })
    batchAddTagId.value = ''
    batchRemoveTagId.value = ''
    await refreshAfterBatchUpdate()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法批量更新衣物。'
  } finally {
    batchSaving.value = false
  }
}

async function refreshAfterBatchUpdate(): Promise<void> {
  selectedIds.value = []
  await Promise.all([loadClothes(), loadDraftCount()])
}

async function uploadSelectedFiles(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  if (!files.length) {
    return
  }

  error.value = ''
  uploadMessage.value = ''

  try {
    await Promise.all(files.map((file) => uploadClothing(file)))
    uploadMessage.value = `已创建 ${files.length} 件衣物`
    await Promise.all([loadClothes(), loadDraftCount()])
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法上传衣物图片。'
  } finally {
    input.value = ''
  }
}
</script>
