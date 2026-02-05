import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import { useUserStore } from '@/store/user'
import ThesisList from '@/views/ThesisList.vue'
import { forceSync, getMyTheses } from '@/api/thesis'
import { ElMessage } from 'element-plus'

// --- Mocks Section ---

vi.mock('@/api/thesis', () => ({
  getMyTheses: vi.fn().mockResolvedValue({ data: [] }),
  forceSync: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  }),
  RouterLink: {
    template: '<a><slot/></a>'
  }
}))

vi.mock('element-plus', async (importOriginal) => {
  const original = await importOriginal()
  return {
    ...original,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

// --- Test Suite ---

describe('ThesisList.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const mountComponentWithRole = (role) => {
    const userStore = useUserStore()
    userStore.role = role

    const wrapper = mount(ThesisList, {
        global: {
            plugins: [ElementPlus]
        }
    })
    return wrapper
  }

  describe('Force Sync Button Visibility', () => {
    it('should display the force sync button for TEACHER role', () => {
      const wrapper = mountComponentWithRole('TEACHER')
      expect(wrapper.find('[data-testid="force-sync-button"]').exists()).toBe(true)
    })

    it('should display the force sync button for ADMIN role', () => {
      const wrapper = mountComponentWithRole('ADMIN')
      expect(wrapper.find('[data-testid="force-sync-button"]').exists()).toBe(true)
    })

    it('should NOT display the force sync button for STUDENT role', () => {
      const wrapper = mountComponentWithRole('STUDENT')
      expect(wrapper.find('[data-testid="force-sync-button"]').exists()).toBe(false)
    })
  })

  describe('Force Sync Functionality', () => {
    let wrapper;

    beforeEach(() => {
      wrapper = mountComponentWithRole('TEACHER')
      // The component calls getMyTheses in onMounted. Clear this call
      // so we can cleanly assert the call made by the sync handler.
      getMyTheses.mockClear();
    })

    it('handles successful sync, shows success message, and reloads data', async () => {
      // Arrange
      const syncResult = { deletedVersions: 5, deletedTheses: 2 }
      forceSync.mockResolvedValue({ data: syncResult })

      // Act
      await wrapper.find('[data-testid="force-sync-button"]').trigger('click')
      await new Promise(resolve => setTimeout(resolve, 0)); 

      // Assert
      expect(forceSync).toHaveBeenCalledTimes(1)
      expect(ElMessage.success).toHaveBeenCalledWith(`同步完成！删除 ${syncResult.deletedVersions} 个版本, ${syncResult.deletedTheses} 篇论文`)
      // Test the side-effect of loadTheses(), which is calling getMyTheses()
      expect(getMyTheses).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.isSyncing).toBe(false)
    })
    
    it('handles sync failure and shows an error message', async () => {
      // Arrange
      const errorMessage = 'Network Error'
      forceSync.mockRejectedValue(new Error(errorMessage))

      // Act
      await wrapper.find('[data-testid="force-sync-button"]').trigger('click')
      await new Promise(resolve => setTimeout(resolve, 0));

      // Assert
      expect(forceSync).toHaveBeenCalledTimes(1)
      expect(ElMessage.error).toHaveBeenCalledWith('同步失败: ' + errorMessage)
      // On failure, the data should not be reloaded
      expect(getMyTheses).not.toHaveBeenCalled()
      expect(wrapper.vm.isSyncing).toBe(false)
    })

    it('shows loading state on button while syncing', async () => {
      // Arrange
      let resolvePromise;
      const promise = new Promise(resolve => { resolvePromise = resolve; });
      forceSync.mockReturnValue(promise);
      
      // Act
      await wrapper.find('[data-testid="force-sync-button"]').trigger('click')
      await wrapper.vm.$nextTick();

      // Assert
      expect(wrapper.vm.isSyncing).toBe(true)
      
      // Act
      resolvePromise({ data: {} });
      await new Promise(resolve => setTimeout(resolve, 0));

      // Assert
      expect(wrapper.vm.isSyncing).toBe(false)
    })
  })
})