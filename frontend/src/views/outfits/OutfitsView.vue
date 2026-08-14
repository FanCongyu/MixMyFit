<template>
  <section class="outfits-view" aria-labelledby="outfits-title">
    <header class="outfits-view__header">
      <div>
        <h1 id="outfits-title">搭配方案</h1>
        <p>{{ list.total }} 个方案</p>
      </div>
    </header>

    <p v-if="error" class="form-message form-message--error" role="alert">{{ error }}</p>
    <p v-if="message" class="form-message">{{ message }}</p>

    <form class="outfits-filters" aria-label="搭配方案筛选">
      <label>
        筛选季节
        <select v-model="filters.season">
          <option value="">全部季节</option>
          <option value="spring">春</option>
          <option value="summer">夏</option>
          <option value="autumn">秋</option>
          <option value="winter">冬</option>
        </select>
      </label>
      <label>
        筛选标签
        <select v-model="selectedTagId">
          <option value="">全部标签</option>
          <option v-for="tag in tags" :key="tag.tagId" :value="String(tag.tagId)">
            {{ tag.name }}
          </option>
        </select>
      </label>
    </form>

    <p v-if="loading">正在加载搭配方案...</p>

    <ul v-else-if="list.items.length" class="outfits-list" aria-label="搭配方案列表">
      <li
        v-for="outfit in list.items"
        :key="outfit.outfitId"
        class="outfits-list__item"
        :aria-label="outfit.title"
      >
        <div>
          <strong>{{ outfit.title }}</strong>
          <p v-if="outfit.note">{{ outfit.note }}</p>
        </div>
        <div class="outfits-list__actions">
          <button type="button" :aria-label="`查看${outfit.title}`" @click="openDetail(outfit.outfitId)">
            查看
          </button>
          <button type="button" :aria-label="`删除${outfit.title}`" @click="confirmDelete(outfit)">
            删除
          </button>
        </div>
      </li>
    </ul>

    <p v-else class="outfits-view__empty">暂无搭配方案</p>

    <section v-if="detail" class="outfit-detail" aria-label="搭配详情">
      <header class="outfits-view__header">
        <h2>{{ detail.title }}</h2>
      </header>

      <ul class="outfit-content-list" aria-label="搭配内容">
        <li
          v-for="item in editableItems"
          :key="`${item.role}-${item.slot ?? 'overlay'}-${item.clothingId}`"
          class="outfit-content-list__item"
        >
          <span>{{ displayOutfitItem(item) }}</span>
          <button type="button" :aria-label="`移除衣物${item.clothingId}`" @click="removeOutfitItem(item)">
            移除
          </button>
        </li>
      </ul>

      <form class="outfit-edit-form" aria-label="编辑搭配方案" @submit.prevent="saveDetail">
        <label>
          编辑名称
          <input v-model="editTitle" type="text" />
        </label>
        <label>
          编辑备注
          <textarea v-model="editNote" rows="3" />
        </label>
        <label>
          编辑季节
          <select v-model="editSeasons" multiple>
            <option value="spring">春</option>
            <option value="summer">夏</option>
            <option value="autumn">秋</option>
            <option value="winter">冬</option>
          </select>
        </label>
        <label>
          编辑标签
          <select v-model="editTagIds" multiple>
            <option v-for="tag in tags" :key="tag.tagId" :value="String(tag.tagId)">
              {{ tag.name }}
            </option>
          </select>
        </label>
        <button type="submit" :disabled="saving">保存修改</button>
      </form>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

import {
  deleteOutfit,
  getOutfit,
  listOutfits,
  listOutfitTags,
  updateOutfit,
  type OutfitDetail,
  type OutfitItemPayload,
  type OutfitListFilters,
  type OutfitListResponse,
  type OutfitSummary,
  type OutfitTag
} from '../../api/outfit'

const filters = reactive<OutfitListFilters>({
  page: 0,
  size: 20,
  season: '',
  tagIds: []
})
const list = ref<OutfitListResponse>({
  items: [],
  page: 0,
  size: 20,
  total: 0
})
const tags = ref<OutfitTag[]>([])
const detail = ref<OutfitDetail | null>(null)
const editTitle = ref('')
const editNote = ref('')
const editSeasons = ref<string[]>([])
const editTagIds = ref<string[]>([])
const editableItems = ref<OutfitItemPayload[]>([])
const selectedTagId = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')

onMounted(async () => {
  await Promise.all([loadList(), loadTags()])
})

watch(filters, () => {
  void loadList()
}, { deep: true })

watch(selectedTagId, () => {
  filters.tagIds = selectedTagId.value ? [selectedTagId.value] : []
})

async function loadTags(): Promise<void> {
  tags.value = await listOutfitTags()
}

async function loadList(): Promise<void> {
  loading.value = true
  error.value = ''

  try {
    list.value = await listOutfits(filters)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法加载搭配方案。'
  } finally {
    loading.value = false
  }
}

async function openDetail(outfitId: number): Promise<void> {
  error.value = ''

  try {
    detail.value = await getOutfit(outfitId)
    editTitle.value = detail.value.title
    editNote.value = detail.value.note ?? ''
    editSeasons.value = [...detail.value.seasons]
    editTagIds.value = detail.value.tags.map((tag) => String(tag.tagId))
    editableItems.value = [...detail.value.items]
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法加载搭配详情。'
  }
}

async function saveDetail(): Promise<void> {
  if (!detail.value) {
    return
  }

  saving.value = true
  error.value = ''
  message.value = ''

  try {
    detail.value = await updateOutfit(detail.value.outfitId, {
      title: editTitle.value.trim(),
      note: editNote.value.trim(),
      seasons: editSeasons.value,
      tagIds: editTagIds.value.map((tagId) => Number(tagId)),
      items: editableItems.value
    })
    message.value = '已保存修改'
    await loadList()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法保存修改。'
  } finally {
    saving.value = false
  }
}

async function confirmDelete(outfit: OutfitSummary): Promise<void> {
  if (!window.confirm(`删除搭配方案“${outfit.title}”？`)) {
    return
  }

  error.value = ''
  message.value = ''

  try {
    await deleteOutfit(outfit.outfitId)
    message.value = '已删除搭配方案'
    if (detail.value?.outfitId === outfit.outfitId) {
      detail.value = null
    }
    await loadList()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法删除搭配方案。'
  }
}

function displayOutfitItem(item: OutfitItemPayload): string {
  const role = item.role === 'main_slot' ? '主槽位' : '配饰'
  const slot = item.slot ? ` ${item.slot}` : ''

  return `衣物 ${item.clothingId} · ${role}${slot}`
}

function removeOutfitItem(target: OutfitItemPayload): void {
  editableItems.value = editableItems.value.filter((item) => item !== target)
}
</script>
