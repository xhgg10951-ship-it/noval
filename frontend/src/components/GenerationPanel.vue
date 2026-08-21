<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  startGeneration,
  continueGeneration,
  retryGeneration,
  listStageGenerationJobs,
  extractGenerationError,
  type GenerationJobResponse,
  type GenerationMode,
} from '@/api/generation'

const props = defineProps<{ stageId: number; planCount?: number; stageStatus?: string }>()

const job = ref<GenerationJobResponse | null>(null)
const mode = ref<GenerationMode>('CONTINUOUS')
const busy = ref(false)
const errorMsg = ref('')

const canStart = computed(
  () => !busy.value && props.stageStatus === 'ACTIVE' && (job.value == null || job.value.status === 'COMPLETED'),
)

const progressPct = computed(() => {
  if (!job.value || job.value.total === 0) return 0
  return Math.round((job.value.currentPlanIndex / job.value.total) * 100)
})

const phaseLabel = computed(() => {
  switch (job.value?.phase) {
    case 'PLANNING': return '规划中'
    case 'WRITING': return '写作中'
    case 'MEMORY': return '记忆提取中'
    case 'CHECKPOINT': return '检查点'
    default: return ''
  }
})

const statusLabel = computed(() => {
  switch (job.value?.status) {
    case 'PENDING': return '等待中'
    case 'RUNNING': return '进行中'
    case 'PAUSED': return '已暂停'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    default: return job.value?.status ?? ''
  }
})

const statusClass = computed(() => {
  switch (job.value?.status) {
    case 'COMPLETED': return 'badge--ok'
    case 'FAILED': return 'badge--error'
    case 'PAUSED': return 'badge--warn'
    case 'RUNNING': return 'badge--run'
    default: return 'badge'
  }
})

async function refreshJob(): Promise<void> {
  try {
    const jobs = await listStageGenerationJobs(props.stageId)
    job.value = jobs.length ? jobs[0] : null
  } catch {
    // non-fatal: keep current view
  }
}

async function start(): Promise<void> {
  errorMsg.value = ''
  busy.value = true
  try {
    job.value = await startGeneration(props.stageId, mode.value)
  } catch (err) {
    errorMsg.value = extractGenerationError(err)
  } finally {
    busy.value = false
  }
}

async function cont(): Promise<void> {
  if (!job.value) return
  errorMsg.value = ''
  busy.value = true
  try {
    job.value = await continueGeneration(job.value.id)
  } catch (err) {
    errorMsg.value = extractGenerationError(err)
  } finally {
    busy.value = false
  }
}

async function retry(): Promise<void> {
  if (!job.value) return
  errorMsg.value = ''
  busy.value = true
  try {
    job.value = await retryGeneration(job.value.id)
  } catch (err) {
    errorMsg.value = extractGenerationError(err)
  } finally {
    busy.value = false
  }
}

onMounted(refreshJob)
watch(() => props.stageId, refreshJob)
</script>

<template>
  <section class="gen-panel">
    <div class="gen-panel__header">
      <h4>多章生成</h4>
      <span v-if="job" :class="['badge', statusClass]">{{ statusLabel }}</span>
    </div>

    <div v-if="errorMsg" class="alert alert--error">{{ errorMsg }}</div>

    <div v-if="!job || job.status === 'COMPLETED'" class="gen-panel__start">
      <p class="gen-panel__hint">
        选择生成模式：<strong>连续</strong>自动完成全部章节；<strong>逐步</strong>每章后暂停，需手动继续。
      </p>
      <div class="gen-panel__modes">
        <label class="gen-panel__mode">
          <input type="radio" value="CONTINUOUS" v-model="mode" :disabled="busy" />
          连续 (Continuous)
        </label>
        <label class="gen-panel__mode">
          <input type="radio" value="STEP" v-model="mode" :disabled="busy" />
          逐步 (Step-by-Step)
        </label>
      </div>
      <button class="btn btn--primary" :disabled="!canStart" @click="start">
        {{ busy ? '生成中…' : '开始生成' }}
      </button>
      <p v-if="props.stageStatus !== 'ACTIVE'" class="gen-panel__note">
        请先在「阶段规划」中确认章节计划，再开始多章生成。
      </p>
      <p v-if="job && job.status === 'COMPLETED'" class="gen-panel__done">
        该阶段所有章节已生成完成（共 {{ job.total }} 章）。
      </p>
    </div>

    <div v-else class="gen-panel__progress">
      <div class="gen-panel__bar">
        <div class="gen-panel__bar-fill" :style="{ width: progressPct + '%' }"></div>
      </div>
      <div class="gen-panel__meta">
        进度 {{ job.currentPlanIndex }} / {{ job.total }}
        <template v-if="phaseLabel && (job.status === 'RUNNING' || job.status === 'PAUSED')">
          · {{ phaseLabel }}
        </template>
        <template v-if="job.mode === 'STEP'"> · 逐步模式</template>
        <template v-else> · 连续模式</template>
      </div>

      <div v-if="job.status === 'PAUSED'" class="gen-panel__actions">
        <button class="btn btn--primary" :disabled="busy" @click="cont">继续下一章</button>
      </div>

      <div v-if="job.status === 'FAILED'" class="gen-panel__actions">
        <button class="btn btn--primary" :disabled="busy" @click="retry">重试</button>
        <p v-if="job.lastError" class="gen-panel__errdetail">{{ job.lastError }}</p>
      </div>
    </div>
  </section>
</template>
