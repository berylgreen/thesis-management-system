<template>
  <div class="version-comparer">
    <el-skeleton v-if="isLoading" :rows="10" animated />
    <el-alert
      v-else-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <el-tabs v-else v-model="activeTab" type="border-card">


      <!-- 完整内容视图（新增） -->
      <el-tab-pane label="完整内容" name="full-content">
        <div class="full-content-view">
          <!-- 统计摘要 (仅对比模式显示) -->
          <el-row v-if="!isSingleView" :gutter="20" style="padding: 0 0 20px 0">
            <el-col :span="6">
              <el-statistic title="新增行数" :value="summary.added" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="删除行数" :value="summary.deleted" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="修改行数" :value="summary.modified" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="图片/表格" :value="summary.richContent" />
            </el-col>
          </el-row>

          <!-- 高亮开关和导航栏 (仅对比模式显示) -->
          <div v-if="!isSingleView" class="highlight-toggle">
            <el-switch
              v-model="highlightDiff"
              active-text="高亮差异"
              inactive-text="普通视图"
              style="margin-right: 15px"
            />
            <span v-if="highlightDiff" class="highlight-legend">
              <span class="legend-item legend-deleted">删除</span>
              <span class="legend-item legend-added">新增</span>
            </span>
            <!-- 差异导航 -->
            <div v-if="highlightDiff && fullContentDiffCount > 0" class="diff-nav-bar">
              <span class="diff-counter">{{ currentFullContentDiffIndex + 1 }} / {{ fullContentDiffCount }}</span>
              <el-button-group>
                <el-button size="small" @click="goToPrevFullContentDiff" :disabled="currentFullContentDiffIndex <= 0">
                  ↑ 上一处
                </el-button>
                <el-button size="small" @click="goToNextFullContentDiff" :disabled="currentFullContentDiffIndex >= fullContentDiffCount - 1">
                  ↓ 下一处
                </el-button>
              </el-button-group>
            </div>
          </div>
          <el-row :gutter="20">
            <!-- 日期新的放在左边 (Revised/新版本，单版本模式显示"文档内容") -->
            <el-col :span="isSingleView || isMobile || !highlightDiff ? 24 : 12">
              <h4 class="version-title" :title="formattedRevisedFileName">{{ isSingleView ? '文档内容' : '新版本' }}: {{ formattedRevisedFileName || (isSingleView ? '' : '新版本') }}</h4>
              <div 
                class="content-blocks-container" 
                :class="{ 'normal-view-container': !highlightDiff && !isMobile }"
                ref="revisedBlocksContainer" 
                @scroll="onRevisedScroll"
              >
                <div
                  v-for="(block, index) in revisedBlocks"
                  :key="'rev-' + index"
                  :class="['content-block', 'block-' + block.type.toLowerCase(), { 'current-diff-block': isCurrentDiffBlock(index, 'revised') }]"
                  :id="'rev-block-' + index"
                  :ref="el => setBlockRef(el, index, 'revised')"
                >
                  <!-- 文本内容（细粒度高亮） -->
                  <p v-if="block.type === 'TEXT'" class="text-block" v-html="getHighlightedText(index, 'revised')"></p>
                  
                  <!-- 表格内容 -->
                  <div v-else-if="block.type === 'TABLE'" class="table-block" v-html="block.content"></div>
                  
                  <!-- 图片内容 -->
                  <div v-else-if="block.type === 'IMAGE'" class="image-block">
                    <img :src="block.content" alt="文档图片" />
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 旧版本放在右边 (Original/旧版本) - 单版本模式不显示 -->
            <el-col v-if="!isSingleView && highlightDiff && !isMobile" :span="12">
              <h4 class="version-title" :title="formattedOriginalFileName">旧版本: {{ formattedOriginalFileName || '旧版本' }}</h4>
              <div class="content-blocks-container" ref="originalBlocksContainer" @scroll="onOriginalScroll">
                <div
                  v-for="(block, index) in originalBlocks"
                  :key="'orig-' + index"
                  :class="['content-block', 'block-' + block.type.toLowerCase(), { 'current-diff-block': isCurrentDiffBlock(index, 'original') }]"
                  :id="'orig-block-' + index"
                  :ref="el => setBlockRef(el, index, 'original')"
                >
                  <!-- 文本内容（细粒度高亮） -->
                  <p v-if="block.type === 'TEXT'" class="text-block" v-html="getHighlightedText(index, 'original')"></p>
                  
                  <!-- 表格内容 -->
                  <div v-else-if="block.type === 'TABLE'" class="table-block" v-html="block.content"></div>
                  
                  <!-- 图片内容 -->
                  <div v-else-if="block.type === 'IMAGE'" class="image-block">
                    <img :src="block.content" alt="文档图片" />
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 并排对比（仅桌面端 + 非单版本模式） -->
      <el-tab-pane v-if="!isSingleView && !isMobile" label="文本对比" name="side-by-side">
        <div class="diff-view-wrapper">
          <div v-html="sideBySideHtml" class="diff-container" ref="sideBySideContainer"></div>

          <!-- 导航栏 -->
          <div v-if="diffBlocks.length > 0" class="diff-navigator">
            <el-button-group>
              <el-button
                size="small"
                :disabled="currentDiffIndex === 0"
                @click="prevDiff"
              >
                上一个
              </el-button>
              <el-button size="small" disabled>
                {{ currentDiffIndex + 1 }} / {{ diffBlocks.length }}
              </el-button>
              <el-button
                size="small"
                :disabled="currentDiffIndex === diffBlocks.length - 1"
                @click="nextDiff"
              >
                下一个
              </el-button>
            </el-button-group>
          </div>
        </div>
      </el-tab-pane>

      <!-- 合并视图（仅对比模式显示） -->
      <el-tab-pane v-if="!isSingleView" label="合并视图" name="unified">
        <div class="diff-view-wrapper">
          <div v-html="unifiedHtml" class="diff-container" ref="unifiedContainer"></div>

          <!-- 导航栏 -->
          <div v-if="diffBlocks.length > 0" class="diff-navigator">
            <el-button-group>
              <el-button
                size="small"
                :disabled="currentDiffIndex === 0"
                @click="prevDiff"
              >
                上一个
              </el-button>
              <el-button size="small" disabled>
                {{ currentDiffIndex + 1 }} / {{ diffBlocks.length }}
              </el-button>
              <el-button
                size="small"
                :disabled="currentDiffIndex === diffBlocks.length - 1"
                @click="nextDiff"
              >
                下一个
              </el-button>
            </el-button-group>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { getDiff, getVersionContent } from '@/api/thesis'
