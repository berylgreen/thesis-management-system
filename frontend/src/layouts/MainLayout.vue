<template>
  <el-container class="main-layout">
    <!-- 侧边栏: 桌面端显示 -->
    <el-aside :width="layoutStore.sidebarWidth" v-if="!layoutStore.isMobile" class="sidebar">
      <div class="logo">
        <el-icon size="24"><school /></el-icon>
        <span v-show="!layoutStore.isSidebarCollapsed">论文管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="layoutStore.isSidebarCollapsed"
        router
        class="el-menu-vertical"
      >
        <el-menu-item index="/theses">
          <el-icon><document /></el-icon>
          <template #title>论文列表</template>
        </el-menu-item>
        <el-menu-item index="/home" v-if="false"> <!-- 示例扩展 -->
          <el-icon><house /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      v-if="layoutStore.isMobile"
    >
      <div class="logo">论文管理系统</div>
      <el-menu :default-active="activeMenu" router @select="drawerVisible = false">
        <el-menu-item index="/theses">
          <el-icon><document /></el-icon>
          <template #title>论文列表</template>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-container>
      <!-- 顶部状态栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon @click="toggleSidebar" class="collapse-btn">
            <expand v-if="layoutStore.isSidebarCollapsed || layoutStore.isMobile" />
            <fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-profile">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.username }}</span>
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>角色: {{ userStore.role }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="content-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLayoutStore } from '../store/layout'
import { useUserStore } from '../store/user'
import {
  Document,
  House,
  Expand,
  Fold,
  School,
  ArrowDown,
  UserFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const layoutStore = useLayoutStore()
const userStore = useUserStore()

const drawerVisible = ref(false)

const activeMenu = computed(() => route.path)
const breadcrumbs = computed(() => route.meta.breadcrumb || [])

const toggleSidebar = () => {
  if (layoutStore.isMobile) {
    drawerVisible.value = true
  } else {
    layoutStore.toggleSidebar()
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleResize = () => {
  layoutStore.checkIsMobile(window.innerWidth)
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background-color: #304156;
  color: #fff;
  transition: width 0.3s;
  box-shadow: 2px 0 6px rgba(0,21,41,0.35);
  z-index: 10;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
  background-color: #2b2f3a;
  color: #fff;
  overflow: hidden;
  white-space: nowrap;
  gap: 10px;
}

.el-menu-vertical {
  border-right: none;
  background-color: transparent;
}

.el-menu-vertical :deep(.el-menu-item) {
  color: #bfcbd9;
}

.el-menu-vertical :deep(.el-menu-item:hover) {
  background-color: #263445;
}

.el-menu-vertical :deep(.el-menu-item.is-active) {
  color: #409eff;
  background-color: #263445;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-profile:hover {
  background-color: #f6f6f6;
}

.username {
  font-size: 14px;
  color: #606266;
}

.content-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* 过渡动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
