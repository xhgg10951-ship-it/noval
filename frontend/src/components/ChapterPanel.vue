<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  generateNextChapter,
  listStageChapters,
  extractChapterError,
  type ChapterResponse,
} from '@/api/chapters'

const props = defineProps<{ stageId: number; planCount?: number }>()

const chapters = ref<ChapterResponse[]>([])
const generating = ref(false)
const errorMsg = ref('')
const forcedAll = ref(false)
const openId = ref<number | null>(null)

// "All generated" when chapter count reaches the plan count, or the backend
// already told us there are no pending plans (409).
const allGenerated = computed(
  () =>
    forcedAll.value ||
    (props.planCount != null && props.planCount > 0 && chapters.value.length >= props.planCount),
)

async function refresh(): Promise<void> {
  errorMsg.value = ''
  try {
    chapters.value = await listStageChapters(props.stageId)
    forcedAll.value = false
  } catch (err) {
    errorMsg.value = extractChapterError(err)
  }
}

async function handleGenerate(): Promise<void> {
  if (allGenerated.value) return
  errorMsg.value = ''
  generating.value = true
  try {
    const created = await generateNextChapter(props.stageId)
    chapters.value = [...chapters.value, created]
    openId.value = created.id
  } catch (err) {
    const msg = extractChapterError(err)
    if (msg === '该阶段所有章节计划均已生成') forcedAll.value = true
    errorMsg.value = msg
  } finally {
    generating.value = false
  }
}

function toggle(id: number): void {
  openId.value = openId.value === id ? null : id
}

onMounted(refresh)
watch(() => props.stageId, refresh)
</script>

<template>
  <section class="chapter-panel">
    <div class="chapter-panel__header">
      <h4>章节生成</h4>
      <button
        class="btn btn--primary btn--small"
        :disabled="generating || allGenerated"
        @click="handleGenerate"
      >
        {{ generating ? '生成中…' : allGenerated ? '已全部生成' : '生成下一章' }}
      </button>
    </div>

    <div v-if="errorMsg" class="alert alert--error">{{ errorMsg }}</div>

    <p v-if="allGenerated && chapters.length" class="chapter-panel__done">
      该阶段所有章节计划均已生成（共 {{ chapters.length }} 章）。
    </p>

    <ul v-if="chapters.length" class="chapter-list">
      <li v-for="c in chapters" :key="c.id" class="chapter-item">
        <button class="chapter-item__head" @click="toggle(c.id)">
          <span class="chapter-item__num">{{ c.chapterNumber }}</span>
          <span class="chapter-item__title">{{ c.title }}</span>
          <span class="chapter-item__toggle">{{ openId === c.id ? '收起' : '展开' }}</span>
        </button>
        <div v-if="openId === c.id" class="chapter-item__body">
          <p v-if="c.summary" class="chapter-item__summary">{{ c.summary }}</p>
          <p class="chapter-item__content">{{ c.content }}</p>
        </div>
      </li>
    </ul>

    <p v-else-if="!errorMsg && !generating" class="chapter-panel__empty">
      尚无生成的章节。点击「生成下一章」开始。
    </p>
  </section>
</template>
