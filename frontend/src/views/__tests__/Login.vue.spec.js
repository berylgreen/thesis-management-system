import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import Login from '../Login.vue'
import { login, register } from '../../api/auth.js'
import { ElMessage } from 'element-plus'

vi.mock('../../api/auth.js')
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))
vi.mock('../../store/user.js', () => ({
  useUserStore: () => ({
    setUser: vi.fn()
  })
}))

describe('Login.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const createWrapper = () => {
    return mount(Login, {
      global: {
        stubs: {
          'el-card': { template: '<div><slot /></div>' },
          'el-tabs': { template: '<div><slot /></div>' },
          'el-tab-pane': { template: '<div><slot /></div>' },
          'el-form': { 
            template: '<form @submit.prevent><slot /></form>',
            methods: {
              validate: vi.fn((cb) => cb ? cb(true) : Promise.resolve(true))
            }
          },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': {
            template: '<input :type="type" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
            props: ['modelValue', 'type', 'placeholder']
          },
          'el-select': {
            template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
            props: ['modelValue']
          },
          'el-option': {
            template: '<option :value="value">{{ label }}</option>',
            props: ['label', 'value']
          },
          'el-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            props: ['type']
          }
        }
      }
    })
  }

  it('should render login form by default', () => {
    const wrapper = createWrapper()
    expect(wrapper.html()).toContain('毕业论文管理系统')
  })

  it('should call login API when login button clicked', async () => {
    // Arrange
    const mockResponse = { data: { token: 'jwt-token', userId: 1, username: 'alice' } }
    login.mockResolvedValue(mockResponse)
    const wrapper = createWrapper()

    // Act
    const usernameInput = wrapper.findAll('input[type="text"]')[0]
    const passwordInput = wrapper.findAll('input[type="password"]')[0]
    await usernameInput.setValue('alice')
    await passwordInput.setValue('secret123')
    
    const loginButton = wrapper.findAll('button').find(btn => btn.text() === '登录')
    await loginButton.trigger('click')
    await wrapper.vm.$nextTick()

    // Assert
    expect(login).toHaveBeenCalledWith({
      username: 'alice',
      password: 'secret123'
    })
    expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
  })

  it('should handle login failure gracefully', async () => {
    // Arrange
    login.mockRejectedValue(new Error('Invalid credentials'))
    const wrapper = createWrapper()

    // Act
    const usernameInput = wrapper.findAll('input[type="text"]')[0]
    const passwordInput = wrapper.findAll('input[type="password"]')[0]
    await usernameInput.setValue('alice')
    await passwordInput.setValue('wrong')
    
    const loginButton = wrapper.findAll('button').find(btn => btn.text() === '登录')
    await loginButton.trigger('click')
    await wrapper.vm.$nextTick()

    // Assert
    expect(login).toHaveBeenCalled()
    expect(ElMessage.success).not.toHaveBeenCalled()
  })

  it('should call register API when register button clicked', async () => {
    // Arrange
    const mockResponse = { data: { token: 'jwt-token', userId: 2 } }
    register.mockResolvedValue(mockResponse)
    const wrapper = createWrapper()

    // Switch to register tab
    wrapper.vm.activeTab = 'register'
    await wrapper.vm.$nextTick()

    // Act - set form values
    wrapper.vm.registerForm = {
      username: 'bob',
      password: 'password',
      role: 'STUDENT',
      realName: 'Bob',
      email: 'bob@example.com'
    }
    await wrapper.vm.$nextTick()

    const registerButton = wrapper.findAll('button').find(btn => btn.text() === '注册')
    await registerButton.trigger('click')
    await wrapper.vm.$nextTick()

    // Assert
    expect(register).toHaveBeenCalledWith({
      username: 'bob',
      password: 'password',
      role: 'STUDENT',
      realName: 'Bob',
      email: 'bob@example.com'
    })
    expect(ElMessage.success).toHaveBeenCalledWith('注册成功')
  })
})