import * as Diff2Html from 'diff2html'
import 'diff2html/bundles/css/diff2html.min.css'
import DiffMatchPatch from 'diff-match-patch'

const props = defineProps({
  version1Id: {
    type: [String, Number],
    required: false,  // 单版本查看模式不需要 version1
    default: null
  },
  version2Id: {
    type: [String, Number],
    required: true
  },
  originalFileName: {
    type: String,
    default: ''
  },
  revisedFileName: {
    type: String,
    default: ''
  },
  singleViewMode: {
    type: Boolean,
    default: false  // 单版本查看模式
  }
})

// 是否为单版本查看模式
const isSingleView = computed(() => props.singleViewMode || !props.version1Id)

// 格式化文件名：移除 UUID 前缀
const formattedOriginalFileName = computed(() => formatFileName(props.originalFileName))
const formattedRevisedFileName = computed(() => formatFileName(props.revisedFileName))

const isLoading = ref(true)
const error = ref(null)
const backendData = ref([])
const isMobile = ref(window.innerWidth < 768)
const activeTab = ref('full-content')
const sideBySideHtml = ref('')
const unifiedHtml = ref('')

// 富文本内容块
const originalBlocks = ref([])
const revisedBlocks = ref([])

// 高亮差异开关
const highlightDiff = ref(true)

// 差异位置索引（用于高亮）
const diffPositionMap = ref({ original: new Map(), revised: new Map() })

// 初始化 diff-match-patch 实例
const dmp = new DiffMatchPatch()

// 缓存细粒度差异结果
const textDiffCache = ref(new Map())

// 完整文档和导航相关状态
const diffBlocks = ref([])
const currentDiffIndex = ref(0)
const sideBySideContainer = ref(null)
const unifiedContainer = ref(null)

