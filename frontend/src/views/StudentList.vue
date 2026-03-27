<template>
  <div class="student-list">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span class="title">学生管理</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学号/姓名"
              clearable
              class="search-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><plus /></el-icon> 新增学生
            </el-button>
            <el-button type="success" @click="handleSyncAllTitles">
              一键统一论文题目
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="students" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="学号" width="160" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="thesisTitle" label="论文题目" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.thesisTitle || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goToTheses(row)">论文</el-button>
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="openResetDialog(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadStudents"
          @current-change="loadStudents"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="isEditing ? '编辑学生' : '新增学生'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="学号" prop="username">
          <el-input v-model="formData.username" :disabled="isEditing" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱（选填）" />
        </el-form-item>
        <el-form-item label="论文题目" prop="thesisTitle">
          <el-input v-model="formData.thesisTitle" placeholder="留空则自动从文件名提取" />
        </el-form-item>
        <el-form-item v-if="!isEditing" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetDialogVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetFormData" :rules="resetRules" label-width="80px">
        <el-form-item label="学生">
          <span>{{ resetTarget.realName }}（{{ resetTarget.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetFormData.newPassword" type="password" show-password placeholder="请输入新密码" @keyup.enter="handleResetPassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStudents, createStudent, updateStudent, deleteStudent, resetPassword, syncAllThesisTitles } from '../api/student'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const router = useRouter()

// 列表数据
const students = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })

// 新增/编辑表单
const formDialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const formData = reactive({
  username: '',
  realName: '',
  email: '',
  thesisTitle: '',
  password: ''
})

const formRules = {
  username: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' },
             { min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

// 重置密码表单
const resetDialogVisible = ref(false)
const resetFormRef = ref(null)
const resetTarget = reactive({ id: null, realName: '', username: '' })
const resetFormData = reactive({ newPassword: '' })
const resetRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' },
                { min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

// 跳转到论文管理页面并筛选该学生
const goToTheses = (row) => {
  router.push({ path: '/theses', query: { student: row.realName } })
}

// ============ 列表操作 ============

const loadStudents = async () => {
  loading.value = true
  try {
    const res = await getStudents({
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value || undefined
    })
    students.value = res.data.records
    pagination.total = Number(res.data.total)
  } catch (error) {
    console.error('加载学生列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadStudents()
}

// ============ 新增/编辑 ============

const openCreateDialog = () => {
  isEditing.value = false
  editingId.value = null
  Object.assign(formData, { username: '', realName: '', email: '', thesisTitle: '', password: '' })
  formDialogVisible.value = true
}

const openEditDialog = (row) => {
  isEditing.value = true
  editingId.value = row.id
  Object.assign(formData, { username: row.username, realName: row.realName, email: row.email || '', thesisTitle: row.thesisTitle || '', password: '' })
  formDialogVisible.value = true
}

const handleSubmit = async () => {
  const form = formRef.value
  if (!form) return
  await form.validate()
  submitting.value = true
  try {
    if (isEditing.value) {
      await updateStudent(editingId.value, { realName: formData.realName, email: formData.email, thesisTitle: formData.thesisTitle })
      ElMessage.success('更新成功')
    } else {
      await createStudent(formData)
      ElMessage.success('创建成功')
    }
    formDialogVisible.value = false
    loadStudents()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// ============ 删除 ============

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生「${row.realName}」（${row.username}）吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    loadStudents()
  } catch (error) {
    if (error !== 'cancel') console.error(error)
  }
}

// ============ 一键统一论文题目 ============

const handleSyncAllTitles = async () => {
  try {
    await ElMessageBox.confirm(
      '将所有学生预设的论文题目同步到其名下的论文记录。未设置论文题目的学生将被跳过。',
      '一键统一论文题目',
      { type: 'info', confirmButtonText: '确定执行', cancelButtonText: '取消' }
    )
    const res = await syncAllThesisTitles()
    const d = res.data
    let msg = `统一完成：${d.students} 个学生、${d.renamed} 个文件已重命名`
    if (d.failed > 0) msg += `，${d.failed} 个失败`
    ElMessage.success(msg)
    loadStudents()
  } catch (error) {
    if (error !== 'cancel') console.error(error)
  }
}

// ============ 重置密码 ============

const openResetDialog = (row) => {
  Object.assign(resetTarget, { id: row.id, realName: row.realName, username: row.username })
  resetFormData.newPassword = ''
  resetDialogVisible.value = true
}

const handleResetPassword = async () => {
  const form = resetFormRef.value
  if (!form) return
  await form.validate()
  submitting.value = true
  try {
    await resetPassword(resetTarget.id, { newPassword: resetFormData.newPassword })
    ElMessage.success('密码重置成功')
    resetDialogVisible.value = false
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}



onMounted(() => {
  loadStudents()
})
</script>

<style scoped>
.student-list {
  padding: 0;
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 220px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}


</style>
