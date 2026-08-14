<template>
  <section class="outfit-editor" aria-labelledby="outfit-editor-title">
    <header class="outfit-editor__header">
      <div>
        <h1 id="outfit-editor-title">搭配编辑器</h1>
        <p>固定主槽位</p>
      </div>
    </header>

    <p v-if="error" class="form-message form-message--error" role="alert">{{ error }}</p>
    <p v-if="saveMessage" class="form-message">{{ saveMessage }}</p>
    <p v-if="loading">正在加载搭配槽位...</p>

    <form v-if="!loading" class="outfit-save-panel" aria-label="搭配保存信息" @submit.prevent="saveOutfit">
      <label>
        方案名称
        <input v-model="title" type="text" />
      </label>
      <label>
        备注
        <textarea v-model="note" rows="3" />
      </label>
      <label>
        搭配季节
        <select v-model="selectedSeasons" multiple>
          <option value="spring">春</option>
          <option value="summer">夏</option>
          <option value="autumn">秋</option>
          <option value="winter">冬</option>
        </select>
      </label>
      <label>
        搭配标签
        <select v-model="selectedOutfitTagIds" multiple>
          <option v-for="tag in outfitTags" :key="tag.tagId" :value="String(tag.tagId)">
            {{ tag.name }}
          </option>
        </select>
      </label>
      <button type="submit" :disabled="saving">保存搭配</button>
    </form>

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
      v-if="!loading"
      class="outfit-accessory-candidates"
      role="region"
      aria-label="配饰候选"
    >
      <header class="outfit-candidates__header">
        <h2>配饰候选</h2>
        <p>自定义品类衣物可作为配饰添加。</p>
      </header>

      <div class="outfit-candidates__list">
        <button
          v-for="item in accessoryCandidates"
          :key="item.clothingId"
          type="button"
          :aria-label="`添加${displayClothingName(item)}`"
          @click="addAccessory(item)"
        >
          {{ displayClothingName(item) }}
        </button>
      </div>

      <p v-if="!accessoryCandidates.length" class="outfit-accessory-layer__empty">
        暂无可添加配饰
      </p>
    </section>

    <section
      v-if="!loading"
      class="outfit-accessory-layer"
      role="region"
      aria-label="配饰层"
      @dragover.prevent
      @drop="dropAccessory"
    >
      <header class="outfit-candidates__header">
        <h2>配饰层</h2>
        <p>支持位置、层级和尺寸档位。</p>
      </header>

      <p v-if="!accessories.length" class="outfit-accessory-layer__empty">
        未添加配饰
      </p>

      <div v-else class="outfit-accessory-layer__items">
        <article
          v-for="accessory in accessories"
          :key="accessory.id"
          class="outfit-accessory"
          :aria-label="`配饰 ${accessory.name}`"
          draggable="true"
          :style="{
            transform: `translate(${accessory.positionX}px, ${accessory.positionY}px)`,
            zIndex: accessory.zIndex
          }"
          @dragstart="startAccessoryDrag(accessory.id)"
        >
          <img
            class="outfit-accessory__image"
            :src="accessory.clothing.imageUrl"
            :alt="accessory.name"
            loading="lazy"
          />
          <strong>{{ accessory.name }}</strong>
          <p>位置 {{ accessory.positionX }}, {{ accessory.positionY }} · 层级 {{ accessory.zIndex }}</p>

          <label>
            {{ accessory.name }}尺寸
            <select v-model="accessory.size">
              <option value="small">small</option>
              <option value="medium">medium</option>
              <option value="large">large</option>
            </select>
          </label>

          <div class="outfit-accessory__actions">
            <button type="button" :aria-label="`左移${accessory.name}`" @click="moveAccessory(accessory.id, -10, 0)">
              左移
            </button>
            <button type="button" :aria-label="`右移${accessory.name}`" @click="moveAccessory(accessory.id, 10, 0)">
              右移
            </button>
            <button type="button" :aria-label="`上移${accessory.name}`" @click="moveAccessory(accessory.id, 0, -10)">
              上移
            </button>
            <button type="button" :aria-label="`下移${accessory.name}`" @click="moveAccessory(accessory.id, 0, 10)">
              下移
            </button>
            <button type="button" :aria-label="`上移一层${accessory.name}`" @click="raiseAccessory(accessory.id)">
              上层
            </button>
            <button type="button" :aria-label="`移除${accessory.name}`" @click="removeAccessory(accessory.id)">
              移除
            </button>
          </div>
        </article>
      </div>

      <output class="outfit-accessory-layer__payload" aria-label="配饰保存数据">
        {{ JSON.stringify(accessoryPayload) }}
      </output>
    </section>

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
  type ClothingCategory,
  type ClothingItem,
  type ClothingListFilters,
  type ClothingTag
} from '../../api/clothing'
import { createOutfit, listOutfitTags, type OutfitItemPayload, type OutfitTag } from '../../api/outfit'
import OutfitMainSlot from '../../components/outfit/OutfitMainSlot.vue'

type MainSlotKey = 'top' | 'bottom' | 'shoes' | 'hat'
type AccessorySize = 'small' | 'medium' | 'large'

type MainSlot = {
  key: MainSlotKey
  label: string
  categoryId: string
}