// 完整内容导航相关状态
const originalBlocksContainer = ref(null)
const revisedBlocksContainer = ref(null)
const fullContentDiffList = ref([]) // 有差异的块列表 [{ origIndex, revIndex }]
const currentFullContentDiffIndex = ref(0)
const blockRefs = ref({ original: {}, revised: {} })

// 统计摘要计算
const summary = computed(() => {
  let added = 0
  let deleted = 0
  let modified = 0
  let richContent = 0

  backendData.value.forEach(chunk => {
    if (chunk.type === 'INSERT') {
      added += chunk.revisedLines?.length || 0
    } else if (chunk.type === 'DELETE') {
      deleted += chunk.originalLines?.length || 0
    } else if (chunk.type === 'CHANGE') {
      modified += chunk.revisedLines?.length || 0
    }
  })

  // 统计富文本内容
  originalBlocks.value.forEach(block => {
    if (block.type === 'TABLE' || block.type === 'IMAGE') {
      richContent++
    }
  })

  return { added, deleted, modified, richContent }
})

// 响应式监听
function handleResize() {
  isMobile.value = window.innerWidth < 768
}

// 存储完整文档数据
const fullDiffData = ref(null)

// 判断内容块是否有差异并返回对应的高亮类
function getBlockDiffClass(index, side) {
  if (!highlightDiff.value) return ''
  
  const map = side === 'original' 
    ? diffPositionMap.value.original 
    : diffPositionMap.value.revised
  
  if (map.has(index)) {
    const diffType = map.get(index)
    if (diffType === 'DELETE') return 'highlight-deleted'
    if (diffType === 'INSERT') return 'highlight-added'
    if (diffType === 'CHANGE') return side === 'original' ? 'highlight-deleted' : 'highlight-added'
  }
  return ''
}

// 构建差异位置索引映射
function buildDiffPositionMap(diffs) {
  const originalMap = new Map()
  const revisedMap = new Map()
  
  if (!diffs || diffs.length === 0) {
    diffPositionMap.value = { original: originalMap, revised: revisedMap }
    return
  }
  
  diffs.forEach(chunk => {
    // 标记原始文档中的差异位置
    for (let i = 0; i < (chunk.originalLines?.length || 0); i++) {
      originalMap.set(chunk.originalPosition + i, chunk.type)
    }
    // 标记修订文档中的差异位置
    for (let i = 0; i < (chunk.revisedLines?.length || 0); i++) {
      revisedMap.set(chunk.revisedPosition + i, chunk.type)
    }
  })
  
  diffPositionMap.value = { original: originalMap, revised: revisedMap }
}
// 缓存段落匹配结果
const paragraphMatchCache = ref({ originalToRevised: new Map(), revisedToOriginal: new Map() })

// 计算两个文本的相似度（0-1之间，1表示完全相同）
function calculateSimilarity(text1, text2) {
  if (!text1 && !text2) return 1
  if (!text1 || !text2) return 0
  if (text1 === text2) return 1
  
  // 使用 Levenshtein 距离计算相似度
  const len1 = text1.length
  const len2 = text2.length
  const maxLen = Math.max(len1, len2)
  
  if (maxLen === 0) return 1
  
  // 简化版：使用公共子串长度比例估算相似度
  const diffs = dmp.diff_main(text1, text2)
  let commonLength = 0
  for (const [op, data] of diffs) {
    if (op === 0) commonLength += data.length
  }
  
  return commonLength / maxLen
}

