package com.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.thesis.dto.StudentDTO;
import com.thesis.entity.User;
import com.thesis.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 学生管理业务逻辑
 */
@Service
public class StudentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 分页查询学生列表，支持按学号/姓名模糊搜索
     */
    public IPage<User> listStudents(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "STUDENT");

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(User::getUsername, keyword)
                .or()
                .like(User::getRealName, keyword)
            );
        }

        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 获取单个学生信息
     */
    public User getStudent(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || !"STUDENT".equals(user.getRole())) {
            throw new RuntimeException("学生不存在");
        }
        return user;
    }

    /**
     * 新增学生
     */
    public User createStudent(StudentDTO.CreateRequest request) {
        // 校验用户名唯一性
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("学号已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());

        userMapper.insert(user);
        return user;
    }

    /**
     * 编辑学生信息
     */
    public User updateStudent(Long id, StudentDTO.UpdateRequest request) {
        User user = getStudent(id);
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setThesisTitle(request.getThesisTitle());
        userMapper.updateById(user);
        return user;
    }

    /**
     * 删除学生（逻辑删除）
     */
    public void deleteStudent(Long id) {
        User user = getStudent(id);
        userMapper.deleteById(user.getId());
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        User user = getStudent(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Autowired
    private com.thesis.mapper.ThesisMapper thesisMapper;

    @Autowired
    private com.thesis.mapper.ThesisVersionMapper thesisVersionMapper;

    /**
     * 获取学生的所有论文及其版本列表
     */
    public java.util.List<java.util.Map<String, Object>> getStudentThesesWithVersions(Long studentId) {
        User student = getStudent(studentId);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.Thesis> tw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        tw.eq(com.thesis.entity.Thesis::getStudentId, studentId)
          .orderByDesc(com.thesis.entity.Thesis::getUpdatedAt);
        java.util.List<com.thesis.entity.Thesis> theses = thesisMapper.selectList(tw);

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.thesis.entity.Thesis thesis : theses) {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", thesis.getId());
            item.put("title", thesis.getTitle());
            item.put("status", thesis.getStatus());
            item.put("currentVersion", thesis.getCurrentVersion());
            item.put("createdAt", thesis.getCreatedAt());

            // 查询该论文所有版本
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.ThesisVersion> vw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            vw.eq(com.thesis.entity.ThesisVersion::getThesisId, thesis.getId())
              .orderByDesc(com.thesis.entity.ThesisVersion::getVersionNum);
            java.util.List<com.thesis.entity.ThesisVersion> versions = thesisVersionMapper.selectList(vw);

            // 构建版本信息（包含文件名）
            java.util.List<java.util.Map<String, Object>> versionList = new java.util.ArrayList<>();
            for (com.thesis.entity.ThesisVersion v : versions) {
                java.util.Map<String, Object> vi = new java.util.LinkedHashMap<>();
                vi.put("id", v.getId());
                vi.put("versionNum", v.getVersionNum());
                vi.put("fileName", extractFileName(v.getFilePath()));
                vi.put("filePath", v.getFilePath());
                vi.put("fileSize", v.getFileSize());
                vi.put("createdAt", v.getCreatedAt());
                versionList.add(vi);
            }
            item.put("versions", versionList);
            result.add(item);
        }
        return result;
    }

    /**
     * 批量重命名论文版本文件为规范格式: 学号_姓名_论文题目_日期.ext
     */
    public java.util.Map<String, Object> batchRenameFiles(StudentDTO.BatchRenameRequest request) {
        User student = getStudent(request.getStudentId());
        String thesisTitle = student.getThesisTitle();
        if (thesisTitle == null || thesisTitle.isEmpty()) {
            throw new RuntimeException("该学生未设置论文题目，请先设置");
        }

        int success = 0;
        int failed = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        // 文件名提取日期的正则: 姓名学号_题目_日期[_序号].ext
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("_(\\d{8})(?:_(\\d+))?\\.[^.]+$");

        for (Long versionId : request.getVersionIds()) {
            try {
                com.thesis.entity.ThesisVersion version = thesisVersionMapper.selectById(versionId);
                if (version == null) {
                    errors.add("版本ID " + versionId + " 不存在");
                    failed++;
                    continue;
                }

                java.io.File oldFile = new java.io.File(version.getFilePath());
                if (!oldFile.exists()) {
                    errors.add("文件不存在: " + oldFile.getName());
                    failed++;
                    continue;
                }

                // 提取日期
                String oldName = oldFile.getName();
                String ext = oldName.substring(oldName.lastIndexOf('.'));
                java.util.regex.Matcher dateMatcher = datePattern.matcher(oldName);
                String dateStr;
                String suffix = "";
                if (dateMatcher.find()) {
                    dateStr = dateMatcher.group(1);
                    if (dateMatcher.group(2) != null) {
                        suffix = "_" + dateMatcher.group(2);
                    }
                } else {
                    // 无法从文件名提取日期，使用文件修改时间
                    java.time.LocalDateTime modTime = java.time.Instant.ofEpochMilli(oldFile.lastModified())
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    dateStr = modTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                }

                // 构建新文件名: 姓名学号_论文题目_日期[_序号].ext
                String newName = student.getRealName() + student.getUsername() + "_" + thesisTitle + "_" + dateStr + suffix + ext;
                java.io.File newFile = new java.io.File(oldFile.getParent(), newName);

                // 如果新旧文件名相同，跳过
                if (oldFile.getName().equals(newName)) {
                    success++;
                    continue;
                }

                // 如果目标文件已存在，追加 _1, _2, _3... 后缀
                if (newFile.exists()) {
                    String baseName = newName.substring(0, newName.lastIndexOf('.'));
                    String extName = newName.substring(newName.lastIndexOf('.'));
                    int counter = 1;
                    while (newFile.exists()) {
                        newName = baseName + "_" + counter + extName;
                        newFile = new java.io.File(oldFile.getParent(), newName);
                        counter++;
                    }
                }

                // 执行重命名
                if (oldFile.renameTo(newFile)) {
                    // 更新数据库中的文件路径
                    version.setFilePath(newFile.getAbsolutePath());
                    thesisVersionMapper.updateById(version);
                    success++;
                } else {
                    errors.add("重命名失败: " + oldName);
                    failed++;
                }
            } catch (Exception e) {
                errors.add("版本ID " + versionId + " 处理异常: " + e.getMessage());
                failed++;
            }
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        return new java.io.File(filePath).getName();
    }
}
