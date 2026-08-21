<script setup lang="ts">
import { ref } from 'vue'
import {
  suggestDirections,
  storyQuery,
  extractAssistanceError,
  type DirectionItem,
} from '@/api/assistance'

const props = defineProps<{ storyId: number }>()

const directions = ref<DirectionItem[]>([])
const suggesting = ref(false)
const selectedIndex = ref<number | null>(null)
const newDirection = ref('')
const suggestError = ref('')

const question = ref('')
const answer = ref('')
const querying = ref(false)
const queryError = ref('')

async function loadSuggestions(): Promise<void> {
  suggestError.value = ''
  suggesting.value = true
  selectedIndex.value = null
  newDirection.value = ''
  try {
    const res = await suggestDirections(props.storyId)
    directions.value = res.directions ?? []
  } catch (err) {
    suggestError.value = extractAssistanceError(err)
  } finally {
    suggesting.value = false
  }
}

function pickDirection(i: number): void {
  selectedIndex.value = i
  const d = directions.value[i]
  newDirection.value = `${d.title}：${d.description}`
}

function rejectAll(): void {
  directions.value = []
  selectedIndex.value = null
  newDirection.value = ''
}

async function ask(): Promise<void> {
  if (!question.value.trim()) return
  queryError.value = ''
  querying.value = true
  try {
    const res = await storyQuery(props.storyId, question.value.trim())
    answer.value = res.answer
  } catch (err) {
    queryError.value = extractAssistanceError(err)
  } finally {
    querying.value = false
  }
}
</script>

<template>
  <section class="assist-panel">
    <h3>创作助手</h3>

    <!-- Planner suggestions (TASK-042/043, AT-K01..K03) -->
    <div class="assist-panel__block">
      <div class="assist-panel__head">
        <h4>后续剧情建议</h4>
        <button class="btn btn--ghost btn--small" :disabled="suggesting" @click="loadSuggestions">
          {{ suggesting ? '生成中…' : '获取建议' }}
        </button>
      </div>
      <div v-if="suggestError" class="alert alert--error">{{ suggestError }}</div>

      <ul v-if="directions.length" class="assist-dirs">
        <li
          v-for="(d, i) in directions"
          :key="i"
          class="assist-dir"
          :class="{ 'assist-dir--selected': selectedIndex === i }"
        >
          <button class="assist-dir__pick" @click="pickDirection(i)">
            <strong>{{ d.title }}</strong>
            <span class="assist-dir__desc">{{ d.description }}</span>
          </button>
        </li>
      </ul>

      <div v-if="selectedIndex !== null" class="assist-panel__edit">
        <label class="assist-panel__label">作为新的阶段方向（可编辑后使用）</label>
        <textarea v-model="newDirection" class="form__textarea" rows="2"></textarea>
        <div class="assist-panel__actions">
          <button class="btn btn--ghost btn--small" @click="rejectAll">全部拒绝</button>
        </div>
        <p class="assist-panel__hint">复制以上文本，到「阶段规划」粘贴为新的阶段方向即可使用。</p>
      </div>
    </div>

    <!-- Story query (TASK-044/045/046, AT-J01..J05) -->
    <div class="assist-panel__block">
      <h4>作品查询</h4>
      <div class="assist-query">
        <input
          v-model="question"
          class="form__input"
          placeholder="例如：主角现在在哪里？持有哪些物品？艾琳和主角是什么关系？"
          @keyup.enter="ask"
        />
        <button class="btn btn--primary btn--small" :disabled="querying || !question.trim()" @click="ask">
          {{ querying ? '查询中…' : '查询' }}
        </button>
      </div>
      <div v-if="queryError" class="alert alert--error">{{ queryError }}</div>
      <p v-if="answer" class="assist-query__answer">{{ answer }}</p>
    </div>
  </section>
</template>