// 构建段落匹配映射（基于相似度）
function buildParagraphMatching() {
  const origBlocks = originalBlocks.value.filter(b => b.type === 'TEXT')
  const revBlocks = revisedBlocks.value.filter(b => b.type === 'TEXT')
  
  const originalToRevised = new Map()
  const revisedToOriginal = new Map()
  
  // 获取实际索引映射
  const origIndices = []
  const revIndices = []
  originalBlocks.value.forEach((b, i) => { if (b.type === 'TEXT') origIndices.push(i) })
  revisedBlocks.value.forEach((b, i) => { if (b.type === 'TEXT') revIndices.push(i) })
  
  // 计算相似度矩阵
  const similarityMatrix = []
  for (let i = 0; i < origBlocks.length; i++) {
    similarityMatrix[i] = []
    for (let j = 0; j < revBlocks.length; j++) {
      similarityMatrix[i][j] = calculateSimilarity(
        origBlocks[i].content || '',
        revBlocks[j].content || ''
      )
    }
  }
  
  // 贪心匹配：按相似度从高到低匹配
  const usedOrig = new Set()
  const usedRev = new Set()
  const matches = []
  
  // 收集所有相似度对
  for (let i = 0; i < origBlocks.length; i++) {
    for (let j = 0; j < revBlocks.length; j++) {
      matches.push({ i, j, similarity: similarityMatrix[i][j] })
    }
  }
  
  // 按相似度降序排序
  matches.sort((a, b) => b.similarity - a.similarity)
  
  // 贪心选择最佳匹配（相似度阈值 > 0.3）
  for (const { i, j, similarity } of matches) {
    if (similarity < 0.3) break // 相似度太低，不匹配
    if (usedOrig.has(i) || usedRev.has(j)) continue
    
    originalToRevised.set(origIndices[i], revIndices[j])
    revisedToOriginal.set(revIndices[j], origIndices[i])
    usedOrig.add(i)
    usedRev.add(j)
  }
  
  paragraphMatchCache.value = { originalToRevised, revisedToOriginal }
  
  // 构建差异列表（用于导航）
  buildFullContentDiffList(originalToRevised, origIndices, usedOrig, revIndices, usedRev)
}

// 构建完整内容差异列表
function buildFullContentDiffList(originalToRevised, origIndices, usedOrig, revIndices, usedRev) {
  const diffList = []
  
  // 遍历原始文档的文本块
  origIndices.forEach((origIndex, i) => {
    const revIndex = originalToRevised.get(origIndex)
    if (revIndex !== undefined) {
      // 匹配到的块，检查是否有差异
      const origText = originalBlocks.value[origIndex]?.content || ''
      const revText = revisedBlocks.value[revIndex]?.content || ''
      if (origText !== revText) {
        diffList.push({ origIndex, revIndex, type: 'CHANGE' })
      }
    } else {
      // 未匹配到的块（删除的内容）
      diffList.push({ origIndex, revIndex: -1, type: 'DELETE' })
    }
  })
  
  // 遍历修订文档中未匹配的块（新增的内容）
  revIndices.forEach((revIndex, j) => {
    if (!usedRev.has(j)) {
      diffList.push({ origIndex: -1, revIndex, type: 'INSERT' })
    }
  })
  
  // 按位置排序
  diffList.sort((a, b) => {
    const posA = a.origIndex >= 0 ? a.origIndex : a.revIndex
    const posB = b.origIndex >= 0 ? b.origIndex : b.revIndex
    return posA - posB
  })
  
  fullContentDiffList.value = diffList
  currentFullContentDiffIndex.value = 0
}

// 计算差异总数
const fullContentDiffCount = computed(() => fullContentDiffList.value.length)

// 设置块的 ref
function setBlockRef(el, index, side) {
  if (el) {
    blockRefs.value[side][index] = el
  }
}

// 判断是否为当前高亮的差异块
function isCurrentDiffBlock(index, side) {
  if (!highlightDiff.value || fullContentDiffList.value.length === 0) return false
  const currentDiff = fullContentDiffList.value[currentFullContentDiffIndex.value]
  if (!currentDiff) return false
  
  if (side === 'original') {
    return currentDiff.origIndex === index
  } else {
    return currentDiff.revIndex === index
  }
}

// 跳转到上一处差异
function goToPrevFullContentDiff() {
  if (currentFullContentDiffIndex.value > 0) {
    currentFullContentDiffIndex.value--
    scrollToCurrentDiff()
  }
}

// 跳转到下一处差异
function goToNextFullContentDiff() {
  if (currentFullContentDiffIndex.value < fullContentDiffList.value.length - 1) {
    currentFullContentDiffIndex.value++
    scrollToCurrentDiff()
  }
}

