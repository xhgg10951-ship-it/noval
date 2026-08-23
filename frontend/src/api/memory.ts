import api from '@/api'

// ---- Types matching the Spring Boot Memory DTOs (TASK-034/035) ----

export interface MemoryCandidate {
  id: number
  storyId: number
  sourceChapterId: number | null
  type: string
  subject: string
  field: string | null
  value: string
  suggestedAction: 'AUTO' | 'REVIEW' | 'IGNORE'
  evidence: string | null
  processingStatus: 'PENDING' | 'APPLIED' | 'IGNORED'
  applied: boolean
  createdAt: string
}

export interface CurrentState {
  id: number
  category: string
  subject: string
  field: string
  value: string
}

export interface Relationship {
  id: number
  subjectA: string
  subjectB: string
  description: string
}

export interface StoryMemory {
  id: number
  type: string
  subject: string | null
  description: string
  importance: number
  scope: 'CHAPTER' | 'STAGE' | 'ARC' | 'STORY'
  active: boolean
  sourceChapterId: number | null
  evidence: string | null
}

export interface MemoryView {
  candidates: MemoryCandidate[]
  currentState: CurrentState[]
  relationships: Relationship[]
  storyMemories: StoryMemory[]
}

// ---- API calls ----

export async function getMemoryView(storyId: number): Promise<MemoryView> {
  const { data } = await api.get<MemoryView>(`/stories/${storyId}/memory`)
  return data
}

export async function applyCandidate(id: number): Promise<MemoryCandidate> {
  const { data } = await api.post<MemoryCandidate>(`/memory/candidates/${id}/apply`)
  return data
}

export async function ignoreCandidate(id: number): Promise<MemoryCandidate> {
  const { data } = await api.post<MemoryCandidate>(`/memory/candidates/${id}/ignore`)
  return data
}
