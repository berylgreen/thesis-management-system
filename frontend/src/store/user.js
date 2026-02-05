import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  function setUser(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    role.value = data.role

    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = ''

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return {
    token,
    userId,
    username,
    role,
    setUser,
    logout
  }
})