// 滚动到当前差异位置
function scrollToCurrentDiff() {
  const currentDiff = fullContentDiffList.value[currentFullContentDiffIndex.value]
  if (!currentDiff) return
  
  // 滚动原始文档
  if (currentDiff.origIndex >= 0) {
    const origEl = blockRefs.value.original[currentDiff.origIndex]
    if (origEl) {
      origEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
  
  // 滚动修订文档
  if (currentDiff.revIndex >= 0) {
    const revEl = blockRefs.value.revised[currentDiff.revIndex]
    if (revEl) {
      setTimeout(() => {
        revEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }, 100)
    }
  }
}

// 同步滚动标志（防止递归触发）
let isSyncingScroll = false

// 原始文档滚动时同步修订文档
function onOriginalScroll(event) {
  if (isSyncingScroll) return
  isSyncingScroll = true
  
  const source = originalBlocksContainer.value
  const target = revisedBlocksContainer.value
  
  if (source && target) {
    // 按比例同步滚动位置
    const scrollRatio = source.scrollTop / (source.scrollHeight - source.clientHeight || 1)
    target.scrollTop = scrollRatio * (target.scrollHeight - target.clientHeight)
  }
  
  setTimeout(() => { isSyncingScroll = false }, 10)
}

// 修订文档滚动时同步原始文档
function onRevisedScroll(event) {
  if (isSyncingScroll) return
  isSyncingScroll = true
  
  const source = revisedBlocksContainer.value
  const target = originalBlocksContainer.value
  
  if (source && target) {
    // 按比例同步滚动位置
    const scrollRatio = source.scrollTop / (source.scrollHeight - source.clientHeight || 1)
    target.scrollTop = scrollRatio * (target.scrollHeight - target.clientHeight)
  }
  
  setTimeout(() => { isSyncingScroll = false }, 10)
}

function getHighlightedText(index, side) {
  const blocks = side === 'original' ? originalBlocks.value : revisedBlocks.value
  const otherBlocks = side === 'original' ? revisedBlocks.value : originalBlocks.value
  
  if (!blocks[index] || blocks[index].type !== 'TEXT') {
    return ''
  }
  
  const text = blocks[index].content || ''
  
  // 如果高亮关闭，直接返回转义后的文本
  if (!highlightDiff.value) {
    return escapeHtml(text)
  }
  
  // 使用智能匹配查找对应段落
  const matchMap = side === 'original' 
    ? paragraphMatchCache.value.originalToRevised 
    : paragraphMatchCache.value.revisedToOriginal
  
  const matchedIndex = matchMap.get(index)
  
  // 如果没有匹配到对应段落，整段标记为新增/删除
  if (matchedIndex === undefined) {
    const className = side === 'original' ? 'diff-deleted' : 'diff-added'
    return `<span class="${className}">${escapeHtml(text)}</span>`
  }
  
  const otherText = otherBlocks[matchedIndex]?.content || ''
  
  // 如果两边文本完全相同，不需要高亮
  if (text === otherText) {
    return escapeHtml(text)
  }
  
  // 使用 diff-match-patch 进行细粒度差异对比
  const diffs = dmp.diff_main(
    side === 'original' ? text : otherText,
    side === 'original' ? otherText : text
  )
  dmp.diff_cleanupSemantic(diffs)
  
  // 生成带高亮的 HTML
  let html = ''
  for (const [op, data] of diffs) {
    const escaped = escapeHtml(data)
    if (side === 'original') {
      // 原始文档：显示删除的内容（op === -1）
      if (op === -1) {
        html += `<span class="diff-deleted">${escaped}</span>`
      } else if (op === 0) {
        html += escaped
      }
      // 新增的内容(op === 1)在原始文档中不显示
    } else {
      // 修订文档：显示新增的内容（op === 1）
      if (op === 1) {
        html += `<span class="diff-added">${escaped}</span>`
      } else if (op === 0) {
        html += escaped
      }
      // 删除的内容(op === -1)在修订文档中不显示
    }
  }
  
  return html
}

// HTML 转义函数
function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}


// 文件名格式化函数：剥离路径和可选的UUID前缀
function formatFileName(fileName) {
  if (!fileName) return '';
  // 1. 剥离路径，获取基本文件名
  const basename = fileName.split(/[/\\]/).pop() || '';
  
  // 2. 匹配并移除常见的UUID前缀
  const uuidPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}[_-]?|^[0-9a-fA-F]{32}[_-]?/;
  const match = basename.match(uuidPattern);
  if (match) {
    return basename.substring(match[0].length);
  }
  return basename;
}

