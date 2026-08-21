<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStoryStore } from '@/stores/story'
import StagePlanning from '@/components/StagePlanning.vue'
import MemoryPanel from '@/components/MemoryPanel.vue'

const route = useRoute()
const router = useRouter()
const storyStore = useStoryStore()

const storyId = computed(() => storyStore.currentStory?.id ?? null)

async function loadStory(id: number): Promise<void> {
  await storyStore.fetchStory(id)
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (id) await loadStory(id)
})

// Reload when navigating between stories.
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) await loadStory(Number(newId))
  },
)

function goBack(): void {
  router.push('/stories')
}
</script>

<template>
  <section class="story-detail">
    <button class="btn btn--ghost" @click="goBack">← 返回列表</button>

    <div v-if="storyStore.loading" class="story-list__empty">加载中…</div>

    <div v-else-if="storyStore.error" class="alert alert--error">{{ storyStore.error }}</div>

    <article v-else-if="storyStore.currentStory" class="story-detail__body">
      <h2>{{ storyStore.currentStory.name }}</h2>
      <p class="story-detail__meta">
        <span class="badge">#{{ storyStore.currentStory.id }}</span>
        <span class="badge">{{ storyStore.currentStory.status }}</span>
        <span class="story-detail__date">
          {{ storyStore.currentStory.createdAt?.replace('T', ' ').slice(0, 19) }}
        </span>
      </p>

      <section class="story-detail__section">
        <h3>核心创意</h3>
        <p>{{ storyStore.currentStory.coreIdea }}</p>
      </section>

      <section v-if="storyStore.currentStory.initialStageDirection" class="story-detail__section">
        <h3>初始导演指令</h3>
        <p>{{ storyStore.currentStory.initialStageDirection }}</p>
      </section>

      <section
        v-if="storyStore.currentStory.constraints?.length"
        class="story-detail__section"
      >
        <h3>故事约束</h3>
        <ul class="story-detail__constraints">
          <li
            v-for="c in storyStore.currentStory.constraints"
            :key="c.id"
            class="constraint-item"
          >
            <span class="constraint-item__type">{{ c.type }}</span>
            <span class="constraint-item__content">{{ c.content }}</span>
          </li>
        </ul>
      </section>

      <!-- Stage planning vertical slice (M2) -->
      <StagePlanning v-if="storyId" :story-id="storyId" class="story-detail__section" />

      <!-- Memory vertical slice (M4) -->
      <MemoryPanel v-if="storyId" :story-id="storyId" class="story-detail__section" />
    </article>

    <div v-else class="story-list__empty">未找到故事。</div>
  </section>
</template>
