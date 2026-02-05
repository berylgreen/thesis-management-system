<template>
  <div class="thesis-detail">
    <el-container>
      <el-header>
        <h1>论文详情</h1>
        <el-button @click="router.back()">返回</el-button>
      </el-header>
      <el-main>
        <el-card>
          <h2>版本列表</h2>
          <div style="margin: 10px 0">
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
          <el-table
            :data="versions"
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="versionNum" label="版本号" width="100" />
            <el-table-column prop="filePath" label="文件路径" />
            <el-table-column prop="fileSize" label="大小" width="120">
              <template #default="{ row }">
                {{ formatSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" />
            <el-table-column prop="createdAt" label="上传时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleDownload(row.id)">
                  下载
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-main>
    </el-container>

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
        :version1-id="selectedVersions[0].id"
        :version2-id="selectedVersions[1].id"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, defineAsyncComponent } from 'vue'
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

// 处理版本选择变化
const handleSelectionChange = (selection) => {
  selectedVersions.value = selection
}

const loadVersions = async () => {
  try {
    const res = await getVersions(thesisId)
    versions.value = res.data
  } catch (error) {
    console.error(error)
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
</style>