// 数据获取
async function fetchData() {
  try {
    // 单版本查看模式：仅获取单个版本的内容
    if (isSingleView.value) {
      const res = await getVersionContent(props.version2Id)
      const data = res.data
      
      if (!data || !data.blocks || data.blocks.length === 0) {
        error.value = '文档内容为空'
      } else {
        revisedBlocks.value = data.blocks || []
        originalBlocks.value = []  // 单版本模式没有原始版本
        activeTab.value = 'full-content'
      }
    } else {
      // 对比模式：获取两个版本的差异
      const res = await getDiff(props.version1Id, props.version2Id)
      fullDiffData.value = res.data

      if (!fullDiffData.value || !fullDiffData.value.diffs || fullDiffData.value.diffs.length === 0) {
        // 即使没有文本差异，也可能有内容要显示
        if (fullDiffData.value?.originalBlocks?.length > 0 || fullDiffData.value?.revisedBlocks?.length > 0) {
          originalBlocks.value = fullDiffData.value.originalBlocks || []
          revisedBlocks.value = fullDiffData.value.revisedBlocks || []
          buildParagraphMatching() // 构建智能段落匹配
          activeTab.value = 'full-content'
        } else {
          error.value = '两个版本无差异'
        }
      } else {
        backendData.value = fullDiffData.value.diffs
        originalBlocks.value = fullDiffData.value.originalBlocks || []
        revisedBlocks.value = fullDiffData.value.revisedBlocks || []
        buildDiffPositionMap(fullDiffData.value.diffs) // 构建差异位置索引
        buildParagraphMatching() // 构建智能段落匹配
        renderDiffs()
      }
    }
  } catch (e) {
    error.value = '数据加载失败：' + (e.response?.data?.message || e.message)
  } finally {
    isLoading.value = false
  }
}

// 渲染 Diff 视图
function renderDiffs() {
  const diffString = convertBackendToDiff2Html(fullDiffData.value)

  if (!diffString) return

  // 使用 diff2html 解析 unified diff 字符串
  const diffJson = Diff2Html.parse(diffString)

  if (!isMobile.value) {
    sideBySideHtml.value = Diff2Html.html(diffJson, {
      drawFileList: false,
      outputFormat: 'side-by-side',
      matching: 'lines'
    })
  }

  unifiedHtml.value = Diff2Html.html(diffJson, {
    drawFileList: false,
    outputFormat: 'line-by-line'
  })
}

// 重建完整文档：从 diff 块标记差异位置
function reconstructFullDocuments(diffs) {
  if (!diffs || diffs.length === 0) {
    return {
      diffBlocks: []
    }
  }

  const blocks = []

  // 按位置排序 diff 块
  const sortedData = [...diffs].sort((a, b) => a.originalPosition - b.originalPosition)

  sortedData.forEach((chunk, index) => {
    // 记录每个 diff 块的信息
    blocks.push({
      index,
      type: chunk.type,
      originalPosition: chunk.originalPosition,
      revisedPosition: chunk.revisedPosition,
      originalLineCount: chunk.originalLines?.length || 0,
      revisedLineCount: chunk.revisedLines?.length || 0
    })
  })

  return {
    diffBlocks: blocks
  }
}

