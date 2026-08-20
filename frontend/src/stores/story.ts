import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Minimal story store for the M0 bootstrap.
 * Expanded in M1 once the Story REST API exists.
 */
export const useStoryStore = defineStore('story', () => {
  const currentStoryId = ref<number | null>(null)

  function setCurrentStory(id: number | null): void {
    currentStoryId.value = id
  }

  return { currentStoryId, setCurrentStory }
})
