import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import GenerationPanel from '@/components/GenerationPanel.vue'

const apiMocks = vi.hoisted(() => ({
  startGeneration: vi.fn(),
  continueGeneration: vi.fn(),
  retryGeneration: vi.fn(),
  pauseGeneration: vi.fn(),
  stopGeneration: vi.fn(),
  getGenerationJob: vi.fn(),
  listStageGenerationJobs: vi.fn(),
}))

vi.mock('@/api/generation', () => ({
  ...apiMocks,
  extractGenerationError: () => 'request failed',
}))

function job(status: string, currentPlanIndex = 0, stageId = 11) {
  return {
    id: 41,
    stageId,
    mode: 'CONTINUOUS',
    currentPlanIndex,
    total: 3,
    status,
    phase: status === 'COMPLETED' || status === 'STOPPED' ? 'CHECKPOINT' : 'WRITING',
    lastError: null,
    createdAt: '2026-08-23T00:00:00',
    updatedAt: '2026-08-23T00:00:00',
  }
}

function buttonByText(wrapper: ReturnType<typeof mount>, label: string) {
  const button = wrapper.findAll('button').find((candidate) => candidate.text() === label)
  if (!button) throw new Error(`button not found: ${label}`)
  return button
}

describe('GenerationPanel release hardening', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    for (const mock of Object.values(apiMocks)) mock.mockReset()
    apiMocks.listStageGenerationJobs.mockResolvedValue([])
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('polls PENDING/RUNNING jobs through 0/3, 1/3, 2/3, 3/3 and stops at COMPLETED', async () => {
    apiMocks.listStageGenerationJobs.mockResolvedValue([job('PENDING', 0)])
    apiMocks.getGenerationJob
      .mockResolvedValueOnce(job('RUNNING', 1))
      .mockResolvedValueOnce(job('RUNNING', 2))
      .mockResolvedValueOnce(job('COMPLETED', 3))

    const wrapper = mount(GenerationPanel, {
      props: { stageId: 11, planCount: 3, stageStatus: 'ACTIVE' },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('进度 0 / 3')

    await vi.advanceTimersByTimeAsync(1500)
    expect(wrapper.text()).toContain('进度 1 / 3')
    await vi.advanceTimersByTimeAsync(1500)
    expect(wrapper.text()).toContain('进度 2 / 3')
    await vi.advanceTimersByTimeAsync(1500)
    expect(wrapper.text()).toContain('3 / 3')
    expect(wrapper.text()).toContain('已完成')

    const terminalCallCount = apiMocks.getGenerationJob.mock.calls.length
    await vi.advanceTimersByTimeAsync(4500)
    expect(apiMocks.getGenerationJob).toHaveBeenCalledTimes(terminalCallCount)
    wrapper.unmount()
  })

  it('clears the old timer when Stage changes and when the component unmounts', async () => {
    apiMocks.listStageGenerationJobs.mockImplementation(async (stageId: number) =>
      stageId === 11 ? [job('RUNNING', 1, 11)] : [job('COMPLETED', 3, 12)],
    )
    apiMocks.getGenerationJob.mockResolvedValue(job('RUNNING', 2, 11))

    const wrapper = mount(GenerationPanel, {
      props: { stageId: 11, planCount: 3, stageStatus: 'ACTIVE' },
    })
    await flushPromises()
    await wrapper.setProps({ stageId: 12 })
    await flushPromises()
    await vi.advanceTimersByTimeAsync(3000)

    expect(apiMocks.getGenerationJob).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('已完成')

    wrapper.unmount()
    await vi.advanceTimersByTimeAsync(3000)
    expect(apiMocks.getGenerationJob).not.toHaveBeenCalled()
  })

  it('offers Pause while RUNNING and shows the safe-checkpoint wait state', async () => {
    apiMocks.listStageGenerationJobs.mockResolvedValue([job('RUNNING', 1)])
    apiMocks.pauseGeneration.mockResolvedValue(job('RUNNING', 1))
    apiMocks.getGenerationJob.mockResolvedValue(job('PAUSED', 1))

    const wrapper = mount(GenerationPanel, {
      props: { stageId: 11, planCount: 3, stageStatus: 'ACTIVE' },
    })
    await flushPromises()

    await buttonByText(wrapper, '暂停').trigger('click')
    await flushPromises()
    expect(apiMocks.pauseGeneration).toHaveBeenCalledWith(41)
    expect(wrapper.text()).toContain('正在等待安全检查点')

    await vi.advanceTimersByTimeAsync(1500)
    expect(wrapper.text()).toContain('已暂停')
    expect(wrapper.text()).not.toContain('正在等待安全检查点')
    wrapper.unmount()
  })

  it('offers Stop while RUNNING and renders STOPPED as a terminal state', async () => {
    apiMocks.listStageGenerationJobs.mockResolvedValue([job('RUNNING', 2)])
    apiMocks.stopGeneration.mockResolvedValue(job('STOPPED', 2))

    const wrapper = mount(GenerationPanel, {
      props: { stageId: 11, planCount: 3, stageStatus: 'ACTIVE' },
    })
    await flushPromises()

    await buttonByText(wrapper, '停止').trigger('click')
    await flushPromises()
    expect(apiMocks.stopGeneration).toHaveBeenCalledWith(41)
    expect(wrapper.text()).toContain('已停止')
    wrapper.unmount()
  })
})
