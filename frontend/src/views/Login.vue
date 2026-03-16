<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>毕业论文管理系统</h2>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" autofocus />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" show-password />
            </el-form-item>
            <el-form-item prop="role">
              <el-select v-model="registerForm.role" placeholder="选择角色" style="width: 100%">
                <el-option label="学生" value="STUDENT" />
                <el-option label="教师" value="TEACHER" />
              </el-select>
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="真实姓名" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱" @keyup.enter="handleRegister" />
            </el-form-item>
            <el-button type="primary" @click="handleRegister" style="width: 100%">注册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { login, register } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  role: '',
  realName: '',
  email: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
    const res = await login(loginForm.value)
    userStore.setUser(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error(error)
  }
}

const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
    const res = await register(registerForm.value)
    userStore.setUser(res.data)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (error) {
    console.error(error)
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 20px;
}
</style>