type AccessoryOverlay = {
  id: number
  clothing: ClothingItem
  name: string
  positionX: number
  positionY: number
  size: AccessorySize
  zIndex: number
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
const outfitTags = ref<OutfitTag[]>([])
const accessoryCandidates = ref<ClothingItem[]>([])
const accessories = ref<AccessoryOverlay[]>([])
const title = ref('')
const note = ref('')
const selectedSeasons = ref<string[]>([])
const selectedOutfitTagIds = ref<string[]>([])
const nextAccessoryId = ref(1)
const draggedAccessoryId = ref<number | null>(null)
const activeSlotKey = ref<MainSlotKey | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const saveMessage = ref('')

const activeSlot = computed(() =>
  mainSlots.value.find((slot) => slot.key === activeSlotKey.value) ?? null
)
const selectedBySlot = computed<Record<MainSlotKey, ClothingItem | null>>(() => ({
  top: selectedItem('top'),
  bottom: selectedItem('bottom'),
  shoes: selectedItem('shoes'),
  hat: selectedItem('hat')
}))
const accessoryPayload = computed(() => accessories.value.map((accessory) => ({
  clothingId: accessory.clothing.clothingId,
  role: 'accessory_overlay',
  positionX: accessory.positionX,
  positionY: accessory.positionY,
  size: accessory.size,
  zIndex: accessory.zIndex
})))

onMounted(async () => {
  loading.value = true
  error.value = ''

  try {
    const [categories, nextColors, nextTags, nextOutfitTags] = await Promise.all([
      listCategories(),
      listClothingColors(),
      listClothingTags(),
      listOutfitTags()
    ])
    colors.value = nextColors
    tags.value = nextTags
    outfitTags.value = nextOutfitTags
    mainSlots.value = mainSlots.value.map((slot) => ({
      ...slot,
      categoryId: String(categories.find((category) =>
        category.type === 'fixed' && category.name === slot.label
      )?.categoryId ?? '')
    }))
    await Promise.all([
      ...mainSlots.value.map((slot) => loadCandidates(slot)),
      loadAccessoryCandidates(categories)
    ])
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

async function loadAccessoryCandidates(categories: ClothingCategory[]): Promise<void> {
  const customCategoryIds = categories
    .filter((category) => category.type !== 'fixed')
    .map((category) => String(category.categoryId))

  const responses = await Promise.all(customCategoryIds.map((categoryId) => listClothes({
    page: 0,
    size: 24,
    categoryId,
    status: 'ready'
  })))
  accessoryCandidates.value = responses
    .flatMap((response) => response.items)
    .filter((item) => item.category?.type !== 'fixed')
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

function displayClothingName(item: ClothingItem): string {
  return item.name?.trim() || '未命名衣物'
}

function addAccessory(item: ClothingItem): void {
  accessories.value.push({
    id: nextAccessoryId.value,
    clothing: item,
    name: displayClothingName(item),
    positionX: 0,
    positionY: 0,
    size: 'medium',
    zIndex: 1
  })
  nextAccessoryId.value += 1
}

function removeAccessory(accessoryId: number): void {
  accessories.value = accessories.value.filter((accessory) => accessory.id !== accessoryId)
}

function moveAccessory(accessoryId: number, deltaX: number, deltaY: number): void {
  const accessory = accessories.value.find((item) => item.id === accessoryId)
  if (!accessory) {
    return
  }

  accessory.positionX += deltaX
  accessory.positionY += deltaY
}

function raiseAccessory(accessoryId: number): void {
  const accessory = accessories.value.find((item) => item.id === accessoryId)
  if (!accessory) {
    return
  }

  accessory.zIndex += 1
}

function startAccessoryDrag(accessoryId: number): void {
  draggedAccessoryId.value = accessoryId
}

function dropAccessory(event: DragEvent): void {
  if (draggedAccessoryId.value === null) {
    return
  }

  const accessory = accessories.value.find((item) => item.id === draggedAccessoryId.value)
  if (!accessory) {
    draggedAccessoryId.value = null
    return
  }

  accessory.positionX = Math.round(event.clientX)
  accessory.positionY = Math.round(event.clientY)
  draggedAccessoryId.value = null
}

function mainSlotPayload(): OutfitItemPayload[] {
  return mainSlots.value
    .map((slot): OutfitItemPayload | null => {
      const item = selectedBySlot.value[slot.key]
      if (!item) {
        return null
      }

      return {
        clothingId: item.clothingId,
        role: 'main_slot',
        slot: slot.key,
        positionX: null,
        positionY: null,
        size: null,
        zIndex: null
      }
    })
    .filter((item): item is OutfitItemPayload => item !== null)
}

function accessorySavePayload(): OutfitItemPayload[] {
  return accessoryPayload.value.map((accessory) => ({
    ...accessory,
    slot: null
  }))
}

async function saveOutfit(): Promise<void> {
  const items = [...mainSlotPayload(), ...accessorySavePayload()]
  error.value = ''
  saveMessage.value = ''

  if (!items.length) {
    error.value = '至少选择一件衣物或配饰'
    return
  }

  saving.value = true

  try {
    const response = await createOutfit({
      title: title.value.trim(),
      note: note.value.trim(),
      seasons: selectedSeasons.value,
      tagIds: selectedOutfitTagIds.value.map((tagId) => Number(tagId)),
      items
    })
    saveMessage.value = `已保存 ${response.title}`
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法保存搭配。'
  } finally {
    saving.value = false
  }
}
</script>
