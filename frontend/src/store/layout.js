import { defineStore } from 'pinia'

export const useLayoutStore = defineStore('layout', {
  state: () => ({
    isSidebarCollapsed: false,
    recentTheses: JSON.parse(localStorage.getItem('recentTheses') || '[]')
  }),
  actions: {
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed
    },
    addRecentThesis(thesis) {
      if (!thesis.id || !thesis.title) return
      const existingIndex = this.recentTheses.findIndex(t => t.id === thesis.id)
      if (existingIndex !== -1) {
        this.recentTheses.splice(existingIndex, 1)
      }
      this.recentTheses.unshift({ id: thesis.id, title: thesis.title })
      if (this.recentTheses.length > 3) {
        this.recentTheses.pop()
      }
      localStorage.setItem('recentTheses', JSON.stringify(this.recentTheses))
    }
  }
})
