<template>
  <form class="clothing-filters" aria-label="衣物筛选">
    <label>
      品类
      <select :value="modelValue.categoryId" @change="update('categoryId', inputValue($event))">
        <option value="">全部品类</option>
        <option
          v-for="category in categories"
          :key="category.categoryId"
          :value="String(category.categoryId)"
        >
          {{ category.name }}
        </option>
      </select>
    </label>

    <label>
      状态
      <select :value="modelValue.status" @change="update('status', inputValue($event))">
        <option value="">全部状态</option>
        <option value="draft">待完善</option>
        <option value="ready">可搭配</option>
      </select>
    </label>

    <label>
      颜色
      <select :value="modelValue.color" @change="update('color', inputValue($event))">
        <option value="">全部颜色</option>
        <option v-for="color in colors" :key="color" :value="color">{{ color }}</option>
      </select>
    </label>

    <label>
      季节
      <select :value="modelValue.season" @change="update('season', inputValue($event))">
        <option value="">全部季节</option>
        <option value="spring">春</option>
        <option value="summer">夏</option>
        <option value="autumn">秋</option>
        <option value="winter">冬</option>
      </select>
    </label>

    <label>
      标签
      <select
        multiple
        :value="modelValue.tagIds"
        @change="updateTags($event)"
      >
        <option v-for="tag in tags" :key="tag.tagId" :value="String(tag.tagId)">
          {{ tag.name }}
        </option>
      </select>
    </label>
  </form>
</template>

<script setup lang="ts">
import type { ClothingCategory, ClothingListFilters, ClothingTag } from '../../api/clothing'

const props = defineProps<{
  modelValue: ClothingListFilters
  categories: ClothingCategory[]
  colors: string[]
  tags: ClothingTag[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ClothingListFilters]
}>()

function inputValue(event: Event): string {
  return (event.target as HTMLSelectElement).value
}

function update(key: keyof ClothingListFilters, value: string): void {
  emit('update:modelValue', {
    ...props.modelValue,
    page: 0,
    [key]: value
  })
}

function updateTags(event: Event): void {
  const selected = Array.from((event.target as HTMLSelectElement).selectedOptions)
    .map((option) => option.value)

  emit('update:modelValue', {
    ...props.modelValue,
    page: 0,
    tagIds: selected
  })
}
</script>
