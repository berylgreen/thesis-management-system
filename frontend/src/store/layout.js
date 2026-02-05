import { defineStore } from 'pinia';

export const useLayoutStore = defineStore('layout', {
  state: () => ({
    isSidebarCollapsed: false,
    isMobile: false,
    mobileBreakpoint: 768,
  }),
  getters: {
    sidebarWidth(state) {
      if (state.isMobile) return '0';
      return state.isSidebarCollapsed ? '64px' : '220px';
    },
  },
  actions: {
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed;
    },
    checkIsMobile(width) {
      this.isMobile = width < this.mobileBreakpoint;
      if (this.isMobile) {
        this.isSidebarCollapsed = true;
      }
    },
  },
});
