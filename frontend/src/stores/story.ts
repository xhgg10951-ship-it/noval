import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createStory as apiCreateStory,
  getStory as apiGetStory,
  listStories as apiListStories,
  type CreateStoryRequest,
  type StoryResponse,
  type StorySummary,
} from '@/api/stories'

/**
 * Story store — M1 vertical slice.
 * Holds the current story, the story list, loading/error state.
 */
export const useStoryStore = defineStore('story', () => {
  const currentStoryId = ref<number | null>(null)
  const currentStory = ref<StoryResponse | null>(null)
  const stories = ref<StorySummary[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  function setCurrentStory(id: number | null): void {
    currentStoryId.value = id
  }

  async function createStory(req: CreateStoryRequest): Promise<StoryResponse> {
    const created = await apiCreateStory(req)
    currentStory.value = created
    currentStoryId.value = created.id
    // Prepend to the list so the UI reflects the new story immediately.
    stories.value.unshift({ id: created.id, name: created.name, createdAt: created.createdAt })
    return created
  }

  async function fetchStory(id: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentStory.value = await apiGetStory(id)
      currentStoryId.value = id
    } catch (e) {
      currentStory.value = null
      error.value = '加载故事失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchStories(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      stories.value = await apiListStories()
    } catch (e) {
      error.value = '加载故事列表失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    currentStoryId,
    currentStory,
    stories,
    loading,
    error,
    setCurrentStory,
    createStory,
    fetchStory,
    fetchStories,
  }
})
