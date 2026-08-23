<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  generateNextChapter,
  listStageChapters,
  listRevisions,
  editChapterContent,
  approveChapter,
  regenerateChapter,
  polishChapter,
  extractChapterError,
  type ChapterResponse,
  type ChapterRevisionResponse,
} from '@/api/chapters'

const props = defineProps<{ stageId: number; planCount?: number; refreshToken?: number }>()

const chapters = ref<ChapterResponse[]>([])
const generating = ref(false)
const errorMsg = ref('')
const forcedAll = ref(false)
const openId = ref<number | null>(null)

// ---- v0.1.1 Phase 5: per-chapter author workflow state ----
const editingId = ref<number | null>(null)
const editingContent = ref('')
const savingEdit = ref(false)
const approvingId = ref<number | null>(null)
const regeneratingId = ref<number | null>(null)
const regenerateInstruction = ref('')
const regenerateOpenId = ref<number | null>(null)
// v0.1.1 Phase 8 (TASK-170): polish state
const polishingId = ref<number | null>(null)
const polishInstruction = ref('')
const polishOpenId = ref<number | null>(null)
const historyFor = ref<number | null>(null)
const historyLoading = ref(false)
const revisions = ref<ChapterRevisionResponse[]>([])

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
  // switching chapters closes any in-flight editor/history pane
  editingId.value = null
  historyFor.value = null
  regenerateOpenId.value = null
}

function startEdit(c: ChapterResponse): void {
  editingId.value = c.id
  editingContent.value = c.content
}

function cancelEdit(): void {
  editingId.value = null
  editingContent.value = ''
}

async function saveEdit(c: ChapterResponse): Promise<void> {
  if (!editingContent.value.trim()) return
  errorMsg.value = ''
  savingEdit.value = true
  try {
    const updated = await editChapterContent(c.id, editingContent.value)
    replaceLocal(updated)
    editingId.value = null
  } catch (err) {
    errorMsg.value = extractChapterError(err)
  } finally {
    savingEdit.value = false
  }
}

async function approve(c: ChapterResponse): Promise<void> {
  errorMsg.value = ''
  approvingId.value = c.id
  try {
    const updated = await approveChapter(c.id)
    replaceLocal(updated)
  } catch (err) {
    errorMsg.value = extractChapterError(err)
  } finally {
    approvingId.value = null
  }
}

async function regenerate(c: ChapterResponse): Promise<void> {
  errorMsg.value = ''
  regeneratingId.value = c.id
  try {
    const updated = await regenerateChapter(c.id, regenerateInstruction.value)
    replaceLocal(updated)
    regenerateOpenId.value = null
    regenerateInstruction.value = ''
  } catch (err) {
    errorMsg.value = extractChapterError(err)
  } finally {
    regeneratingId.value = null
  }
}

// v0.1.1 Phase 8 (TASK-170): fact-preserving polish
async function polish(c: ChapterResponse): Promise<void> {
  errorMsg.value = ''
  polishingId.value = c.id
  try {
    const updated = await polishChapter(c.id, polishInstruction.value)
    replaceLocal(updated)
    polishOpenId.value = null
    polishInstruction.value = ''
  } catch (err) {
    errorMsg.value = extractChapterError(err)
  } finally {
    polishingId.value = null
  }
}

async function toggleHistory(c: ChapterResponse): Promise<void> {
  if (historyFor.value === c.id) {
    historyFor.value = null
    return
  }
  historyFor.value = c.id
  historyLoading.value = true
  try {
    revisions.value = await listRevisions(c.id)
  } catch (err) {
    errorMsg.value = extractChapterError(err)
  } finally {
    historyLoading.value = false
  }
}

function replaceLocal(updated: ChapterResponse): void {
  const idx = chapters.value.findIndex((x) => x.id === updated.id)
  if (idx >= 0) chapters.value[idx] = updated
}

function revisionSourceLabel(sourceType: string): string {
  switch (sourceType) {
    case 'AI_GENERATED': return 'AI 初稿'
    case 'MANUAL_EDIT': return '作者手改'
    case 'AI_REWRITE': return 'AI 重写'
    case 'AI_POLISH': return 'AI 润色'
    default: return sourceType
  }
}

