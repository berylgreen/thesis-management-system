import request from '../utils/request'

export function getMyTheses() {
  return request({
    url: '/thesis/my',
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

export function forceSync() {
  return request({
    url: '/thesis/admin/force-sync',
    method: 'post'
  })
}
