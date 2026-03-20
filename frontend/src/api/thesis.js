import request from '../utils/request'

export function getMyTheses() {
  return request({
    url: '/thesis/my',
    method: 'get'
  })
}

export function getThesis(id) {
  return request({
    url: `/thesis/${id}`,
    method: 'get'
  })
}

export function createThesis(title) {
  return request({
    url: '/thesis/create',
    method: 'post',
    params: { title }
  })
}

export function uploadVersion(thesisId, formData) {
  return request({
    url: `/thesis/${thesisId}/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getVersions(thesisId) {
  return request({
    url: `/thesis/${thesisId}/versions`,
    method: 'get'
  })
}

export function downloadVersion(versionId) {
  return request({
    url: `/thesis/version/${versionId}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

export function getDiff(version1Id, version2Id) {
  return request({
    url: '/diff/compare',
    method: 'get',
    params: { version1Id, version2Id }
  })
}

// 获取单个版本的文档内容（用于单版本查看模式）
export function getVersionContent(versionId) {
  return request({
    url: '/diff/content',
    method: 'get',
    params: { versionId }
  })
}

export function forceSync() {
  return request({
    url: '/thesis/admin/force-sync',
    method: 'post'
  })
}

// 分析论文（自动取最新版本，执行摘要/目录/参考文献/引用检测）
export function analyzeThesis(thesisId) {
  return request({
    url: `/thesis/${thesisId}/analyze`,
    method: 'get',
    timeout: 120000 // 文献验证耗时较长，2分钟超时
  })
}
