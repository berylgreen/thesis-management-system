<template>
  <div class="thesis-list">
    <el-container>
      <el-header>
        <h1>{{ pageTitle }}</h1>
        <div class="header-buttons">
          <el-button type="warning" @click="handleForceSync" :loading="isSyncing" v-if="isTeacher" data-testid="force-sync-button">
            强制更新
          </el-button>
          <el-button type="primary" @click="showCreateDialog = true" v-if="userStore.role === 'STUDENT'">
            新建论文
          </el-button>
        </div>
      </el-header>
      <el-main>
        <el-table :data="theses" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="studentUsername" label="学号" width="150" v-if="isTeacher" />
          <el-table-column prop="studentName" label="学生姓名" width="120" v-if="isTeacher" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="currentVersion" label="当前版本" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewDetail(row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-main>
    </el-container>

    <el-dialog v-model="showCreateDialog" title="新建论文" width="500px">
      <el-form :model="createForm">
        <el-form-item label="论文标题">
          <el-input v-model="createForm.title" placeholder="请输入论文标题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { getMyTheses, createThesis, forceSync } from '../api/thesis'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const theses = ref([])
const showCreateDialog = ref(false)
const createForm = ref({ title: '' })
const isSyncing = ref(false)

const isTeacher = computed(() => {
  return userStore.role === 'TEACHER' || userStore.role === 'ADMIN'
})

const pageTitle = computed(() => {
  return isTeacher.value ? '所有论文' : '我的论文'
})

const loadTheses = async () => {
  try {
    const res = await getMyTheses()
    theses.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const handleCreate = async () => {
  try {
    await createThesis(createForm.value.title)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value.title = ''
    loadTheses()
  } catch (error) {
    console.error(error)
  }
}

const handleForceSync = async () => {
  isSyncing.value = true
  try {
    const res = await forceSync()
    const data = res.data
    ElMessage.success(`同步完成！删除 ${data.deletedVersions || 0} 个版本, ${data.deletedTheses || 0} 篇论文`)
    loadTheses()
  } catch (error) {
    ElMessage.error('同步失败: ' + (error.message || '未知错误'))
  } finally {
    isSyncing.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/thesis/${id}`)
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    SUBMITTED: 'warning',
    REVIEWED: 'success',
    APPROVED: 'success'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    REVIEWED: '已批改',
    APPROVED: '已通过'
  }
  return map[status] || status
}

onMounted(() => {
  loadTheses()
})
</script>

<style scoped>
.thesis-list {
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

.header-buttons {
  display: flex;
  gap: 10px;
}
</style>
