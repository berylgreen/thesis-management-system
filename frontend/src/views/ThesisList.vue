<template>
  <div class="thesis-list">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span class="title">{{ pageTitle }}</span>
          <div class="header-controls">
            <!-- 学生姓名筛选 -->
            <el-select
              v-if="isTeacher"
              v-model="filterStudent"
              placeholder="全部学生"
              clearable
              filterable
              style="width: 160px"
            >
              <el-option
                v-for="s in studentOptions"
                :key="s"
                :label="s"
                :value="s"
              />
            </el-select>

            <!-- 状态筛选 -->
            <el-select
              v-model="filterStatus"
              placeholder="全部状态"
              clearable
              style="width: 130px"
            >
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="已批改" value="REVIEWED" />
              <el-option label="已通过" value="APPROVED" />
            </el-select>

            <el-divider direction="vertical" />

            <!-- 功能按钮 -->
            <el-button type="primary" @click="openUploadDialog" :disabled="!selectedThesis">
              上传新版本
            </el-button>
            <el-button type="warning" @click="openAnalysisDialog" :disabled="!selectedThesis">
              论文分析
            </el-button>
            <el-button type="success" @click="openVersionSelectDialog" :disabled="!selectedThesis">
              版本对比
            </el-button>

            <el-divider direction="vertical" v-if="isTeacher" />

            <el-button type="warning" plain @click="handleForceSync" :loading="isSyncing" v-if="isTeacher" data-testid="force-sync-button">
              强制更新
            </el-button>
            <el-button type="primary" @click="showCreateDialog = true" v-if="userStore.role === 'STUDENT'">
              新建论文
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredTheses"
        style="width: 100%"
        v-loading="loading"
        highlight-current-row
        @current-change="handleCurrentChange"
        row-class-name="clickable-row"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="studentUsername" label="学号" width="140" v-if="isTeacher" />
        <el-table-column prop="studentName" label="学生姓名" width="100" v-if="isTeacher" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentVersion" label="版本" width="80" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>
    </el-card>

    <!-- 新建论文 Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建论文" width="500px" destroy-on-close>
      <el-form :model="createForm">
        <el-form-item label="论文标题">
          <el-input v-model="createForm.title" placeholder="请输入论文标题" @keyup.enter="handleCreate" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 上传新版本 Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传新版本" width="500px" destroy-on-close>
      <el-form :model="uploadForm">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="uploadForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 版本选择 Dialog（选择 1~2 个版本进行查看/对比） -->
    <el-dialog v-model="showVersionSelectDialog" title="选择版本进行查看/对比" width="700px" destroy-on-close>
      <el-table
        :data="versionList"
        v-loading="versionsLoading"
        size="small"
        border
        stripe
        @selection-change="handleVersionSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="versionNum" label="版本号" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">V{{ row.versionNum }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件名" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatFileName(row.filePath) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="170" />
      </el-table>
      <template #footer>
        <el-button @click="showVersionSelectDialog = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="selectedVersions.length === 0 || selectedVersions.length > 2"
          @click="startCompare"
        >
          {{ selectedVersions.length <= 1 ? '查看' : '对比' }} ({{ selectedVersions.length }}/2)
        </el-button>
      </template>
    </el-dialog>

    <!-- 版本对比/查看 Dialog -->
    <el-dialog
      v-model="showCompareDialog"
      :title="compareDialogTitle"
      fullscreen
      destroy-on-close
    >
      <VersionComparer
        v-if="showCompareDialog && sortedCompareVersions.length >= 1"
        :version1-id="sortedCompareVersions.length >= 2 ? sortedCompareVersions[1].id : null"
        :version2-id="sortedCompareVersions[0].id"
        :original-file-name="sortedCompareVersions.length >= 2 ? sortedCompareVersions[1].filePath : ''"
        :revised-file-name="sortedCompareVersions[0].filePath"
        :single-view-mode="sortedCompareVersions.length === 1"
      />
    </el-dialog>

    <!-- 论文分析 Dialog -->
    <el-dialog
      v-model="showAnalysisDialog"
      title="论文分析"
      fullscreen
      destroy-on-close
    >
      <ThesisAnalysis
        v-if="showAnalysisDialog && selectedThesis"
        :thesis-id="selectedThesis.id"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import {
  getMyTheses, createThesis, forceSync,
  getVersions, uploadVersion, downloadVersion
} from '../api/thesis'
import { ElMessage } from 'element-plus'

// 懒加载重量级组件
const VersionComparer = defineAsyncComponent(() =>
  import('../components/VersionComparer.vue')
)
const ThesisAnalysis = defineAsyncComponent(() =>
  import('../components/ThesisAnalysis.vue')
)

const route = useRoute()
const userStore = useUserStore()

// ==================== 列表数据 ====================
const theses = ref([])
const loading = ref(false)
const searchKeyword = ref(route.query.search || '')
const filterStatus = ref('')
const filterStudent = ref('')
const selectedThesis = ref(null)

watch(() => route.query.search, (val) => {
  searchKeyword.value = val || ''
})

// 学生姓名选项（去重）
const studentOptions = computed(() => {
  const names = theses.value
    .map(t => t.studentName)
    .filter(Boolean)
  return [...new Set(names)].sort()
})

// 联合过滤：关键词 + 状态 + 学生
const filteredTheses = computed(() => {
  let list = theses.value
  if (filterStatus.value) {
    list = list.filter(t => t.status === filterStatus.value)
  }
  if (filterStudent.value) {
    list = list.filter(t => t.studentName === filterStudent.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(t =>
      (t.title && t.title.toLowerCase().includes(kw)) ||
      (t.studentName && t.studentName.toLowerCase().includes(kw)) ||
      (t.studentUsername && t.studentUsername.toLowerCase().includes(kw))
    )
  }
  return list
})

const isTeacher = computed(() => {
  return userStore.role === 'TEACHER' || userStore.role === 'ADMIN'
})

const pageTitle = computed(() => {
  return isTeacher.value ? '所有论文' : '我的论文'
})

const isSyncing = ref(false)

// ==================== 数据加载 ====================
const loadTheses = async () => {
  loading.value = true
  try {
    const res = await getMyTheses()
    theses.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 选中当前行
const handleCurrentChange = (row) => {
  selectedThesis.value = row
}

// ==================== 新建论文 ====================
const showCreateDialog = ref(false)
const createForm = ref({ title: '' })

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

// ==================== 强制同步 ====================
const handleForceSync = async () => {
  isSyncing.value = true
  try {
    const res = await forceSync()
    const data = res.data
    ElMessage.success(`同步完成！删除 ${data.deletedVersions || 0} 个版本, ${data.deletedTheses || 0} 篇论文, 合并 ${data.mergedTheses || 0} 篇重复论文`)
    loadTheses()
  } catch (error) {
    ElMessage.error('同步失败: ' + (error.message || '未知错误'))
  } finally {
    isSyncing.value = false
  }
}

// ==================== 上传版本 ====================
const showUploadDialog = ref(false)
const uploadForm = ref({ remark: '' })
const uploadFile = ref(null)

const openUploadDialog = () => {
  if (!selectedThesis.value) return
  uploadForm.value.remark = ''
  uploadFile.value = null
  showUploadDialog.value = true
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const handleUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  const formData = new FormData()
  formData.append('file', uploadFile.value)
  formData.append('remark', uploadForm.value.remark)

  try {
    await uploadVersion(selectedThesis.value.id, formData)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    loadTheses()
  } catch (error) {
    console.error(error)
  }
}

// ==================== 论文分析 ====================
const showAnalysisDialog = ref(false)

const openAnalysisDialog = () => {
  if (!selectedThesis.value) return
  showAnalysisDialog.value = true
}

// ==================== 版本对比 ====================
const showVersionSelectDialog = ref(false)
const showCompareDialog = ref(false)
const versionList = ref([])
const versionsLoading = ref(false)
const selectedVersions = ref([])

const openVersionSelectDialog = async () => {
  if (!selectedThesis.value) return
  showVersionSelectDialog.value = true
  versionsLoading.value = true
  selectedVersions.value = []
  try {
    const res = await getVersions(selectedThesis.value.id)
    versionList.value = res.data
  } catch (error) {
    console.error(error)
    versionList.value = []
  } finally {
    versionsLoading.value = false
  }
}

const handleVersionSelectionChange = (selection) => {
  selectedVersions.value = selection
}

// 排序：新版本在前
const sortedCompareVersions = ref([])

const startCompare = () => {
  if (selectedVersions.value.length === 0) return
  const sorted = [...selectedVersions.value].sort((a, b) => {
    if (a.createdAt !== b.createdAt) {
      return new Date(b.createdAt) - new Date(a.createdAt)
    }
    return b.versionNum - a.versionNum
  })
  sortedCompareVersions.value = sorted
  showVersionSelectDialog.value = false
  showCompareDialog.value = true
}

const compareDialogTitle = computed(() => {
  const vers = sortedCompareVersions.value
  if (vers.length === 1) {
    return `查看: ${formatFileName(vers[0].filePath) || '文档'}`
  }
  if (vers.length >= 2) {
    return `对比: ${formatFileName(vers[0].filePath) || '新版本'} ↔ ${formatFileName(vers[1].filePath) || '旧版本'}`
  }
  return '版本对比'
})

// ==================== 工具函数 ====================
const formatFileName = (filePath) => {
  if (!filePath) return ''
  const basename = filePath.split(/[/\\]/).pop() || ''
  const uuidPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}[_-]?|^[0-9a-fA-F]{32}[_-]?/
  const match = basename.match(uuidPattern)
  if (match) return basename.substring(match[0].length)
  return basename
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const getStatusType = (status) => {
  const map = { DRAFT: 'info', SUBMITTED: 'warning', REVIEWED: 'success', APPROVED: 'success' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { DRAFT: '草稿', SUBMITTED: '已提交', REVIEWED: '已批改', APPROVED: '已通过' }
  return map[status] || status
}

onMounted(() => {
  loadTheses()
})
</script>

<style scoped>
.thesis-list {
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
  gap: 8px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.header-controls {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

:deep(.clickable-row) {
  cursor: pointer;
}

:deep(.el-table__body tr.current-row > td) {
  background-color: #ecf5ff !important;
}
</style>
