import api from '@/api'

// ---- Types matching the Spring Boot Chapter DTOs (TASK-023/024) ----

export interface ChapterResponse {
  id: number
  storyId: number
  stageId: number
  planId: number | null
  chapterNumber: number
  title: string
  content: string
  summary: string | null
  generationStatus: string
  createdAt: string
  updatedAt: string
}

// ---- API calls ----

export async function generateNextChapter(stageId: number): Promise<ChapterResponse> {
  const { data } = await api.post<ChapterResponse>(`/stages/${stageId}/chapters`)
  return data
}

export async function listStageChapters(stageId: number): Promise<ChapterResponse[]> {
  const { data } = await api.get<ChapterResponse[]>(`/stages/${stageId}/chapters`)
  return data
}

export async function getChapter(chapterId: number): Promise<ChapterResponse> {
  const { data } = await api.get<ChapterResponse>(`/chapters/${chapterId}`)
  return data
}

// ---- Error helper ----

export function extractChapterError(err: unknown): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const resp = (err as { response: { data?: { code?: string; message?: string }; status?: number } }).response
    if (resp?.data?.code === 'NO_PENDING_CHAPTER') return '该阶段所有章节计划均已生成'
    if (resp?.data?.code === 'AI_SERVICE_ERROR' && resp.data.message) return resp.data.message
    if (resp?.data?.message) return resp.data.message
    if (resp?.status === 404) return '未找到该阶段'
  }
  return '请求失败，请检查后端服务与 AI 服务是否运行'
}
