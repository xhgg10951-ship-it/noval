<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  listArcs,
  createArc,
  updateArc,
  type ArcResponse,
} from '@/api/arcs'

const props = defineProps<{ storyId: number }>()

const arcs = ref<ArcResponse[]>([])
const loading = ref(false)
const errorMsg = ref('')
const creating = ref(false)
const savingId = ref<number | null>(null)
const editingId = ref<number | null>(null)

const newArc = reactive({
  title: '',
  goal: '',
  targetStartChapter: 1 as number | null,
  targetEndChapter: 60 as number | null,
})

interface EditRow {
  title: string
  goal: string
  targetStartChapter: number
  targetEndChapter: number
}
const editRow = reactive<EditRow>({ title: '', goal: '', targetStartChapter: 1, targetEndChapter: 1 })

async function refresh(): Promise<void> {
  errorMsg.value = ''
  loading.value = true
  try {
    arcs.value = await listArcs(props.storyId)
  } catch {
    errorMsg.value = '卷列表加载失败'
  } finally {
    loading.value = false
  }
}

function extractError(err: unknown): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const resp = (err as { response?: { data?: { message?: string } } }).response
    if (resp?.data?.message) return resp.data.message
  }
  return '请求失败'
}

function validate(start: number | null, end: number | null): string {
  if (start == null || end == null || start < 1 || end < start) {
    return '章节范围无效（需 1 ≤ 起始 ≤ 结束）'
  }
  return ''
}

async function handleCreate(): Promise<void> {
  errorMsg.value = ''
  const invalid = validate(newArc.targetStartChapter, newArc.targetEndChapter)
  if (invalid) {
    errorMsg.value = invalid
    return
  }
  if (!newArc.title.trim()) {
    errorMsg.value = '请填写卷标题'
    return
  }
  creating.value = true
  try {
    await createArc(props.storyId, {
      title: newArc.title.trim(),
      goal: newArc.goal.trim() || undefined,
      targetStartChapter: newArc.targetStartChapter!,
      targetEndChapter: newArc.targetEndChapter!,
    })
    newArc.title = ''
    newArc.goal = ''
    await refresh()
  } catch (err) {
    errorMsg.value = extractError(err)
  } finally {
    creating.value = false
  }
}

function startEdit(arc: ArcResponse): void {
  editingId.value = arc.id
  editRow.title = arc.title
  editRow.goal = arc.goal ?? ''
  editRow.targetStartChapter = arc.targetStartChapter
  editRow.targetEndChapter = arc.targetEndChapter
}

function cancelEdit(): void {
  editingId.value = null
}

async function saveEdit(arc: ArcResponse): Promise<void> {
  errorMsg.value = ''
  const invalid = validate(editRow.targetStartChapter, editRow.targetEndChapter)
  if (invalid) {
    errorMsg.value = invalid
    return
  }
  savingId.value = arc.id
  try {
    await updateArc(arc.id, {
      title: editRow.title.trim(),
      goal: editRow.goal.trim() || undefined,
      targetStartChapter: editRow.targetStartChapter,
      targetEndChapter: editRow.targetEndChapter,
    })
    editingId.value = null
    await refresh()
  } catch (err) {
    errorMsg.value = extractError(err)
  } finally {
    savingId.value = null
  }
}

async function activate(arc: ArcResponse): Promise<void> {
  errorMsg.value = ''
  savingId.value = arc.id
  try {
    await updateArc(arc.id, { status: 'ACTIVE' })
    await refresh()
  } catch (err) {
    errorMsg.value = extractError(err)
  } finally {
    savingId.value = null
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'PLANNED': return '规划中'
    case 'ACTIVE': return '当前卷'
    case 'COMPLETED': return '已完结'
    default: return status
  }
}

onMounted(refresh)
</script>

<template>
  <section class="arc-panel">
    <h3>故事卷（Arc）</h3>
    <p class="arc-panel__hint">
      卷是长篇的粗粒度结构：每卷一个章节范围和一个目标。规划新阶段时，
      AI 会被告知「当前卷」及其目标，避免过早推进终局剧情。
    </p>

    <div v-if="errorMsg" class="alert alert--error">{{ errorMsg }}</div>

    <!-- existing arcs -->
    <ul v-if="arcs.length" class="arc-list">
      <li v-for="a in arcs" :key="a.id" class="arc-item" :class="{ 'arc-item--active': a.status === 'ACTIVE' }">
        <template v-if="editingId === a.id">
          <input v-model="editRow.title" class="form__input form__input--flex" placeholder="卷标题" />
          <input v-model.number="editRow.targetStartChapter" class="form__input form__input--small" type="number" min="1" />
          <span>–</span>
          <input v-model.number="editRow.targetEndChapter" class="form__input form__input--small" type="number" min="1" />
          <input v-model="editRow.goal" class="form__input form__input--flex" placeholder="本卷目标（可选）" />
          <button class="btn btn--primary btn--small" :disabled="savingId === a.id" @click="saveEdit(a)">保存</button>
          <button class="btn btn--ghost btn--small" @click="cancelEdit">取消</button>
        </template>
        <template v-else>
          <div class="arc-item__main">
            <span class="arc-item__title">{{ a.title }}</span>
            <span class="badge" :class="a.status === 'ACTIVE' ? 'badge--ok' : 'badge--muted'">
              {{ statusLabel(a.status) }}
            </span>
            <span class="arc-item__range">第 {{ a.targetStartChapter }}–{{ a.targetEndChapter }} 章</span>
          </div>
          <p v-if="a.goal" class="arc-item__goal">{{ a.goal }}</p>
          <div class="plan-item__actions">
            <button class="btn btn--ghost btn--small" @click="startEdit(a)">编辑</button>
            <button
              v-if="a.status !== 'ACTIVE'"
              class="btn btn--ghost btn--small"
              :disabled="savingId === a.id"
              @click="activate(a)"
            >
              设为当前卷
            </button>
          </div>
        </template>
      </li>
    </ul>
    <p v-else-if="!loading" class="arc-panel__empty">尚无卷。为长篇添加第一卷以启用节奏控制。</p>

    <!-- create form -->
    <div class="arc-create">
      <input v-model="newArc.title" class="form__input form__input--flex" placeholder="新卷标题，例：第一卷·初入异界" />
      <input v-model.number="newArc.targetStartChapter" class="form__input form__input--small" type="number" min="1" placeholder="起始章" />
      <span>–</span>
      <input v-model.number="newArc.targetEndChapter" class="form__input form__input--small" type="number" min="1" placeholder="结束章" />
      <input v-model="newArc.goal" class="form__input form__input--flex" placeholder="本卷目标（可选）" />
      <button class="btn btn--primary btn--small" :disabled="creating" @click="handleCreate">
        {{ creating ? '添加中…' : '+ 添加卷' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.arc-panel__hint {
  font-size: 0.82rem;
  color: var(--text-muted, #6b7280);
  margin-top: -0.25rem;
}

.arc-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.arc-item {
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 8px;
  padding: 0.7rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.arc-item--active {
  border-color: #34d399;
}

.arc-item__main {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.arc-item__title {
  font-weight: 600;
}

.arc-item__range {
  font-size: 0.82rem;
  color: var(--text-muted, #6b7280);
}

.arc-item__goal {
  margin: 0;
  font-size: 0.85rem;
}

.arc-create {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 0.75rem;
  border-top: 1px dashed var(--border, #d1d5db);
  padding-top: 0.75rem;
}
</style>
