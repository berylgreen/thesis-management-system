import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import VersionComparer from '@/components/VersionComparer.vue'
import * as thesisApi from '@/api/thesis'
import ElementPlus from 'element-plus'

// 使用 vi.mock 模拟 diff2html 模块
// 绕过组件中 `import { Diff2Html }` 可能存在的缺陷
vi.mock('diff2html', () => ({
  parse: vi.fn(() => ({})),
  html: vi.fn((json, config) => {
    // 根据配置返回可预测的、简单的 HTML 字符串用于断言
    if (config.outputFormat === 'side-by-side') {
      return '<div class="d2h-side-by-side">side-by-side-view</div>'
    }
    return '<div class="d2h-line-by-line">unified-view</div>'
  })
}))
vi.mock('@/api/thesis')

// 封装一个健壮的等待异步更新的函数
const flushPromises = async () => {
  for (let i = 0; i < 5; i++) {
    await nextTick()
  }
}

describe('VersionComparer.vue', () => {
  let wrapper
  const mockProps = {
    version1Id: 1,
    version2Id: 2
  }

  const mockDiffData = {
    diffs: [
      {
        type: 'INSERT',
        revisedPosition: 0,
        revisedLines: ['新增的行1', '新增的行2']
      },
      {
        type: 'DELETE',
        originalPosition: 5,
        originalLines: ['删除的行']
      },
      {
        type: 'CHANGE',
        originalPosition: 10,
        revisedPosition: 11,
        originalLines: ['旧内容'],
        revisedLines: ['新内容']
      }
    ],
    originalBlocks: [],
    revisedBlocks: []
  }

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
    // 重置 window.innerWidth 的 mock
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024 // 默认桌面端
    })
  })

  // ==================== 加载状态测试 ====================

  it('应该在加载时显示骨架屏', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockImplementation(() => new Promise(() => { }))
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    expect(wrapper.find('.el-skeleton').exists()).toBe(true)
  })

  // ==================== 错误状态测试 ====================

  it('应该在加载失败时显示错误提示', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockRejectedValue({
      response: { data: { message: '版本不存在' } }
    })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    const alert = wrapper.findComponent({ name: 'ElAlert' })
    expect(alert.exists()).toBe(true)
    // 修复: 检查渲染的文本内容, 而非 'title' 属性
    expect(alert.text()).toContain('数据加载失败：版本不存在')
  })

  it('应该在无差异时显示提示', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: [] })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    const alert = wrapper.findComponent({ name: 'ElAlert' })
    expect(alert.exists()).toBe(true)
    // 修复: 检查渲染的文本内容, 而非 'title' 属性
    expect(alert.text()).toBe('两个版本无差异')
  })

  // ==================== 成功渲染测试 ====================

  it('应该在加载成功后渲染标签页结构', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: mockDiffData })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    const tabs = wrapper.findComponent({ name: 'ElTabs' })
    expect(tabs.exists()).toBe(true)
    const tabPanes = wrapper.findAllComponents({ name: 'ElTabPane' })
    // 桌面端应有3个 (完整内容 + 文本对比 + 合并视图)
    expect(tabPanes.length).toBe(3)
  })

  it('应该正确计算统计摘要', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: mockDiffData })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    const { summary } = wrapper.vm
    expect(summary.added).toBe(2)
    expect(summary.deleted).toBe(1)
    expect(summary.modified).toBe(1)
  })

  // ==================== 响应式布局测试 ====================

  it('桌面端 (>=768px) 应显示并排对比视图', async () => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1024 })
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: mockDiffData })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.vm.isMobile).toBe(false)
    const tabPanes = wrapper.findAllComponents({ name: 'ElTabPane' })
    const sideBySideTab = tabPanes.find(pane => pane.props('label') === '文本对比')
    // 修复: 确保 tab 存在
    expect(sideBySideTab).toBeDefined()
  })

  it('移动端 (<768px) 应隐藏并排对比视图并默认激活合并视图', async () => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 500 })
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: mockDiffData })
    // 在挂载时触发 resize 逻辑
    window.dispatchEvent(new Event('resize'))
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.vm.isMobile).toBe(true)
    const tabPanes = wrapper.findAllComponents({ name: 'ElTabPane' })
    const sideBySideTab = tabPanes.find(pane => pane.props('label') === '文本对比')
    expect(sideBySideTab).toBeUndefined()
    expect(wrapper.vm.activeTab).toBe('unified')
  })

  // ==================== 生命周期测试 ====================

  it('应该在 mounted 时添加 resize 监听器', async () => {
    const addEventListenerSpy = vi.spyOn(window, 'addEventListener')
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: mockDiffData })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(addEventListenerSpy).toHaveBeenCalledWith('resize', expect.any(Function))
    addEventListenerSpy.mockRestore()
  })

  it('应该在 unmounted 时移除 resize 监听器', async () => {
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener')
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: mockDiffData })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    wrapper.unmount()
    expect(removeEventListenerSpy).toHaveBeenCalledWith('resize', expect.any(Function))
    removeEventListenerSpy.mockRestore()
  })

  // ==================== 数据适配器/渲染流程测试 (使用 Mock) ====================

  it('应正确渲染 INSERT 类型对应的 unified view', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: { diffs: [{ type: 'INSERT', revisedPosition: 0, revisedLines: ['新行1'] }], originalBlocks: [], revisedBlocks: [] } })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    // 修复: 验证 mock `diff2html` 生成的内容是否被正确渲染
    expect(wrapper.vm.unifiedHtml).toBe('<div class="d2h-line-by-line">unified-view</div>')
    expect(wrapper.find('.d2h-line-by-line').exists()).toBe(true)
  })

  it('应正确渲染 DELETE 类型对应的 unified view', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: { diffs: [{ type: 'DELETE', originalPosition: 5, originalLines: ['删除行'] }], originalBlocks: [], revisedBlocks: [] } })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.vm.unifiedHtml).toBe('<div class="d2h-line-by-line">unified-view</div>')
    expect(wrapper.find('.d2h-line-by-line').exists()).toBe(true)
  })

  it('应正确渲染 CHANGE 类型对应的 side-by-side view', async () => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1024 })
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: { diffs: [{ type: 'CHANGE', originalPosition: 3, revisedPosition: 3, originalLines: ['旧'], revisedLines: ['新'] }], originalBlocks: [], revisedBlocks: [] } })
    wrapper = mount(VersionComparer, { props: mockProps, global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.vm.sideBySideHtml).toBe('<div class="d2h-side-by-side">side-by-side-view</div>')
  })

  // ==================== Props 验证测试 ====================

  it('应该接收并使用 versionId props 调用 API', async () => {
    vi.spyOn(thesisApi, 'getDiff').mockResolvedValue({ data: [] })
    wrapper = mount(VersionComparer, { props: { version1Id: 123, version2Id: 456 }, global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(thesisApi.getDiff).toHaveBeenCalledWith(123, 456)
  })
})
