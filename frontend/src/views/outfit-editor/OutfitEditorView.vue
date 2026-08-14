<template>
  <section class="outfit-editor" aria-labelledby="outfit-editor-title">
    <header class="outfit-editor__header">
      <div>
        <h1 id="outfit-editor-title">搭配编辑器</h1>
        <p>固定主槽位</p>
      </div>
    </header>

    <p v-if="error" class="form-message form-message--error" role="alert">{{ error }}</p>
    <p v-if="loading">正在加载搭配槽位...</p>

    <div v-if="!loading" class="outfit-slot-grid">
      <OutfitMainSlot
        v-for="slot in mainSlots"
        :key="slot.key"
        :label="slot.label"
        :candidates="candidatesBySlot[slot.key]"
        :selected="selectedBySlot[slot.key]"
        @clear="clearSlot(slot.key)"
        @next="selectRelative(slot.key, 1)"
        @previous="selectRelative(slot.key, -1)"
        @open-selector="openSelector(slot.key)"
      />
    </div>

    <section
      v-if="activeSlot"
      class="outfit-candidates"
      role="dialog"
      :aria-label="`${activeSlot.label}候选选择器`"
    >
      <header class="outfit-candidates__header">
        <h2>{{ activeSlot.label }}候选</h2>
        <button type="button" @click="activeSlotKey = null">关闭</button>
      </header>

      <form class="outfit-candidates__filters" aria-label="候选筛选">
        <label>
          候选颜色
          <select v-model="candidateFilters.color">
            <option value="">全部颜色</option>
            <option v-for="color in colors" :key="color" :value="color">{{ color }}</option>
          </select>
        </label>

        <label>
          候选季节
          <select v-model="candidateFilters.season">
            <option value="">全部季节</option>
            <option value="spring">春</option>
            <option value="summer">夏</option>
            <option value="autumn">秋</option>
            <option value="winter">冬</option>
          </select>
        </label>

        <label>
          候选标签
          <select v-model="candidateFilters.tagIds" multiple>
            <option v-for="tag in tags" :key="tag.tagId" :value="String(tag.tagId)">
              {{ tag.name }}
            </option>
          </select>
        </label>
      </form>

      <div class="outfit-candidates__list" aria-label="候选衣物">
        <button
          v-for="item in candidatesBySlot[activeSlot.key]"
          :key="item.clothingId"
          type="button"
          @click="selectItem(activeSlot.key, item.clothingId)"
        >
          {{ item.name?.trim() || '未命名衣物' }}
        </button>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import {
  listCategories,
  listClothes,
  listClothingColors,
  listClothingTags,
  type ClothingItem,
  type ClothingListFilters,
  type ClothingTag
} from '../../api/clothing'
import OutfitMainSlot from '../../components/outfit/OutfitMainSlot.vue'

type MainSlotKey = 'top' | 'bottom' | 'shoes' | 'hat'

type MainSlot = {
  key: MainSlotKey
  label: string
  categoryId: string
}

const mainSlots = ref<MainSlot[]>([
  { key: 'top', label: '上装', categoryId: '' },
  { key: 'bottom', label: '下装', categoryId: '' },
  { key: 'shoes', label: '鞋子', categoryId: '' },
  { key: 'hat', label: '帽子', categoryId: '' }
])

const candidatesBySlot = reactive<Record<MainSlotKey, ClothingItem[]>>({
  top: [],
  bottom: [],
  shoes: [],
  hat: []
})
const selectedIdsBySlot = reactive<Record<MainSlotKey, number | null>>({
  top: null,
  bottom: null,
  shoes: null,
  hat: null
})
const candidateFilters = reactive<Pick<ClothingListFilters, 'color' | 'season' | 'tagIds'>>({
  color: '',
  season: '',
  tagIds: []
})
const colors = ref<string[]>([])
const tags = ref<ClothingTag[]>([])
const activeSlotKey = ref<MainSlotKey | null>(null)
const loading = ref(true)
const error = ref('')

const activeSlot = computed(() =>
  mainSlots.value.find((slot) => slot.key === activeSlotKey.value) ?? null
)
const selectedBySlot = computed<Record<MainSlotKey, ClothingItem | null>>(() => ({
  top: selectedItem('top'),
  bottom: selectedItem('bottom'),
  shoes: selectedItem('shoes'),
  hat: selectedItem('hat')
}))

onMounted(async () => {
  loading.value = true
  error.value = ''

  try {
    const [categories, nextColors, nextTags] = await Promise.all([
      listCategories(),
      listClothingColors(),
      listClothingTags()
    ])
    colors.value = nextColors
    tags.value = nextTags
    mainSlots.value = mainSlots.value.map((slot) => ({
      ...slot,
      categoryId: String(categories.find((category) =>
        category.type === 'fixed' && category.name === slot.label
      )?.categoryId ?? '')
    }))
    await Promise.all(mainSlots.value.map((slot) => loadCandidates(slot)))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法加载搭配编辑器。'
  } finally {
    loading.value = false
  }
})

watch(candidateFilters, () => {
  if (activeSlot.value) {
    void loadCandidates(activeSlot.value, candidateFilters)
  }
}, { deep: true })

function selectedItem(slotKey: MainSlotKey): ClothingItem | null {
  const selectedId = selectedIdsBySlot[slotKey]

  return candidatesBySlot[slotKey].find((item) => item.clothingId === selectedId) ?? null
}

async function loadCandidates(
  slot: MainSlot,
  filters: Pick<ClothingListFilters, 'color' | 'season' | 'tagIds'> = {}
): Promise<void> {
  if (!slot.categoryId) {
    candidatesBySlot[slot.key] = []
    return
  }

  const response = await listClothes({
    page: 0,
    size: 24,
    categoryId: slot.categoryId,
    status: 'ready',
    color: filters.color,
    season: filters.season,
    tagIds: filters.tagIds
  })
  candidatesBySlot[slot.key] = response.items

  if (!selectedIdsBySlot[slot.key] && response.items.length) {
    selectedIdsBySlot[slot.key] = response.items[0].clothingId
  }
}

function selectRelative(slotKey: MainSlotKey, offset: 1 | -1): void {
  const candidates = candidatesBySlot[slotKey]
  if (!candidates.length) {
    return
  }

  const currentIndex = candidates.findIndex((item) => item.clothingId === selectedIdsBySlot[slotKey])
  const nextIndex = currentIndex === -1
    ? 0
    : (currentIndex + offset + candidates.length) % candidates.length
  selectedIdsBySlot[slotKey] = candidates[nextIndex].clothingId
}

function selectItem(slotKey: MainSlotKey, clothingId: number): void {
  selectedIdsBySlot[slotKey] = clothingId
}

function clearSlot(slotKey: MainSlotKey): void {
  selectedIdsBySlot[slotKey] = null
}

function openSelector(slotKey: MainSlotKey): void {
  activeSlotKey.value = slotKey
}
</script>
