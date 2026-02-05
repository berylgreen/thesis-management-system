<template>
  <div class="home">
    <el-container>
      <el-header>
        <h1>毕业论文管理系统</h1>
        <div>
          <span>欢迎，{{ userStore.username }} ({{ roleText }})</span>
          <el-button @click="handleLogout" type="danger" size="small">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <el-card>
          <h2>功能导航</h2>
          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="8">
              <el-card shadow="hover" @click="router.push('/thesis')" class="nav-card">
                <h3>我的论文</h3>
                <p>查看和管理论文</p>
              </el-card>
            </el-col>
            <el-col :span="8" v-if="userStore.role === 'STUDENT'">
              <el-card shadow="hover" class="nav-card">
                <h3>提交论文</h3>
                <p>上传新版本</p>
              </el-card>
            </el-col>
            <el-col :span="8" v-if="userStore.role === 'TEACHER'">
              <el-card shadow="hover" @click="router.push('/thesis')" class="nav-card">
                <h3>批改论文</h3>
                <p>查看待批改论文</p>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const roleText = computed(() => {
  const roleMap = {
    STUDENT: '学生',
    TEACHER: '教师',
    ADMIN: '管理员'
  }
  return roleMap[userStore.role] || userStore.role
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f5f5f5;
}

.el-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #409eff;
  color: white;
}

.nav-card {
  cursor: pointer;
  text-align: center;
  transition: transform 0.3s;
}

.nav-card:hover {
  transform: translateY(-5px);
}
</style>
