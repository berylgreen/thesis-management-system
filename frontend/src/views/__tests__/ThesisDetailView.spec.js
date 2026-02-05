import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ThesisList from '../ThesisList.vue'
import ThesisDetail from '../ThesisDetail.vue'
import * as thesisApi from '../../api/thesis'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'

// Mock 路由
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  }),
  useRoute: () => ({
    params: { id: '123' }
  })
}))

// Mock API
vi.mock('../../api/thesis', () => ({
  getMyTheses: vi.fn(),
  getVersions: vi.fn(),
  createThesis: vi.fn(),
  forceSync: vi.fn()
}))

// Mock User Store
vi.mock('../../store/user', () => ({
  useUserStore: () => ({
    role: 'STUDENT',
    username: 'test_student'
  })
}))

describe('Thesis Detail Functionality', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  describe('ThesisList.vue 跳转功能', () => {
    it('点击“查看详情”按钮应正确跳转到详情页', async () => {
      const mockTheses = [
        { id: 1, title: '测试论文 1', status: 'DRAFT', currentVersion: 1, createdAt: '2026-02-05' }
      ]
      vi.mocked(thesisApi.getMyTheses).mockResolvedValue({ data: mockTheses })

      const wrapper = mount(ThesisList, {
        global: {
          plugins: [ElementPlus]
        }
      })

      await flushPromises()

      // 查找所有按钮并找到文本为“查看详情”的
      const buttons = wrapper.findAll('.el-button')
      const viewBtn = buttons.find(b => b.text().includes('查看详情'))

      expect(viewBtn).toBeDefined()
      await viewBtn.trigger('click')

      expect(mockPush).toHaveBeenCalledWith('/thesis/1')
    })
  })

  describe('ThesisDetail.vue 数据加载', () => {
    it('组件挂载时应调用 getVersions 并渲染列表', async () => {
      const mockVersions = [
        { id: 101, versionNum: 1, filePath: '/uploads/v1.docx', fileSize: 1024, remark: 'Initial', createdAt: '2026-02-05' }
      ]
      vi.mocked(thesisApi.getVersions).mockResolvedValue({ data: mockVersions })

      const wrapper = mount(ThesisDetail, {
        global: {
          plugins: [ElementPlus]
        }
      })

      // 验证 API 调用
      expect(thesisApi.getVersions).toHaveBeenCalledWith('123')

      await flushPromises()

      // 验证渲染
      const text = wrapper.text()
      expect(text).toContain('V1')
      expect(text).toContain('v1.docx')
    })

    it('API 调用失败时应正常结束 loading 状态', async () => {
      vi.mocked(thesisApi.getVersions).mockRejectedValue(new Error('Network Error'))

      const wrapper = mount(ThesisDetail, {
        global: {
          plugins: [ElementPlus]
        }
      })

      await flushPromises()

      // 检查组件内部的 loading 状态是否设为 false
      expect(wrapper.vm.loading).toBe(false)
    })
  })
})
