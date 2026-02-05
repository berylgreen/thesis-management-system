import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ThesisDetail from '../ThesisDetail.vue'
import { getVersions, uploadVersion, downloadVersion } from '../../api/thesis.js'
import { ElMessage } from 'element-plus'

vi.mock('../../api/thesis.js')
vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { id: '1' }
  }),
  useRouter: () => ({
    back: vi.fn()
  })
}))

global.URL.createObjectURL = vi.fn(() => 'blob:url')
global.Blob = vi.fn((content, options) => ({ content, options }))

describe('ThesisDetail.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  const createWrapper = () => {
    return mount(ThesisDetail, {
      global: {
        stubs: {
          'el-container': { template: '<div><slot /></div>' },
          'el-header': { template: '<div><slot /></div>' },
          'el-main': { template: '<div><slot /></div>' },
          'el-card': { template: '<div><slot /></div>' },
          'el-table': { 
            template: '<div class="el-table"><slot /></div>',
            props: ['data']
          },
          'el-table-column': { 
            template: '<div><slot /></div>',
            props: ['prop', 'label', 'width']
          },
          'el-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            props: ['type', 'size']
          },
          'el-dialog': {
            template: '<div v-if="modelValue" class="el-dialog"><slot /><slot name="footer" /></div>',
            props: ['modelValue', 'title', 'width']
          },
          'el-form': { template: '<form><slot /></form>', props: ['model'] },
          'el-form-item': { template: '<div><slot /></div>', props: ['label'] },
          'el-input': {
            template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
            props: ['modelValue', 'type', 'placeholder']
          },
          'el-upload': {
            template: '<div class="el-upload"><slot /></div>',
            props: ['autoUpload', 'limit', 'onChange']
          }
        }
      }
    })
  }

  it('should fetch and display versions on mount', async () => {
    const mockVersions = [
      { id: 1, versionNum: 1, filePath: '/path/file1.docx', fileSize: 1024 },
      { id: 2, versionNum: 2, filePath: '/path/file2.docx', fileSize: 2048 }
    ]
    getVersions.mockResolvedValue({ data: mockVersions })

    const wrapper = createWrapper()
    await flushPromises()

    expect(getVersions).toHaveBeenCalledWith('1')
    expect(wrapper.vm.versions).toEqual(mockVersions)
  })

  it('should open upload dialog when button is clicked', async () => {
    getVersions.mockResolvedValue({ data: [] })
    const wrapper = createWrapper()
    await flushPromises()

    const uploadButton = wrapper.findAll('button').find(btn => btn.text().includes('上传新版本'))
    await uploadButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.showUploadDialog).toBe(true)
  })

  it('should show warning when uploading without file', async () => {
    getVersions.mockResolvedValue({ data: [] })
    const wrapper = createWrapper()
    await flushPromises()

    wrapper.vm.showUploadDialog = true
    wrapper.vm.uploadFile = null
    await wrapper.vm.$nextTick()

    const uploadButton = wrapper.findAll('button').find(btn => btn.text() === '上传')
    await uploadButton.trigger('click')
    await flushPromises()

    expect(ElMessage.warning).toHaveBeenCalledWith('请选择文件')
    expect(uploadVersion).not.toHaveBeenCalled()
  })

  it('should format file size correctly', () => {
    getVersions.mockResolvedValue({ data: [] })
    const wrapper = createWrapper()

    expect(wrapper.vm.formatSize(0)).toBe('0 B')
    expect(wrapper.vm.formatSize(1024)).toBe('1.00 KB')
    expect(wrapper.vm.formatSize(1048576)).toBe('1.00 MB')
  })
})
