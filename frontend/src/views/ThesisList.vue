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


            <el-checkbox v-if="isTeacher" v-model="onlyShowLatest" style="margin-right: 15px;">
              仅显示学生最新论文
            </el-checkbox>

            <!-- 功能按钮 -->
            <el-button type="primary" @click="openUploadDialog" :disabled="!selectedThesis">
              上传新版本
            </el-button>
            <el-button type="warning" @click="openAnalysisDialog" :disabled="!selectedThesis">
              论文分析
            </el-button>
            <el-button type="success" @click="handleDirectCompare" :disabled="selectedTheses.length === 0 || selectedTheses.length > 2" :loading="compareLoading">
              论文对比 ({{ selectedTheses.length }}/2)
            </el-button>
            <el-button type="info" plain @click="handleBatchRename" :disabled="selectedTheses.length === 0" :loading="renaming" v-if="isTeacher">
              批量重命名 ({{ selectedTheses.length }})
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
        ref="thesisTableRef"
        :data="filteredTheses"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
        row-class-name="clickable-row"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="studentUsername" label="学号" width="140" v-if="isTeacher" />
        <el-table-column prop="studentName" label="学生姓名" width="100" v-if="isTeacher" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="currentVersion" label="版本" width="80" align="center" />
        <el-table-column prop="fileDate" label="文件日期" width="120" align="center" />
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
      :title="analysisDialogTitle"
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
import { batchRenameFiles } from '../api/student'
import { ElMessage, ElMessageBox } from 'element-plus'

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

const filterStudent = ref(route.query.student || '')
const selectedTheses = ref([])
const selectedThesis = computed(() => selectedTheses.value.length > 0 ? selectedTheses.value[0] : null)
const onlyShowLatest = ref(true)

watch(() => route.query.search, (val) => {
  searchKeyword.value = val || ''
})

// 监听 route.query.student 变化
watch(() => route.query.student, (val) => {
  filterStudent.value = val || ''
})

// 学生姓名选项（去重）
const studentOptions = computed(() => {
  const names = theses.value
    .map(t => t.studentName)
    .filter(Boolean)
  return [...new Set(names)].sort()
})

const isTeacher = computed(() => {
  return userStore.role === 'TEACHER' || userStore.role === 'ADMIN'
})

