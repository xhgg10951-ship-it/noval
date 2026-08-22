<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStoryStore } from '@/stores/story'
import { extractApiError, type ConstraintInput } from '@/api/stories'

interface ConstraintRow {
  id: number
  type: string
  content: string
}

const router = useRouter()
const storyStore = useStoryStore()

const form = reactive({
  name: '',
  coreIdea: '',
  initialStageDirection: '',
  defaultTargetCharacters: 3000 as number | null,
  targetChapterCount: null as number | null,
  writingStyle: '',
})

const constraints = reactive<ConstraintRow[]>([
  { id: Date.now(), type: '', content: '' },
])

const submitting = ref(false)
const successMsg = ref('')
const errorMsg = ref('')

function addConstraint(): void {
  constraints.push({ id: Date.now() + constraints.length, type: '', content: '' })
}

function removeConstraint(index: number): void {
  if (constraints.length > 1) {
    constraints.splice(index, 1)
  }
}

async function handleSubmit(): Promise<void> {
  errorMsg.value = ''
  successMsg.value = ''

  if (!form.name.trim()) {
    errorMsg.value = '请填写故事名称'
    return
  }
  if (!form.coreIdea.trim()) {
    errorMsg.value = '请填写核心创意'
    return
  }

  // Filter to only filled constraint rows.
  const filledConstraints: ConstraintInput[] = constraints
    .filter((c) => c.type.trim() && c.content.trim())
    .map((c, i) => ({
      type: c.type.trim(),
      content: c.content.trim(),
      sortOrder: i,
    }))

  submitting.value = true
  try {
    const created = await storyStore.createStory({
      name: form.name.trim(),
      coreIdea: form.coreIdea.trim(),
      initialStageDirection: form.initialStageDirection.trim() || undefined,
      defaultTargetCharacters: form.defaultTargetCharacters ?? undefined,
      targetChapterCount: form.targetChapterCount ?? undefined,
      writingStyle: form.writingStyle.trim() || undefined,
      constraints: filledConstraints.length > 0 ? filledConstraints : undefined,
    })
    successMsg.value = `故事创建成功！(ID: ${created.id})`
    // Reset form
    form.name = ''
    form.coreIdea = ''
    form.initialStageDirection = ''
    form.defaultTargetCharacters = 3000
    form.targetChapterCount = null
    form.writingStyle = ''
    constraints.splice(0, constraints.length, { id: Date.now(), type: '', content: '' })
    // Navigate to story list after a brief delay so the user sees success.
    setTimeout(() => router.push('/stories'), 800)
  } catch (err) {
    errorMsg.value = extractApiError(err)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="create-story">
    <h2>创建新故事</h2>
    <p class="create-story__hint">填写基本信息和约束，提交后将保存到数据库。</p>

    <form class="form" @submit.prevent="handleSubmit">
      <div class="form__field">
        <label class="form__label" for="story-name">故事名称 *</label>
        <input
          id="story-name"
          v-model="form.name"
          class="form__input"
          type="text"
          placeholder="例：星际拾荒者"
          maxlength="200"
        />
      </div>

      <div class="form__field">
        <label class="form__label" for="core-idea">核心创意 *</label>
        <textarea
          id="core-idea"
          v-model="form.coreIdea"
          class="form__textarea"
          placeholder="一句话描述故事的核心冲突或卖点"
          rows="3"
        ></textarea>
      </div>

      <div class="form__field">
        <label class="form__label" for="stage-direction">初始导演指令（可选）</label>
        <textarea
          id="stage-direction"
          v-model="form.initialStageDirection"
          class="form__textarea"
          placeholder="例：主角在一艘废弃空间站中醒来，不知道自己是谁"
          rows="2"
        ></textarea>
      </div>

      <div class="form__field">
        <label class="form__label" for="target-chars">单章默认目标字数（可选）</label>
        <input
          id="target-chars"
          v-model.number="form.defaultTargetCharacters"
          class="form__input"
          type="number"
          min="300"
          max="20000"
          step="100"
          placeholder="默认 3000"
        />
        <p class="form__sub">生成章节时以此为目标长度；留空则使用默认值 3000。</p>
      </div>

      <div class="form__field">
        <label class="form__label" for="target-chapters">长篇目标章节数（可选）</label>
        <input
          id="target-chapters"
          v-model.number="form.targetChapterCount"
          class="form__input"
          type="number"
          min="1"
          max="5000"
          step="1"
          placeholder="例：600"
        />
        <p class="form__sub">长篇节奏锚点：规划时会告知 AI 本书的目标总章数与当前位置，避免过早推进终局。短篇可留空。</p>
      </div>

      <div class="form__field">
        <label class="form__label" for="writing-style">写作风格提示（可选）</label>
        <input
          id="writing-style"
          v-model="form.writingStyle"
          class="form__input"
          type="text"
          placeholder="例：冷峻克制，少用形容词"
        />
      </div>

      <div class="form__field">
        <label class="form__label">故事约束</label>
        <p class="form__sub">每个约束包含类型和内容，至少填一项才算有效。</p>
        <div
          v-for="(c, index) in constraints"
          :key="c.id"
          class="constraint-row"
        >
          <input
            v-model="c.type"
            class="form__input form__input--small"
            type="text"
            placeholder="类型（如：主角设定、世界观、风格）"
          />
          <input
            v-model="c.content"
            class="form__input form__input--flex"
            type="text"
            placeholder="约束内容"
          />
          <button
            v-if="constraints.length > 1"
            type="button"
            class="btn btn--icon"
            @click="removeConstraint(index)"
          >
            ✕
          </button>
        </div>
        <button type="button" class="btn btn--ghost" @click="addConstraint">
          + 添加约束
        </button>
      </div>

      <div v-if="errorMsg" class="alert alert--error">{{ errorMsg }}</div>
      <div v-if="successMsg" class="alert alert--success">{{ successMsg }}</div>

      <button type="submit" class="btn btn--primary" :disabled="submitting">
        {{ submitting ? '提交中…' : '创建故事' }}
      </button>
    </form>
  </section>
</template>
