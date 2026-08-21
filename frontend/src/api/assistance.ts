import api from '@/api'

// ---- Types matching the Spring Boot assistance DTOs (M6 / TASK-042..046) ----

export interface DirectionItem {
  title: string
  description: string
}

export interface SuggestDirectionsResponse {
  directions: DirectionItem[]
}

export interface StoryQueryResponse {
  answer: string
}

// ---- API calls ----

export async function suggestDirections(storyId: number): Promise<SuggestDirectionsResponse> {
  const { data } = await api.post<SuggestDirectionsResponse>(
    `/stories/${storyId}/suggest-directions`,
  )
  return data
}

export async function storyQuery(storyId: number, question: string): Promise<StoryQueryResponse> {
  const { data } = await api.post<StoryQueryResponse>(`/stories/${storyId}/story-query`, {
    question,
  })
  return data
}

// ---- Error helper ----

export function extractAssistanceError(err: unknown): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const resp = (
      err as { response: { data?: { code?: string; message?: string }; status?: number } }
    ).response
    if (resp?.data?.message) return resp.data.message
    if (resp?.status === 404) return '未找到该故事'
    if (resp?.status === 502) return 'AI 服务调用失败，请检查 AI Service 是否运行'
  }
  return '请求失败，请检查后端与 AI 服务是否运行'
}
