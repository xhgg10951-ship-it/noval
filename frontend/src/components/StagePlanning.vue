<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  createStage,
  listStages,
  getStage,
  replanStage,
  replanRemainingStage,
  confirmStage,
  updatePlanGoal,
  extractStageError,
  type StageResponse,
  type StageSummary,
  type ChapterPlanResponse,
} from '@/api/stages'
import ChapterPanel from '@/components/ChapterPanel.vue'
import GenerationPanel from '@/components/GenerationPanel.vue'

const props = defineProps<{ storyId: number }>()

// ---- stage list ----
const stages = ref<StageSummary[]>([])

// ---- new stage form ----
const direction = ref('')
const generating = ref(false)
const errorMsg = ref('')

// ---- current stage under review ----
const stage = ref<StageResponse | null>(null)
const targetCount = ref<number>(3)
const replanning = ref(false)
const confirming = ref(false)
const editingPlanId = ref<number | null>(null)
const editingGoal = ref('')
const savingGoal = ref(false)

onMounted(async () => {
  await refreshStages()
})

async function refreshStages(): Promise<void> {
  try {
    stages.value = await listStages(props.storyId)
  } catch {
    // list failure is non-fatal; user can still create
  }
}

async function handleGenerate(): Promise<void> {
  errorMsg.value = ''
  if (!direction.value.trim()) {
    errorMsg.value = '请填写阶段方向'
    return
  }
  generating.value = true
  try {
    stage.value = await createStage(props.storyId, direction.value.trim())
    targetCount.value = stage.value.suggestedChapterCount
    direction.value = ''
    await refreshStages()
  } catch (err) {
    errorMsg.value = extractStageError(err)
  } finally {
    generating.value = false
  }
}

async function loadStage(id: number): Promise<void> {
  errorMsg.value = ''
  try {
    stage.value = await getStage(id)
    targetCount.value = stage.value.targetChapterCount ?? stage.value.suggestedChapterCount
  } catch (err) {
    errorMsg.value = extractStageError(err)
  }
}

async function handleReplan(): Promise<void> {
  if (!stage.value) return
  errorMsg.value = ''
  replanning.value = true
  try {
    stage.value = await replanStage(stage.value.id, targetCount.value)
    await refreshStages()
  } catch (err) {
    errorMsg.value = extractStageError(err)
  } finally {
    replanning.value = false
  }
}

async function handleConfirm(): Promise<void> {
  if (!stage.value) return
  errorMsg.value = ''
  confirming.value = true
  try {
    stage.value = await confirmStage(stage.value.id)
    await refreshStages()
  } catch (err) {
    errorMsg.value = extractStageError(err)
  } finally {
    confirming.value = false
  }
}

// ---- v0.1.1 Phase 4 (TASK-138): Replan Remaining for ACTIVE/PAUSED stages ----
const remainingCount = ref<number>(2)
const authorInstruction = ref('')
const replanningRemaining = ref(false)

function planStatusLabel(plan: ChapterPlanResponse): string | null {
  switch (plan.status) {
    case 'COMPLETED': return '已完成'
    case 'SUPERSEDED': return '已被新计划替代'
    case 'ACTIVE': return null // the default, no badge noise
    default: return plan.status
  }
}

async function handleReplanRemaining(): Promise<void> {
  if (!stage.value) return
  errorMsg.value = ''
  if (!Number.isInteger(remainingCount.value) || remainingCount.value < 1 || remainingCount.value > 50) {
    errorMsg.value = '剩余章节数需为 1–50 的整数'
    return
  }
  replanningRemaining.value = true
  try {
    stage.value = await replanRemainingStage(
      stage.value.id,
      remainingCount.value,
      authorInstruction.value,
    )
    authorInstruction.value = ''
    await refreshStages()
  } catch (err) {
    errorMsg.value = extractStageError(err)
  } finally {
    replanningRemaining.value = false
  }
}

function startEditGoal(plan: ChapterPlanResponse): void {
  editingPlanId.value = plan.id
  editingGoal.value = plan.goal
}

async function saveGoal(): Promise<void> {
  if (editingPlanId.value == null || !editingGoal.value.trim()) return
  savingGoal.value = true
  try {
    const updated = await updatePlanGoal(editingPlanId.value, editingGoal.value.trim())
    if (stage.value) {
      const idx = stage.value.plans.findIndex((p) => p.id === updated.id)
      if (idx >= 0) stage.value.plans[idx] = updated
    }
    editingPlanId.value = null
  } catch (err) {
    errorMsg.value = extractStageError(err)
  } finally {
    savingGoal.value = false
  }
}

function cancelEditGoal(): void {
  editingPlanId.value = null
}

function statusLabel(status: string): string {
  switch (status) {
    case 'PLANNING': return '规划中'
    case 'ACTIVE': return '已确认'
    case 'COMPLETED': return '已完成'
    case 'ABANDONED': return '已废弃'
    default: return status
  }
}
</script>

