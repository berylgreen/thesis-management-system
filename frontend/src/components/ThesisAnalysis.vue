<template>
  <div class="thesis-analysis" v-loading="loading" element-loading-text="正在分析论文，文献验证可能需要较长时间...">
    <!-- 分析头部 -->
    <div v-if="result" class="analysis-header">
      <el-alert type="success" :closable="false" show-icon>
        <template #title>
          分析完成
        </template>
      </el-alert>
    </div>

    <div v-if="error" class="analysis-error">
      <el-alert type="error" :title="error" :closable="false" show-icon />
    </div>

    <!-- 分析结果 Tab -->
    <el-tabs v-if="result" v-model="activeTab" type="border-card" class="analysis-tabs">
      <!-- ==================== 摘要分析 ==================== -->
      <el-tab-pane label="摘要分析" name="abstract">
        <div class="section">
          <h4 class="section-title">提取的摘要</h4>
          <div class="abstract-text" v-if="result.abstractText">
            {{ result.abstractText }}
          </div>
          <el-empty v-else description="未检测到摘要内容" :image-size="80" />
        </div>

        <div class="section" v-if="result.abstractAnalysis">
          <h4 class="section-title">要素检测</h4>
          <div class="element-grid">
            <div class="element-item" v-for="item in abstractElements" :key="item.label">
              <el-tag :type="item.value ? 'success' : 'danger'" size="large" effect="dark">
                <el-icon class="element-icon">
                  <component :is="item.value ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
                </el-icon>
                {{ item.label }}
              </el-tag>
            </div>
          </div>
          <el-alert
            :type="allElementsPresent ? 'success' : 'warning'"
            :title="result.abstractAnalysis.summary"
            :closable="false"
            show-icon
            style="margin-top: 16px;"
          />
        </div>

        <div class="section" v-if="result.llmAnalysis">
          <h4 class="section-title">AI 评估（LLM）</h4>
          <div class="llm-result">{{ result.llmAnalysis }}</div>
        </div>
      </el-tab-pane>

      <!-- ==================== 目录分析 ==================== -->
      <el-tab-pane label="目录分析" name="toc">
        <div class="section" v-if="result.chapters && result.chapters.length > 0">
          <h4 class="section-title">章节结构</h4>
          <el-table :data="result.chapters" border stripe size="small" class="chapter-table">
            <el-table-column label="章节标题" min-width="300">
              <template #default="{ row }">
                <span :style="{ paddingLeft: (row.level - 1) * 24 + 'px' }">
                  {{ row.title }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="level" label="级别" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.level === 1 ? '' : 'info'">
                  H{{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="段落数" width="100" align="center">
              <template #default="{ row }">
                {{ row.level === 1 ? row.paragraphCount : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="占比" width="200">
              <template #default="{ row }">
                <template v-if="row.level === 1 && row.proportion > 0">
                  <el-progress
                    :percentage="row.proportion"
                    :color="getProportionColor(row.proportion)"
                    :stroke-width="16"
                    :text-inside="true"
                  />
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="未检测到章节结构（可能论文未使用标题样式）" :image-size="80" />

        <div class="section" v-if="result.proportionAnalysis">
          <h4 class="section-title">结构完整性检查</h4>
          <div class="check-grid">
            <div class="check-item">
              <el-tag :type="result.proportionAnalysis.hasRequirements ? 'success' : 'danger'" effect="dark">
                {{ result.proportionAnalysis.hasRequirements ? '✓' : '✗' }} 需求分析/游戏策划
              </el-tag>
            </div>
            <div class="check-item">
              <el-tag :type="result.proportionAnalysis.hasDesign ? 'success' : 'danger'" effect="dark">
                {{ result.proportionAnalysis.hasDesign ? '✓' : '✗' }} 设计
              </el-tag>
            </div>
            <div class="check-item">
              <el-tag :type="result.proportionAnalysis.hasImplementation ? 'success' : 'danger'" effect="dark">
                {{ result.proportionAnalysis.hasImplementation ? '✓' : '✗' }} 实现
              </el-tag>
            </div>
            <div class="check-item">
              <el-tag :type="result.proportionAnalysis.hasTesting ? 'success' : 'danger'" effect="dark">
                {{ result.proportionAnalysis.hasTesting ? '✓' : '✗' }} 测试
              </el-tag>
            </div>
          </div>
          <el-alert
            :type="result.proportionAnalysis.meetsRequirement ? 'success' : 'warning'"
            :title="result.proportionAnalysis.summary"
            :closable="false"
            show-icon
            style="margin-top: 16px;"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 参考文献 ==================== -->
      <el-tab-pane label="参考文献" name="refs">
        <div class="section" v-if="result.references && result.references.length > 0">
          <h4 class="section-title">
            参考文献列表
            <el-tag size="small" style="margin-left: 8px;">共 {{ result.references.length }} 条</el-tag>
            <el-tag size="small" type="success" style="margin-left: 4px;">
              已验证 {{ verifiedCount }} 条
            </el-tag>
            <el-tag size="small" type="danger" style="margin-left: 4px;" v-if="unverifiedCount > 0">
              存疑 {{ unverifiedCount }} 条
            </el-tag>
          </h4>

          <el-table :data="result.references" border stripe size="small" class="ref-table">
            <el-table-column label="#" width="60" align="center">
              <template #default="{ row }">
                [{{ row.index }}]
              </template>
            </el-table-column>
            <el-table-column label="文献信息" min-width="300">
              <template #default="{ row }">
                <div class="ref-info">
                  <div class="ref-title" v-if="row.title">
                    <strong>{{ row.title }}</strong>
                  </div>
                  <div class="ref-meta">
                    <span v-if="row.authors">{{ row.authors }}</span>
                    <span v-if="row.year">({{ row.year }})</span>
                  </div>
                  <div class="ref-raw" v-if="!row.title">{{ row.rawText }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="验证状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.verified ? 'success' : 'danger'" size="small" effect="dark">
                  {{ row.verified ? '已验证' : '存疑' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="正文引用" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.citedInText ? 'success' : 'warning'" size="small">
                  {{ row.citedInText ? '已引用' : '未引用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="验证详情" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.verifyDetail || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="搜索" width="80" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.searchUrl"
                  link
                  type="primary"
                  @click="openUrl(row.searchUrl)"
                >
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="未检测到参考文献" :image-size="80" />
      </el-tab-pane>

      <!-- ==================== 引用检测 ==================== -->
      <el-tab-pane name="citations">
        <template #label>
          引用检测
          <el-badge
            v-if="result.citationIssues && result.citationIssues.length > 0"
            :value="result.citationIssues.length"
            type="danger"
            style="margin-left: 4px;"
          />
        </template>

        <div class="section" v-if="result.citationIssues && result.citationIssues.length > 0">
          <el-alert
            type="warning"
            :title="`发现 ${result.citationIssues.length} 个引用问题`"
            :closable="false"
            show-icon
            style="margin-bottom: 16px;"
          />

          <el-table :data="result.citationIssues" border stripe size="small">
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.type === 'FIGURE' ? 'primary' : row.type === 'TABLE' ? 'warning' : 'danger'"
                  size="small"
                  effect="dark"
                >
                  {{ typeLabels[row.type] || row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标识" width="120" align="center">
              <template #default="{ row }">
                <strong>{{ row.identifier }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="问题描述" min-width="300">
              <template #default="{ row }">
                {{ row.description }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="section">
          <el-alert type="success" title="所有图、表、参考文献均已在正文中引用 ✓" :closable="false" show-icon />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { analyzeThesis } from '../api/thesis'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const props = defineProps({
  thesisId: {
    type: [Number, String],
    required: true
  }
})

const loading = ref(false)
const result = ref(null)
const error = ref(null)
const activeTab = ref('abstract')

const typeLabels = {
  FIGURE: '图',
  TABLE: '表',
  REFERENCE: '文献'
}

// 摘要要素计算
const abstractElements = computed(() => {
  if (!result.value?.abstractAnalysis) return []
  const a = result.value.abstractAnalysis
  return [
    { label: '研究内容/意义', value: a.hasResearchContent },
    { label: '采用的技术', value: a.hasTechnology },
    { label: '解决的问题', value: a.hasProblem },
    { label: '实际效果', value: a.hasResult }
  ]
})

const allElementsPresent = computed(() =>
  abstractElements.value.every(e => e.value)
)

// 文献统计
const verifiedCount = computed(() =>
  result.value?.references?.filter(r => r.verified).length || 0
)
const unverifiedCount = computed(() =>
  result.value?.references?.filter(r => !r.verified).length || 0
)

// 占比颜色
const getProportionColor = (proportion) => {
  if (proportion >= 30) return '#67c23a'
  if (proportion >= 15) return '#409eff'
  return '#909399'
}

const openUrl = (url) => {
  window.open(url, '_blank')
}

const doAnalysis = async () => {
  loading.value = true
  error.value = null
  result.value = null
  try {
    const res = await analyzeThesis(props.thesisId)
    result.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '分析失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  doAnalysis()
})
</script>

<style scoped>
.thesis-analysis {
  min-height: 400px;
  padding: 16px;
}

.analysis-header {
  margin-bottom: 16px;
}

.analysis-error {
  margin-bottom: 16px;
}

.analysis-tabs {
  border-radius: 8px;
  overflow: hidden;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
}

.abstract-text {
  background: #f5f7fa;
  border-left: 4px solid #409eff;
  padding: 16px 20px;
  border-radius: 4px;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
}

.element-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.element-item {
  display: flex;
  align-items: center;
}

.element-icon {
  margin-right: 4px;
}

.check-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.check-item {
  display: flex;
  align-items: center;
}

.ref-info {
  line-height: 1.6;
}

.ref-title {
  color: #303133;
}

.ref-meta {
  font-size: 12px;
  color: #909399;
}

.ref-raw {
  font-size: 13px;
  color: #606266;
}

.llm-result {
  background: #fdf6ec;
  border-left: 4px solid #e6a23c;
  padding: 16px 20px;
  border-radius: 4px;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
}

.chapter-table,
.ref-table {
  margin-top: 8px;
}

:deep(.el-table .cell) {
  line-height: 1.6;
}

:deep(.el-tabs__content) {
  padding: 20px;
}
</style>
