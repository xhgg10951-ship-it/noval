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
  currentRevisionId: number | null
  currentRevisionVersion: number | null
  sourceType: string | null // AI_GENERATED / MANUAL_EDIT / AI_REWRITE / AI_POLISH
  status: string | null // DRAFT / APPROVED (v0.1.1 Phase 5)
  memoryExtractionStatus: string | null // PENDING / COMPLETED / FAILED / STALE
  createdAt: string
  updatedAt: string
}

export interface ChapterRevisionResponse {
  id: number
  chapterId: number
  versionNumber: number
  content: string
  sourceType: string
  createdAt: string
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

// v0.1.1 Phase 5 — author workflow over immutable revisions (TASK-142..146)

export async function listRevisions(chapterId: number): Promise<ChapterRevisionResponse[]> {
  const { data } = await api.get<ChapterRevisionResponse[]>(`/chapters/${chapterId}/revisions`)
  return data
}

/** Manual edit: creates a NEW MANUAL_EDIT revision; nothing is overwritten. */
export async function editChapterContent(chapterId: number, content: string): Promise<ChapterResponse> {
  const { data } = await api.put<ChapterResponse>(`/chapters/${chapterId}/content`, { content })
  return data
}

/** DRAFT -> APPROVED (idempotent). */
export async function approveChapter(chapterId: number): Promise<ChapterResponse> {
  const { data } = await api.post<ChapterResponse>(`/chapters/${chapterId}/approve`)
  return data
}

/** Regenerate from the SAME ChapterSpec; creates an AI_REWRITE revision. */
export async function regenerateChapter(
  chapterId: number,
  authorInstruction?: string,
): Promise<ChapterResponse> {
  const body: Record<string, unknown> = {}
  if (authorInstruction?.trim()) body.authorInstruction = authorInstruction.trim()
  const { data } = await api.post<ChapterResponse>(`/chapters/${chapterId}/regenerate`, body)
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
