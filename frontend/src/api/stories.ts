import api from '@/api'

// ---- Types matching the Spring Boot DTOs (TASK-009) ----

export interface ConstraintInput {
  type: string
  content: string
  sortOrder?: number
}

export interface CreateStoryRequest {
  name: string
  coreIdea: string
  initialStageDirection?: string
  defaultTargetCharacters?: number
  targetChapterCount?: number
  writingStyle?: string
  constraints?: ConstraintInput[]
}

export interface ConstraintResponse {
  id: number
  type: string
  content: string
  sortOrder: number
}

export interface StoryResponse {
  id: number
  name: string
  coreIdea: string
  initialStageDirection: string | null
  defaultTargetCharacters: number | null
  targetChapterCount: number | null
  writingStyle: string | null
  status: string
  constraints: ConstraintResponse[]
  createdAt: string
  updatedAt: string
}

export interface StorySummary {
  id: number
  name: string
  createdAt: string
}

// API error shape from GlobalExceptionHandler
export interface ApiErrorResponse {
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

// ---- API calls ----

export async function createStory(req: CreateStoryRequest): Promise<StoryResponse> {
  const { data } = await api.post<StoryResponse>('/stories', req)
  return data
}

export async function getStory(id: number): Promise<StoryResponse> {
  const { data } = await api.get<StoryResponse>(`/stories/${id}`)
  return data
}

// v0.1.1 Phase 6 (TASK-150): partial writing-settings update; null = unchanged.
export interface WritingSettingsUpdate {
  defaultTargetCharacters?: number
  writingStyle?: string
  targetChapterCount?: number
}

export async function updateWritingSettings(
  id: number,
  settings: WritingSettingsUpdate,
): Promise<StoryResponse> {
  const { data } = await api.patch<StoryResponse>(`/stories/${id}/writing-settings`, settings)
  return data
}

export async function listStories(): Promise<StorySummary[]> {
  const { data } = await api.get<StorySummary[]>('/stories')
  return data
}

// ---- Error helper ----

export function extractApiError(err: unknown): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const resp = (err as { response: { data?: ApiErrorResponse; status?: number } }).response
    if (resp?.data?.message) {
      return resp.data.message
    }
    if (resp?.status === 404) return '未找到该故事'
    if (resp?.status === 400) return '输入有误，请检查必填项'
  }
  return '请求失败，请检查后端服务是否运行'
}
