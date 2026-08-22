import api from '@/api'

// ---- Types matching the Spring Boot Arc DTOs (v0.1.1 Phase 6 / TASK-152) ----

export interface ArcResponse {
  id: number
  storyId: number
  title: string
  goal: string | null
  targetStartChapter: number
  targetEndChapter: number
  status: string // PLANNED / ACTIVE / COMPLETED
  createdAt: string
  updatedAt: string
}

export interface ArcRequest {
  title: string
  goal?: string
  targetStartChapter: number
  targetEndChapter: number
  status?: string
}

export async function listArcs(storyId: number): Promise<ArcResponse[]> {
  const { data } = await api.get<ArcResponse[]>(`/stories/${storyId}/arcs`)
  return data
}

export async function createArc(storyId: number, req: ArcRequest): Promise<ArcResponse> {
  const { data } = await api.post<ArcResponse>(`/stories/${storyId}/arcs`, req)
  return data
}

export async function updateArc(arcId: number, req: Partial<ArcRequest>): Promise<ArcResponse> {
  const { data } = await api.put<ArcResponse>(`/arcs/${arcId}`, req)
  return data
}
