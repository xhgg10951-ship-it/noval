import api from '@/api'

// ---- Types matching the Spring Boot GenerationJob DTO (M5 / TASK-036..041) ----

export type GenerationMode = 'STEP' | 'CONTINUOUS'
export type GenerationStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'PAUSED'
  | 'COMPLETED'
  | 'FAILED'
  | 'STOPPED'
export type GenerationPhase = 'PLANNING' | 'WRITING' | 'MEMORY' | 'CHECKPOINT' | null

export interface GenerationJobResponse {
  id: number
  stageId: number
  mode: GenerationMode
  currentPlanIndex: number
  total: number
  status: GenerationStatus
  phase: GenerationPhase
  lastError: string | null
  createdAt: string
  updatedAt: string
}

// ---- API calls ----

export async function startGeneration(
  stageId: number,
  mode: GenerationMode,
): Promise<GenerationJobResponse> {
  const { data } = await api.post<GenerationJobResponse>(
    `/stages/${stageId}/generate`,
    null,
    { params: { mode } },
  )
  return data
}

export async function continueGeneration(jobId: number): Promise<GenerationJobResponse> {
  const { data } = await api.post<GenerationJobResponse>(`/generation-jobs/${jobId}/continue`)
  return data
}

export async function retryGeneration(jobId: number): Promise<GenerationJobResponse> {
  const { data } = await api.post<GenerationJobResponse>(`/generation-jobs/${jobId}/retry`)
  return data
}

export async function pauseGeneration(jobId: number): Promise<GenerationJobResponse> {
  const { data } = await api.post<GenerationJobResponse>(`/generation-jobs/${jobId}/pause`)
  return data
}

export async function stopGeneration(jobId: number): Promise<GenerationJobResponse> {
  const { data } = await api.post<GenerationJobResponse>(`/generation-jobs/${jobId}/stop`)
  return data
}

export async function getGenerationJob(jobId: number): Promise<GenerationJobResponse> {
  const { data } = await api.get<GenerationJobResponse>(`/generation-jobs/${jobId}`)
  return data
}

export async function listStageGenerationJobs(
  stageId: number,
): Promise<GenerationJobResponse[]> {
  const { data } = await api.get<GenerationJobResponse[]>(`/stages/${stageId}/generation-jobs`)
  return data
}

// ---- Error helper ----

export function extractGenerationError(err: unknown): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const resp = (
      err as { response: { data?: { code?: string; message?: string }; status?: number } }
    ).response
    if (resp?.data?.message) return resp.data.message
    if (resp?.status === 404) return '未找到该生成任务'
  }
  return '生成请求失败，请检查后端与 AI 服务是否运行'
}
