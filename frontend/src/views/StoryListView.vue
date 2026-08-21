<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStoryStore } from '@/stores/story'

const router = useRouter()
const storyStore = useStoryStore()

onMounted(async () => {
  await storyStore.fetchStories()
})

async function refresh(): Promise<void> {
  await storyStore.fetchStories()
}

function viewStory(id: number): void {
  router.push(`/stories/${id}`)
}
</script>

<template>
  <section class="story-list">
    <div class="story-list__header">
      <h2>故事列表</h2>
      <div class="story-list__actions">
        <button class="btn btn--ghost" @click="refresh" :disabled="storyStore.loading">
          {{ storyStore.loading ? '刷新中…' : '刷新' }}
        </button>
        <RouterLink to="/create" class="btn btn--primary">+ 创建故事</RouterLink>
      </div>
    </div>

    <div v-if="storyStore.error" class="alert alert--error">{{ storyStore.error }}</div>

    <div v-if="storyStore.loading" class="story-list__empty">加载中…</div>

    <div v-else-if="storyStore.stories.length === 0" class="story-list__empty">
      还没有故事，点击「创建故事」开始吧。
    </div>

    <ul v-else class="story-list__items">
      <li
        v-for="story in storyStore.stories"
        :key="story.id"
        class="story-card"
        @click="viewStory(story.id)"
      >
        <span class="story-card__id">#{{ story.id }}</span>
        <span class="story-card__name">{{ story.name }}</span>
        <span class="story-card__date">{{ story.createdAt?.replace('T', ' ').slice(0, 19) }}</span>
      </li>
    </ul>
  </section>
</template>
