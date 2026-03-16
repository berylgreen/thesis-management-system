import request from '../utils/request'

/**
 * 分页查询学生列表
 * @param {Object} params - { page, size, keyword }
 */
export function getStudents(params) {
  return request({
    url: '/students',
    method: 'get',
    params
  })
}

/**
 * 获取单个学生
 */
export function getStudent(id) {
  return request({
    url: `/students/${id}`,
    method: 'get'
  })
}

/**
 * 新增学生
 */
export function createStudent(data) {
  return request({
    url: '/students',
    method: 'post',
    data
  })
}

/**
 * 编辑学生信息
 */
export function updateStudent(id, data) {
  return request({
    url: `/students/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除学生
 */
export function deleteStudent(id) {
  return request({
    url: `/students/${id}`,
    method: 'delete'
  })
}

/**
 * 重置密码
 */
export function resetPassword(id, data) {
  return request({
    url: `/students/${id}/reset-password`,
    method: 'put',
    data
  })
}

/**
 * 获取学生的所有论文及版本
 */
export function getStudentTheses(id) {
  return request({
    url: `/students/${id}/theses`,
    method: 'get'
  })
}

/**
 * 批量重命名论文版本文件
 */
export function batchRenameFiles(data) {
  return request({
    url: '/students/batch-rename',
    method: 'post',
    data
  })
}
