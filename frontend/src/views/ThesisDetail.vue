<template>
  <div class="thesis-detail">
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span class="title">版本管理</span>
          <div class="header-buttons">
            <el-button type="primary" @click="showUploadDialog = true">
              上传新版本
            </el-button>
            <el-button
              type="success"
              :disabled="selectedVersions.length !== 2"
              @click="showCompareDialog = true"
            >
              对比所选版本 ({{ selectedVersions.length }}/2)
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="versions"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        v-loading="loading"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="versionNum" label="版本号" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">V{{ row.versionNum }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatFileName(row.filePath) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="上传时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDownload(row.id)">
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传 Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传新版本" width="500px">
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

    <!-- 版本对比 Dialog -->
    <el-dialog
      v-model="showCompareDialog"
      title="版本对比"
      fullscreen
      destroy-on-close
    >
      <VersionComparer
        v-if="showCompareDialog && selectedVersions.length === 2"
        :version1-id="sortedSelectedVersions[1].id"
        :version2-id="sortedSelectedVersions[0].id"
        :original-file-name="sortedSelectedVersions[1].filePath"
        :revised-file-name="sortedSelectedVersions[0].filePath"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getVersions, uploadVersion, downloadVersion } from '../api/thesis'
import { ElMessage } from 'element-plus'

// 懒加载 VersionComparer 组件
const VersionComparer = defineAsyncComponent(() =>
  import('../components/VersionComparer.vue')
)

const route = useRoute()
const router = useRouter()

const thesisId = route.params.id
const versions = ref([])
const showUploadDialog = ref(false)
const showCompareDialog = ref(false)
const selectedVersions = ref([])
const uploadForm = ref({ remark: '' })
const uploadFile = ref(null)
const loading = ref(false)

// 对选择的版本进行排序：[新版本, 旧版本]
const sortedSelectedVersions = computed(() => {
  if (selectedVersions.value.length !== 2) return []
  return [...selectedVersions.value].sort((a, b) => {
    // 先按创建时间比
    if (a.createdAt !== b.createdAt) {
      return new Date(b.createdAt) - new Date(a.createdAt)
    }
    // 同一时间按版本号比
    return b.versionNum - a.versionNum
  })
})

// 格式化文件名：移除路径和 UUID 前缀
const formatFileName = (filePath) => {
  if (!filePath) return ''
  // 1. 剥离路径，获取基本文件名 (兼容 / 和 \)
  const basename = filePath.split(/[/\\]/).pop() || ''

  // 2. 匹配并移除常见的 UUID 前缀 (36位标准格式 或 32位紧凑格式)
  const uuidPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}[_-]?|^[0-9a-fA-F]{32}[_-]?/
  const match = basename.match(uuidPattern)
  if (match) {
    return basename.substring(match[0].length)
  }
  return basename
}

// 处理版本选择变化
const handleSelectionChange = (selection) => {
  selectedVersions.value = selection
}

const loadVersions = async () => {
  loading.value = true
  try {
    const res = await getVersions(thesisId)
    versions.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
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
    await uploadVersion(thesisId, formData)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    uploadForm.value.remark = ''
    uploadFile.value = null
    loadVersions()
  } catch (error) {
    console.error(error)
  }
}

const handleDownload = async (versionId) => {
  try {
    const res = await downloadVersion(versionId)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `version_${versionId}.docx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    console.error(error)
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

onMounted(() => {
  loadVersions()
})
</script>

<style scoped>
.thesis-detail {
  padding: 0;
}

.detail-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.header-buttons {
  display: flex;
  gap: 12px;
}
</style>