// 数据适配器:后端 FullDiffResult → 标准 unified diff 字符串（带完整上下文）
function convertBackendToDiff2Html(data) {
  if (!data || !data.diffs) return ''

  const { diffs, originalLines, revisedLines } = data

  const reconstructed = reconstructFullDocuments(diffs)
  diffBlocks.value = reconstructed.diffBlocks

  // 生成标准 unified diff 格式字符串，包含完整文档上下文
  const oldName = (formattedOriginalFileName.value || '旧版本').replace(/[\r\n\t]/g, ' ')
  const newName = (formattedRevisedFileName.value || '新版本').replace(/[\r\n\t]/g, ' ')
  let diffString = `--- 旧版本: ${oldName}\n+++ 新版本: ${newName}\n`

  // 创建差异位置索引（用于快速查找某行是否有差异）
  const originalDiffMap = new Map() // originalPosition -> chunk
  const revisedDiffMap = new Map()  // revisedPosition -> chunk

  diffs.forEach(chunk => {
    for (let i = 0; i < (chunk.originalLines?.length || 0); i++) {
      originalDiffMap.set(chunk.originalPosition + i, chunk)
    }
    for (let i = 0; i < (chunk.revisedLines?.length || 0); i++) {
      revisedDiffMap.set(chunk.revisedPosition + i, chunk)
    }
  })

  // 使用完整文档内容生成 unified diff
  // 生成一个包含所有内容的大 hunk
  const totalOriginal = originalLines?.length || 0
  const totalRevised = revisedLines?.length || 0

  diffString += `@@ -1,${totalOriginal} +1,${totalRevised} @@\n`

  // 双指针遍历原始文档和修订文档
  let origIdx = 0
  let revIdx = 0
  const sortedDiffs = [...diffs].sort((a, b) => a.originalPosition - b.originalPosition)
  let diffIdx = 0

  while (origIdx < totalOriginal || revIdx < totalRevised) {
    const currentDiff = sortedDiffs[diffIdx]

    // 如果当前位置没有差异，输出上下文行
    if (!currentDiff ||
        (origIdx < currentDiff.originalPosition && revIdx < currentDiff.revisedPosition)) {
      // 输出未变化的行（上下文）
      const origLine = originalLines[origIdx] || ''
      diffString += ' ' + origLine + '\n'
      origIdx++
      revIdx++
    } else if (currentDiff && origIdx === currentDiff.originalPosition) {
      // 输出差异
      if (currentDiff.type === 'DELETE') {
        currentDiff.originalLines.forEach(line => {
          diffString += '-' + line + '\n'
        })
        origIdx += currentDiff.originalLines.length
      } else if (currentDiff.type === 'INSERT') {
        currentDiff.revisedLines.forEach(line => {
          diffString += '+' + line + '\n'
        })
        revIdx += currentDiff.revisedLines.length
      } else if (currentDiff.type === 'CHANGE') {
        currentDiff.originalLines.forEach(line => {
          diffString += '-' + line + '\n'
        })
        currentDiff.revisedLines.forEach(line => {
          diffString += '+' + line + '\n'
        })
        origIdx += currentDiff.originalLines.length
        revIdx += currentDiff.revisedLines.length
      }
      diffIdx++
    } else {
      // 处理边界情况
      if (origIdx < totalOriginal) {
        diffString += ' ' + (originalLines[origIdx] || '') + '\n'
        origIdx++
        revIdx++
      } else if (revIdx < totalRevised) {
        diffString += '+' + (revisedLines[revIdx] || '') + '\n'
        revIdx++
      } else {
        break
      }
    }
  }

  return diffString
}

// 导航到上一个差异
function prevDiff() {
  if (currentDiffIndex.value > 0) {
    currentDiffIndex.value--
    scrollToDiff(currentDiffIndex.value)
  }
}

// 导航到下一个差异
function nextDiff() {
  if (currentDiffIndex.value < diffBlocks.value.length - 1) {
    currentDiffIndex.value++
    scrollToDiff(currentDiffIndex.value)
  }
}

// 滚动到指定的差异块并高亮
function scrollToDiff(index) {
  if (index < 0 || index >= diffBlocks.value.length) return

  const container = activeTab.value === 'side-by-side'
    ? sideBySideContainer.value
    : unifiedContainer.value

  if (!container) return

  // 等待下一帧渲染完成
  setTimeout(() => {
    // 移除之前的高亮
    container.querySelectorAll('.current-diff-highlight').forEach(el => {
      el.classList.remove('current-diff-highlight')
    })

    // 查找所有差异行（删除或插入）
    const diffLines = container.querySelectorAll('.d2h-del, .d2h-ins')

    if (diffLines.length === 0) return

    // 计算目标差异块的起始行索引
    let lineIndex = 0
    for (let i = 0; i < index; i++) {
      const block = diffBlocks.value[i]
      lineIndex += block.originalLineCount + block.revisedLineCount
    }

    // 获取目标行
    const targetLine = diffLines[lineIndex]

    if (targetLine) {
      // 添加高亮
      targetLine.classList.add('current-diff-highlight')

      // 平滑滚动到目标位置
      targetLine.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      })
    }
  }, 100)
}

