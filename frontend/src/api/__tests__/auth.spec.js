import { describe, it, expect, vi, beforeEach } from 'vitest'
import { login, register } from '../auth.js'
import request from '../../utils/request.js'

vi.mock('../../utils/request.js')

describe('API: auth.js', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('should call request with correct parameters', async () => {
      // Arrange
      const loginData = { username: 'alice', password: 'secret123' }
      const mockResponse = { token: 'jwt-token', userId: 1 }
      request.mockResolvedValue(mockResponse)

      // Act
      const result = await login(loginData)

      // Assert
      expect(request).toHaveBeenCalledWith({
        url: '/auth/login',
        method: 'post',
        data: loginData
      })
      expect(result).toEqual(mockResponse)
    })

    it('should handle login failure', async () => {
      // Arrange
      const loginData = { username: 'alice', password: 'wrong' }
      request.mockRejectedValue(new Error('Invalid credentials'))

      // Act + Assert
      await expect(login(loginData)).rejects.toThrow('Invalid credentials')
    })
  })

  describe('register', () => {
    it('should call request with correct parameters', async () => {
      // Arrange
      const registerData = {
        username: 'bob',
        password: 'password',
        role: 'STUDENT',
        realName: 'Bob',
        email: 'bob@example.com'
      }
      const mockResponse = { token: 'jwt-token', userId: 2 }
      request.mockResolvedValue(mockResponse)

      // Act
      const result = await register(registerData)

      // Assert
      expect(request).toHaveBeenCalledWith({
        url: '/auth/register',
        method: 'post',
        data: registerData
      })
      expect(result).toEqual(mockResponse)
    })
  })
})
