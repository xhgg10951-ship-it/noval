import { beforeEach, describe, expect, it, vi } from 'vitest'

const http = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}))

vi.mock('@/api', () => ({ default: http }))

import { pauseGeneration, stopGeneration } from '@/api/generation'

describe('generation control API', () => {
  beforeEach(() => {
    http.get.mockReset()
    http.post.mockReset()
    http.post.mockResolvedValue({ data: { id: 7 } })
  })

  it('posts Pause and Stop to the Spring generation-job endpoints', async () => {
    await pauseGeneration(7)
    await stopGeneration(7)

    expect(http.post).toHaveBeenNthCalledWith(1, '/generation-jobs/7/pause')
    expect(http.post).toHaveBeenNthCalledWith(2, '/generation-jobs/7/stop')
  })
})
