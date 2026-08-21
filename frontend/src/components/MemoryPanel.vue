<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  getMemoryView,
  applyCandidate,
  ignoreCandidate,
  type MemoryView,
  type MemoryCandidate,
} from '@/api/memory'

const props = defineProps<{ storyId: number }>()

const view = ref<MemoryView | null>(null)
const errorMsg = ref('')
const busyId = ref<number | null>(null)

async function refresh(): Promise<void> {
  errorMsg.value = ''
  try {
    view.value = await getMemoryView(props.storyId)
  } catch {
    errorMsg.value = '读取记忆失败'
  }
}

async function handleApply(c: MemoryCandidate): Promise<void> {
  busyId.value = c.id
  try {
    await applyCandidate(c.id)
    await refresh()
  } catch {
    errorMsg.value = '应用记忆失败'
  } finally {
    busyId.value = null
  }
}

async function handleIgnore(c: MemoryCandidate): Promise<void> {
  busyId.value = c.id
  try {
    await ignoreCandidate(c.id)
    await refresh()
  } catch {
    errorMsg.value = '忽略记忆失败'
  } finally {
    busyId.value = null
  }
}

function actionLabel(a: string): string {
  return a === 'AUTO' ? '自动' : a === 'REVIEW' ? '待审' : '忽略'
}

function statusLabel(s: string): string {
  return s === 'APPLIED' ? '已应用' : s === 'IGNORED' ? '已忽略' : '待处理'
}

onMounted(refresh)
</script>

<template>
  <section class="memory-panel">
    <h4>记忆与状态</h4>
    <div v-if="errorMsg" class="alert alert--error">{{ errorMsg }}</div>

    <!-- Current State -->
    <div class="memory-block">
      <h5>当前状态</h5>
      <ul v-if="view?.currentState.length" class="kv-list">
        <li v-for="s in view.currentState" :key="s.id">
          <span class="kv-list__key">{{ s.field }}</span>
          <span class="kv-list__val">{{ s.value }}</span>
        </li>
      </ul>
      <p v-else class="memory-empty">暂无记录</p>
    </div>

    <!-- Relationships -->
    <div class="memory-block">
      <h5>人物关系</h5>
      <ul v-if="view?.relationships.length" class="rel-list">
        <li v-for="r in view.relationships" :key="r.id">
          <strong>{{ r.subjectA }} → {{ r.subjectB }}</strong>
          <span>{{ r.description }}</span>
        </li>
      </ul>
      <p v-else class="memory-empty">暂无记录</p>
    </div>

    <!-- Story Memory -->
    <div class="memory-block">
      <h5>故事记忆</h5>
      <ul v-if="view?.storyMemories.length" class="mem-list">
        <li v-for="m in view.storyMemories" :key="m.id">
          <span class="mem-list__type">{{ m.type }}</span>
          <span>{{ m.description }}</span>
        </li>
      </ul>
      <p v-else class="memory-empty">暂无记录</p>
    </div>

    <!-- Review queue -->
    <div class="memory-block">
      <h5>记忆候选（待审 / 自动）</h5>
      <ul v-if="view?.candidates.length" class="cand-list">
        <li v-for="c in view.candidates" :key="c.id" class="cand-item" :class="`cand-item--${c.processingStatus.toLowerCase()}`">
          <div class="cand-item__head">
            <span class="badge" :class="`badge--${c.suggestedAction.toLowerCase()}`">{{ actionLabel(c.suggestedAction) }}</span>
            <span class="cand-item__type">{{ c.type }}</span>
            <span class="cand-item__subj">{{ c.subject }}<template v-if="c.field"> · {{ c.field }}</template></span>
            <span class="cand-item__status">{{ statusLabel(c.processingStatus) }}</span>
          </div>
          <p class="cand-item__value">{{ c.value }}</p>
          <p v-if="c.evidence" class="cand-item__evidence">依据：{{ c.evidence }}</p>
          <div v-if="c.processingStatus !== 'APPLIED' && c.processingStatus !== 'IGNORED'" class="cand-item__actions">
            <button class="btn btn--primary btn--small" :disabled="busyId === c.id" @click="handleApply(c)">采用</button>
            <button class="btn btn--ghost btn--small" :disabled="busyId === c.id" @click="handleIgnore(c)">忽略</button>
          </div>
        </li>
      </ul>
      <p v-else class="memory-empty">暂无候选</p>
    </div>
  </section>
</template>