onMounted(refresh)
watch(() => props.stageId, refresh)
watch(() => props.refreshToken, refresh)
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
      该阶段所有章节计划均已生成（共 {{ chapters.length }} 章）。生成内容为草稿，
      你可以编辑、重新生成或批准每一章。
    </p>

    <ul v-if="chapters.length" class="chapter-list">
      <li v-for="c in chapters" :key="c.id" class="chapter-item">
        <button class="chapter-item__head" @click="toggle(c.id)">
          <span class="chapter-item__num">{{ c.chapterNumber }}</span>
          <span class="chapter-item__title">{{ c.title }}</span>
          <span v-if="c.status === 'APPROVED'" class="badge badge--ok">已批准 · v{{ c.currentRevisionVersion }}</span>
          <span v-else class="badge badge--muted">草稿 · v{{ c.currentRevisionVersion }}</span>
          <span class="chapter-item__toggle">{{ openId === c.id ? '收起' : '展开' }}</span>
        </button>
        <div v-if="openId === c.id" class="chapter-item__body">
          <p v-if="c.summary" class="chapter-item__summary">{{ c.summary }}</p>

          <template v-if="editingId === c.id">
            <textarea v-model="editingContent" class="form__textarea chapter-edit__textarea" rows="12"></textarea>
            <div class="plan-item__actions">
              <button class="btn btn--primary btn--small" :disabled="savingEdit || !editingContent.trim()" @click="saveEdit(c)">
                {{ savingEdit ? '保存中…' : '保存为新修订' }}
              </button>
              <button class="btn btn--ghost btn--small" @click="cancelEdit">取消</button>
            </div>
          </template>
          <p v-else class="chapter-item__content">{{ c.content }}</p>

          <div v-if="editingId !== c.id" class="chapter-workflow">
            <span v-if="c.memoryExtractionStatus && c.memoryExtractionStatus !== 'COMPLETED'" class="badge badge--warn">
              记忆{{ c.memoryExtractionStatus === 'STALE' ? '待刷新' : c.memoryExtractionStatus }}
            </span>
            <button class="btn btn--ghost btn--small" @click="startEdit(c)">编辑</button>
            <button class="btn btn--ghost btn--small" :disabled="approvingId === c.id" @click="approve(c)">
              {{ approvingId === c.id ? '批准中…' : c.status === 'APPROVED' ? '再次批准' : '批准本章' }}
            </button>
            <button class="btn btn--ghost btn--small" @click="regenerateOpenId = regenerateOpenId === c.id ? null : c.id">
              重新生成
            </button>
            <button class="btn btn--ghost btn--small" @click="polishOpenId = polishOpenId === c.id ? null : c.id">
              AI 润色
            </button>
            <button class="btn btn--ghost btn--small" @click="toggleHistory(c)">
              {{ historyFor === c.id ? '收起历史' : '修订历史' }}
            </button>
          </div>

          <!-- TASK-144: optional steering for regeneration -->
          <div v-if="regenerateOpenId === c.id" class="chapter-regenerate">
            <textarea
              v-model="regenerateInstruction"
              class="form__textarea"
              rows="2"
              placeholder="可选：本次重写的指示，例：节奏更紧凑，删掉环境描写。"
            ></textarea>
            <button class="btn btn--primary btn--small" :disabled="regeneratingId === c.id" @click="regenerate(c)">
              {{ regeneratingId === c.id ? '重写中…' : '确认重写（保留旧版本）' }}
            </button>
          </div>

          <!-- v0.1.1 Phase 8 (TASK-170): polish with optional instruction -->
          <div v-if="polishOpenId === c.id" class="chapter-regenerate">
            <textarea
              v-model="polishInstruction"
              class="form__textarea"
              rows="2"
              placeholder="可选：润色指示，例：对话更口语化，删掉总结式结尾。（事实不会被改变）"
            ></textarea>
            <button class="btn btn--primary btn--small" :disabled="polishingId === c.id" @click="polish(c)">
              {{ polishingId === c.id ? '润色中…' : '开始润色（保留旧版本）' }}
            </button>
          </div>

          <!-- TASK-145: revision history (no diff viewer by design) -->
          <div v-if="historyFor === c.id" class="revision-history">
            <p v-if="historyLoading" class="revision-history__loading">加载中…</p>
            <ul v-else class="revision-history__list">
              <li v-for="r in revisions" :key="r.id" class="revision-history__item">
                <span class="revision-history__meta">
                  v{{ r.versionNumber }} · {{ revisionSourceLabel(r.sourceType) }} ·
                  {{ new Date(r.createdAt).toLocaleString() }}
                </span>
                <p class="revision-history__content">{{ r.content }}</p>
              </li>
            </ul>
          </div>
        </div>
      </li>
    </ul>

    <p v-else-if="!errorMsg && !generating" class="chapter-panel__empty">
      尚无生成的章节。点击「生成下一章」开始。
    </p>
  </section>
</template>