<template>
  <section class="stage-planning">
    <h3>阶段规划</h3>

    <!-- new stage form -->
    <div class="stage-form">
      <textarea
        v-model="direction"
        class="form__textarea"
        placeholder="输入阶段方向，例：主角和艾琳前往冒险者公会完成注册，并在过程中第一次小规模展示自己的特殊力量。"
        rows="2"
      ></textarea>
      <button class="btn btn--primary" :disabled="generating" @click="handleGenerate">
        {{ generating ? '规划中…' : '生成章节计划' }}
      </button>
    </div>

    <!-- existing stages -->
    <div v-if="stages.length > 0" class="stage-tabs">
      <button
        v-for="s in stages"
        :key="s.id"
        class="stage-tab"
        :class="{ 'stage-tab--active': stage?.id === s.id }"
        @click="loadStage(s.id)"
      >
        阶段 #{{ s.id }} · {{ statusLabel(s.status) }}
      </button>
    </div>

    <div v-if="errorMsg" class="alert alert--error">{{ errorMsg }}</div>

    <!-- current stage detail -->
    <article v-if="stage" class="stage-detail">
      <header class="stage-detail__header">
        <span class="badge">{{ statusLabel(stage.status) }}</span>
        <span class="stage-detail__meta">
          AI 建议 {{ stage.suggestedChapterCount }} 章
          <template v-if="stage.targetChapterCount"> · 目标 {{ stage.targetChapterCount }} 章</template>
        </span>
      </header>

      <p class="stage-detail__direction">{{ stage.direction }}</p>

      <ul class="plan-list">
        <li v-for="plan in stage.plans" :key="plan.id" class="plan-item" :class="{ 'plan-item--superseded': plan.status === 'SUPERSEDED' }">
          <span class="plan-item__order">{{ plan.chapterOrder }}</span>
          <div class="plan-item__body">
            <template v-if="editingPlanId === plan.id">
              <textarea v-model="editingGoal" class="form__textarea" rows="2"></textarea>
              <div class="plan-item__actions">
                <button class="btn btn--primary btn--small" :disabled="savingGoal" @click="saveGoal">
                  {{ savingGoal ? '保存中…' : '保存' }}
                </button>
                <button class="btn btn--ghost btn--small" @click="cancelEditGoal">取消</button>
              </div>
            </template>
            <template v-else>
              <p class="plan-item__goal">{{ plan.goal }}</p>
              <p v-if="plan.expectedProgress" class="plan-item__progress">{{ plan.expectedProgress }}</p>
              <span v-if="planStatusLabel(plan)" class="badge badge--muted">
                {{ planStatusLabel(plan) }} · 计划版本 v{{ plan.planVersion }}
              </span>
              <button
                v-if="stage.status === 'PLANNING'"
                class="btn btn--ghost btn--small"
                @click="startEditGoal(plan)"
              >
                编辑目标
              </button>
            </template>
          </div>
        </li>
      </ul>

      <!-- replan / confirm controls (only while PLANNING) -->
      <div v-if="stage.status === 'PLANNING'" class="stage-controls">
        <label class="stage-controls__label">
          目标章节数
          <input v-model.number="targetCount" type="number" min="1" max="50" class="form__input form__input--count" />
        </label>
        <button class="btn btn--ghost" :disabled="replanning" @click="handleReplan">
          {{ replanning ? '重新规划中…' : '重新规划' }}
        </button>
        <button class="btn btn--primary" :disabled="confirming" @click="handleConfirm">
          {{ confirming ? '确认中…' : '确认计划' }}
        </button>
      </div>

      <!-- v0.1.1 Phase 4 (TASK-138): Replan Remaining — change the future, never the past -->
      <div v-if="stage.status === 'ACTIVE' || stage.status === 'PAUSED'" class="stage-controls stage-controls--remaining">
        <p class="stage-controls__hint">
          重新规划只影响<b>未生成</b>的章节：已完成章节及其历史计划会原样保留。
          若正在后台生成，请先暂停或停止，到达安全检查点后再操作。
        </p>
        <label class="stage-controls__label">
          剩余章节数
          <input v-model.number="remainingCount" type="number" min="1" max="50" class="form__input form__input--count" />
        </label>
        <textarea
          v-model="authorInstruction"
          class="form__textarea"
          rows="2"
          placeholder="可选：对剩余章节的调整指示，例：后半段转向地下城探索，减少城镇日常。"
        ></textarea>
        <button class="btn btn--ghost" :disabled="replanningRemaining" @click="handleReplanRemaining">
          {{ replanningRemaining ? '重新规划剩余章节中…' : '重新规划剩余章节' }}
        </button>
      </div>

      <!-- multi-chapter generation + progress (M5, TASK-036..041) -->
      <GenerationPanel
        v-if="stage"
        :stage-id="stage.id"
        :plan-count="stage.plans?.length"
        :stage-status="stage.status"
      />

      <!-- single chapter generation + reading (M3, TASK-024) -->
      <ChapterPanel v-if="stage" :stage-id="stage.id" :plan-count="stage.plans?.length" />
    </article>
  </section>
</template>