onMounted(() => {
  fetchData()
  activeTab.value = isMobile.value ? 'unified' : 'full-content'
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.version-comparer {
  width: 100%;
  height: 100%;
}

.diff-view-wrapper {
  position: relative;
}

.diff-container {
  max-height: 70vh;
  overflow: auto;
}

.diff-navigator {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
  background: white;
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

/* 完整内容视图样式 */
.full-content-view {
  padding: 20px;
}

.version-title {
  margin-bottom: 15px;
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 4px;
  text-align: left;
  font-weight: bold;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-blocks-container {
  max-height: 70vh;
  overflow: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  background: #fff;
}

.normal-view-container {
  max-width: 900px;
  margin: 0 auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.content-block {
  margin-bottom: 15px;
  padding: 10px;
  border-radius: 4px;
}

.block-text {
  background: #fafafa;
}

.block-table {
  background: #f0f9ff;
  overflow-x: auto;
}

.block-image {
  background: #f5fff5;
  text-align: center;
}

.text-block {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.table-block :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.table-block :deep(td),
.table-block :deep(th) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.table-block :deep(tr:nth-child(even)) {
  background-color: #f9f9f9;
}

.image-block img {
  max-width: 100%;
  height: auto;
  border: 1px solid #eee;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 隐藏 diff2html 的文件头 */
:deep(.d2h-file-header) {
  display: none;
}

/* 交换左右栏：让新版本(Revised)在左，旧版本(Original)在右 */
:deep(.d2h-file-side-diff) {
  display: flex;
  flex-direction: row-reverse;
}

:deep(.d2h-code-side-wrapper) {
  flex: 1;
  width: 50%;
}

/* 当前差异高亮效果 */
:deep(.current-diff-highlight) {
  background-color: rgba(255, 235, 59, 0.3) !important;
  box-shadow: 0 0 0 2px #ffc107 !important;
  transition: all 0.3s ease;
}

/* 增强差异行的可见性 */
:deep(.d2h-del) {
  background-color: #ffe6e6 !important;
}

:deep(.d2h-ins) {
  background-color: #e6ffe6 !important;
}

/* 上下文行样式 */
:deep(.d2h-code-line-ctn) {
  padding: 2px 4px;
}

/* 高亮开关区域 */
.highlight-toggle {
  padding: 10px 15px;
  margin-bottom: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.highlight-legend {
  display: flex;
  gap: 15px;
  font-size: 13px;
}

.legend-item {
  padding: 3px 10px;
  border-radius: 3px;
  font-weight: 500;
}

.legend-deleted {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef9a9a;
}

.legend-added {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #a5d6a7;
}

.legend-changed {
  background: #fff3e0;
  color: #e65100;
  border: 1px solid #ffcc80;
}

/* 差异高亮样式 */
.highlight-deleted {
  background: linear-gradient(90deg, #ffcdd2 0%, #ffebee 100%) !important;
  border-left: 4px solid #f44336 !important;
  animation: highlightPulse 2s ease-in-out;
}

.highlight-added {
  background: linear-gradient(90deg, #c8e6c9 0%, #e8f5e9 100%) !important;
  border-left: 4px solid #4caf50 !important;
  animation: highlightPulse 2s ease-in-out;
}

@keyframes highlightPulse {
  0% { opacity: 0.7; }
  50% { opacity: 1; }
  100% { opacity: 1; }
}

/* 细粒度文本差异高亮样式 */
.diff-deleted {
  background-color: #ffcdd2;
  color: #b71c1c;
  text-decoration: line-through;
  padding: 1px 2px;
  border-radius: 2px;
}

.diff-added {
  background-color: #c8e6c9;
  color: #1b5e20;
  padding: 1px 2px;
  border-radius: 2px;
  font-weight: 500;
}

/* 当前差异块高亮 */
.current-diff-block {
  outline: 3px solid #ffc107 !important;
  outline-offset: 2px;
  background-color: rgba(255, 235, 59, 0.15) !important;
  position: relative;
}

.current-diff-block::before {
  content: '▶';
  position: absolute;
  left: -20px;
  top: 50%;
  transform: translateY(-50%);
  color: #ffc107;
  font-size: 14px;
}

/* 差异导航栏样式 */
.diff-nav-bar {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-left: 20px;
  padding-left: 20px;
  border-left: 1px solid #ddd;
}

.diff-counter {
  font-size: 13px;
  color: #666;
  font-weight: 500;
  min-width: 60px;
}
</style>