// 联合过滤：关键词 + 状态 + 学生 + 仅显示最新
const filteredTheses = computed(() => {
  let list = theses.value

  // 先按仅显示最新过滤
  if (isTeacher.value && onlyShowLatest.value) {
    const latestMap = new Map()
    list.forEach(t => {
      const studentId = t.studentUsername || t.studentName
      if (!studentId) {
        latestMap.set(`NO_STUDENT_${t.id}`, t)
        return
      }
      const existing = latestMap.get(studentId)
      if (!existing) {
        latestMap.set(studentId, t)
      } else {
        const date1 = t.fileDate || ''
        const date2 = existing.fileDate || ''
        if (date1 > date2) {
          latestMap.set(studentId, t)
        } else if (date1 === date2 && t.id > existing.id) {
          latestMap.set(studentId, t)
        }
      }
    })
    list = Array.from(latestMap.values())
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

// 复选框选中变化
const thesisTableRef = ref(null)
const handleSelectionChange = (selection) => {
  selectedTheses.value = selection
}

// 点击行任意位置切换选中
const handleRowClick = (row) => {
  thesisTableRef.value.toggleRowSelection(row)
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
    ElMessage.success(`同步完成！删除 ${data.deletedVersions || 0} 个版本, ${data.deletedTheses || 0} 篇论文`)
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
const analysisVersionName = ref('')

const analysisDialogTitle = computed(() => {
  const title = selectedThesis.value?.title || '论文'
  return analysisVersionName.value
    ? `论文分析 - ${analysisVersionName.value}`
    : `论文分析 - ${title}`
})

const openAnalysisDialog = async () => {
  if (!selectedThesis.value) return
  // 获取最新版本文件名
  try {
    const res = await getVersions(selectedThesis.value.id)
    const versions = res.data
    if (versions && versions.length > 0) {
      analysisVersionName.value = formatFileName(versions[0].filePath)
    } else {
      analysisVersionName.value = ''
    }
  } catch {
    analysisVersionName.value = ''
  }
  showAnalysisDialog.value = true
}

// ==================== 论文对比 ====================
const showCompareDialog = ref(false)
const compareLoading = ref(false)
const sortedCompareVersions = ref([])

const handleDirectCompare = async () => {
  if (selectedTheses.value.length === 0 || selectedTheses.value.length > 2) return
  compareLoading.value = true
  try {
    // 获取每篇选中论文的最新版本
    const versionPromises = selectedTheses.value.map(t => getVersions(t.id))
    const results = await Promise.all(versionPromises)
    const versions = results.map(res => {
      const list = res.data
      if (!list || list.length === 0) return null
      // 取最新版本（按版本号降序，第一个即最新）
      return list[0]
    }).filter(Boolean)

    if (versions.length === 0) {
      ElMessage.warning('选中的论文没有可用版本')
      return
    }

    // 按创建时间排序，新的在前
    const sorted = [...versions].sort((a, b) => {
      return new Date(b.createdAt) - new Date(a.createdAt)
    })
    sortedCompareVersions.value = sorted
    showCompareDialog.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('获取版本信息失败')
  } finally {
    compareLoading.value = false
  }
}

const compareDialogTitle = computed(() => {
  const vers = sortedCompareVersions.value
  if (vers.length === 1) {
    return `查看: ${formatFileName(vers[0].filePath) || '文档'}`
  }
  if (vers.length >= 2) {
    return `对比: ${formatFileName(vers[0].filePath) || '新版本'} ↔ ${formatFileName(vers[1].filePath) || '旧版本'}`
  }
  return '论文对比'
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


// ==================== 批量重命名 ====================
const renaming = ref(false)

const handleBatchRename = async () => {
  if (selectedTheses.value.length === 0) return
  const theses = selectedTheses.value
  try {
    // 获取所有选中论文的版本
    const versionResults = await Promise.all(theses.map(t => getVersions(t.id)))
    const allVersionIds = []
    const studentIds = new Set()
    let totalFiles = 0
    for (let i = 0; i < theses.length; i++) {
      const versions = versionResults[i].data || []
      totalFiles += versions.length
      studentIds.add(theses[i].studentId)
      versions.forEach(v => allVersionIds.push({ studentId: theses[i].studentId, versionId: v.id }))
    }
    if (totalFiles === 0) {
      ElMessage.warning('选中的论文没有可重命名的文件')
      return
    }
    await ElMessageBox.confirm(
       `确定将 ${theses.length} 篇论文重命名为规范格式：姓名+学号_论文题目_日期.ext？`,
       '批量重命名',
       { type: 'info', confirmButtonText: '确定', cancelButtonText: '取消' }
     )
    renaming.value = true
    // 按学生分组调用重命名接口
    const grouped = {}
    allVersionIds.forEach(item => {
      if (!grouped[item.studentId]) grouped[item.studentId] = []
      grouped[item.studentId].push(item.versionId)
    })
    let totalSuccess = 0
    let totalFailed = 0
    for (const [studentId, versionIds] of Object.entries(grouped)) {
      const res = await batchRenameFiles({ studentId: Number(studentId), versionIds })
      totalSuccess += res.data.success || 0
      totalFailed += res.data.failed || 0
      if (res.data.errors?.length > 0) console.warn('重命名错误:', res.data.errors)
    }
    ElMessage.success(`重命名完成: 成功 ${totalSuccess} 个, 失败 ${totalFailed} 个`)
    loadTheses()
  } catch (error) {
    if (error !== 'cancel') console.error(error)
  } finally {
    renaming.value = false
  }
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
