<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside
      :width="layoutStore.isSidebarCollapsed ? '64px' : '240px'"
      class="sidebar"
    >
      <div class="logo-container">
        <el-icon size="24" color="#409eff"><school /></el-icon>
        <span v-show="!layoutStore.isSidebarCollapsed" class="logo-text">毕业论文管理</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="layoutStore.isSidebarCollapsed"
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#ffffff"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/">
          <el-icon><house /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>

        <el-menu-item index="/theses">
          <el-icon><files /></el-icon>
          <template #title>论文管理</template>
        </el-menu-item>

        <!-- Recent Theses Section -->
        <div v-if="!layoutStore.isSidebarCollapsed && layoutStore.recentTheses.length > 0" class="menu-divider">
          最近查看
        </div>

        <el-menu-item
          v-for="thesis in layoutStore.recentTheses"
          :key="thesis.id"
          :index="'/thesis/' + thesis.id"
          class="recent-item"
        >
          <el-icon><document /></el-icon>
          <template #title>
            <span class="truncate">{{ thesis.title }}</span>
          </template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer" @click="layoutStore.toggleSidebar">
        <el-icon>
          <expand v-if="layoutStore.isSidebarCollapsed" />
          <fold v-else />
        </el-icon>
      </div>
    </el-aside>

    <el-container class="main-container">
      <!-- Header -->
      <el-header class="top-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-center">
          <el-input
            v-model="searchQuery"
            placeholder="搜索论文..."
            class="global-search"
            clearable
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">{{ userStore.username }}</span>
              <el-icon><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLayoutStore } from '@/store/layout'
import { useUserStore } from '@/store/user'
import {
  House,
  Files,
  Document,
  Search,
  ArrowDown,
  School,
  Expand,
  Fold
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const layoutStore = useLayoutStore()
const userStore = useUserStore()

const searchQuery = ref('')

const activeMenu = computed(() => {
  const { path } = route
  return path
})

const breadcrumbs = computed(() => {
  return route.meta.breadcrumb || []
})

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    // Navigate to profile
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  background-color: #f8fafc;
}

.sidebar {
  background-color: #1e293b;
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 4px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 100;
  overflow-x: hidden;
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.logo-text {
  color: white;
  font-weight: 700;
  font-size: 1.1rem;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding-top: 12px;
}

.menu-divider {
  padding: 20px 20px 8px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.recent-item {
  opacity: 0.8;
}

.recent-item :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
}

.sidebar-footer {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  color: #94a3b8;
  transition: background 0.2s;
}

.sidebar-footer:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: white;
}

.main-container {
  flex-direction: column;
}

.top-header {
  background-color: white;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 99;
}

.header-center {
  flex: 0 1 400px;
}

.global-search {
  --el-input-border-radius: 8px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background-color: #f1f5f9;
}

.username {
  font-weight: 500;
  color: #1e293b;
  font-size: 0.9rem;
}

.main-content {
  padding: 24px;
  background-color: #f8fafc;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:deep(.el-menu-item.is-active) {
  background-color: #3b82f6 !important;
  color: white !important;
  border-radius: 8px;
  margin: 0 8px;
}

:deep(.el-menu-item) {
  margin: 0 8px;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  margin-bottom: 4px;
}

:deep(.el-menu--collapse .el-menu-item) {
  margin: 0 4px;
}
</style>
