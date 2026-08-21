import api from '@/api'

// ---- Types matching the Spring Boot Stage DTOs (TASK-015/016) ----

export interface ChapterPlanResponse {
  id: number
  stageId: number
  chapterOrder: number
  goal: string
  expectedProgress: string | null
  createdAt: string
  updatedAt: string
}

export interface StageResponse {
  id: number
  storyId: number
  direction: string
  status: string
  suggestedChapterCount: number
  targetChapterCount: number | null
  plans: ChapterPlanResponse[]
  createdAt: string
  updatedAt: string
}

export interface StageSummary {
  id: number
  storyId: number
  status: string
  suggestedChapterCount: number | null
  targetChapterCount: number | null
  createdAt: string
}

// ---- API calls ----

export async function createStage(
  storyId: number,
  direction: string,
  targetChapterCount?: number,
): Promise<StageResponse> {
  const body: Record<string, unknown> = { direction }
  if (targetChapterCount != null) body.targetChapterCount = targetChapterCount
  const { data } = await api.post<StageResponse>(`/stories/${storyId}/stages`, body)
  return data
}

export async function listStages(storyId: number): Promise<StageSummary[]> {
  const { data } = await api.get<StageSummary[]>(`/stories/${storyId}/stages`)
  return data
}

export async function getStage(stageId: number): Promise<StageResponse> {
  const { data } = await api.get<StageResponse>(`/stages/${stageId}`)
  return data
}

export async function replanStage(stageId: number, targetChapterCount: number): Promise<StageResponse> {
  const { data } = await api.post<StageResponse>(`/stages/${stageId}/replan`, { targetChapterCount })
  return data
}

export async function confirmStage(stageId: number): Promise<StageResponse> {
  const { data } = await api.post<StageResponse>(`/stages/${stageId}/confirm`)
  return data
}

export async function updatePlanGoal(planId: number, goal: string): Promise<ChapterPlanResponse> {
  const { data } = await api.put<ChapterPlanResponse>(`/stages/plans/${planId}`, { goal })
  return data
}

// ---- Error helper ----

export function extractStageError(err: unknown): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const resp = (err as { response: { data?: { code?: string; message?: string }; status?: number } }).response
    if (resp?.data?.code === 'AI_SERVICE_ERROR' && resp.data.message) {
      return resp.data.message
    }
    if (resp?.data?.message) return resp.data.message
    if (resp?.status === 404) return '未找到该阶段'
  }
  return '请求失败，请检查后端服务与 AI 服务是否运行'
}
