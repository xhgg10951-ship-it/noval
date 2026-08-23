import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MemoryPanel from '@/components/MemoryPanel.vue'

const apiMocks = vi.hoisted(() => ({
  getMemoryView: vi.fn(),
  applyCandidate: vi.fn(),
  ignoreCandidate: vi.fn(),
}))

vi.mock('@/api/memory', () => apiMocks)

describe('MemoryPanel release hardening', () => {
  beforeEach(() => {
    for (const mock of Object.values(apiMocks)) mock.mockReset()
    apiMocks.getMemoryView.mockResolvedValue({
      candidates: [],
      currentState: [],
      relationships: [],
      storyMemories: [{
        id: 7,
        type: 'FORESHADOWING',
        subject: '月蚀教团',
        description: '教团会在北境遗迹重现',
        importance: 5,
        scope: 'ARC',
        active: true,
        sourceChapterId: 12,
        evidence: '第十二章末尾出现月蚀印记',
      }],
    })
  })

  it('renders Memory v2 type, importance, scope, active, source and evidence', async () => {
    const wrapper = mount(MemoryPanel, { props: { storyId: 3 } })
    await flushPromises()

    expect(wrapper.text()).toContain('FORESHADOWING')
    expect(wrapper.text()).toContain('重要度 5')
    expect(wrapper.text()).toContain('范围 ARC')
    expect(wrapper.text()).toContain('启用')
    expect(wrapper.text()).toContain('来源章节 #12')
    expect(wrapper.text()).toContain('依据：第十二章末尾出现月蚀印记')
  })

  it('shows candidate importance/scope and never offers actions for superseded facts', async () => {
    apiMocks.getMemoryView.mockResolvedValue({
      candidates: [{
        id: 9,
        storyId: 3,
        sourceChapterId: 12,
        type: 'PLOT_FACT',
        subject: '林夜',
        field: null,
        value: '已从正文删除的铁剑事实',
        suggestedAction: 'REVIEW',
        evidence: '旧版本正文',
        importance: 5,
        scope: 'STORY',
        processingStatus: 'SUPERSEDED',
        applied: false,
        createdAt: '2026-08-23T00:00:00',
      }],
      currentState: [],
      relationships: [],
      storyMemories: [],
    })

    const wrapper = mount(MemoryPanel, { props: { storyId: 3 } })
    await flushPromises()

    expect(wrapper.text()).toContain('重要度 5')
    expect(wrapper.text()).toContain('范围 STORY')
    expect(wrapper.text()).toContain('已失效')
    expect(wrapper.findAll('button')).toHaveLength(0)
  })
})
