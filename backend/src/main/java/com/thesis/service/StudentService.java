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

    @Autowired
    private FileInitService fileInitService;

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
     * 若论文题目为空，则尝试从该学生最新的 Word 文件名中提取
     */
    public User updateStudent(Long id, StudentDTO.UpdateRequest request) {
        User user = getStudent(id);
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());

        String title = request.getThesisTitle();
        if (title == null || title.trim().isEmpty()) {
            // 从最新版本文件名提取论文题目
            title = extractTitleFromLatestVersion(id);
        }
        user.setThesisTitle(title);

        userMapper.updateById(user);
        return user;
    }

    /**
     * 从学生最新版本的文件名中提取论文题目
     * 文件名格式: 姓名学号_论文题目_日期.ext
     */
    private String extractTitleFromLatestVersion(Long studentId) {
        // 文件名格式: 姓名学号_论文题目_日期[_序号].ext
        java.util.regex.Pattern filePattern = java.util.regex.Pattern.compile(
            "^.+?\\d{12}_(.+?)_\\d{8}(?:_\\d+)?\\.(docx?|pdf)$");

        // 查该学生所有论文
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.Thesis> tw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        tw.eq(com.thesis.entity.Thesis::getStudentId, studentId);
        java.util.List<com.thesis.entity.Thesis> theses = thesisMapper.selectList(tw);

        // 遍历所有论文，取最新版本
        com.thesis.entity.ThesisVersion latestVersion = null;
        for (com.thesis.entity.Thesis thesis : theses) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.ThesisVersion> vw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            vw.eq(com.thesis.entity.ThesisVersion::getThesisId, thesis.getId())
              .orderByDesc(com.thesis.entity.ThesisVersion::getCreatedAt)
              .last("LIMIT 1");
            com.thesis.entity.ThesisVersion v = thesisVersionMapper.selectOne(vw);
            if (v != null && (latestVersion == null || v.getCreatedAt().isAfter(latestVersion.getCreatedAt()))) {
                latestVersion = v;
            }
        }

        if (latestVersion == null || latestVersion.getFilePath() == null) {
            return null;
        }

        String fileName = new java.io.File(latestVersion.getFilePath()).getName();
        java.util.regex.Matcher matcher = filePattern.matcher(fileName);
        if (matcher.matches()) {
            String title = matcher.group(1);
            if (!"未命名论文".equals(title)) {
                return title;
            }
        }
        return null;
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
          .orderByAsc(com.thesis.entity.Thesis::getCreatedAt);
        java.util.List<com.thesis.entity.Thesis> theses = thesisMapper.selectList(tw);

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        int versionCounter = 0;
        for (com.thesis.entity.Thesis thesis : theses) {
            versionCounter++;
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", thesis.getId());
            item.put("title", thesis.getTitle());
            item.put("currentVersion", versionCounter);
            item.put("createdAt", thesis.getCreatedAt());

            // 查询该论文所有版本
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.ThesisVersion> vw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            vw.eq(com.thesis.entity.ThesisVersion::getThesisId, thesis.getId())
              .orderByDesc(com.thesis.entity.ThesisVersion::getCreatedAt);
            java.util.List<com.thesis.entity.ThesisVersion> versions = thesisVersionMapper.selectList(vw);

            // 构建版本信息（包含文件名）
            java.util.List<java.util.Map<String, Object>> versionList = new java.util.ArrayList<>();
            for (com.thesis.entity.ThesisVersion v : versions) {
                java.util.Map<String, Object> vi = new java.util.LinkedHashMap<>();
                vi.put("id", v.getId());
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
     * 一键统一所有学生论文题目：遍历所有已设置 thesisTitle 的学生，
     * 1. 将 User.thesisTitle 同步到 Thesis.title
     * 2. 将所有论文版本的物理文件重命名为: 姓名学号_论文题目_日期.ext
     * @return 统计结果
     */
    public java.util.Map<String, Object> syncAllThesisTitles() {
        // 查询所有已设置论文题目的学生
        LambdaQueryWrapper<User> sw = new LambdaQueryWrapper<>();
        sw.eq(User::getRole, "STUDENT")
          .isNotNull(User::getThesisTitle)
          .ne(User::getThesisTitle, "");
        java.util.List<User> students = userMapper.selectList(sw);

        int processedStudents = 0;
        int renamedFiles = 0;
        int skippedStudents = 0;
        int failedFiles = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        // 文件名提取日期的正则: 姓名学号_题目_日期[_序号].ext
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("_(\\d{8})(?:_(\\d+))?\\.[^.]+$");

        for (User student : students) {
            String thesisTitle = student.getThesisTitle();

            // 1. 查询该学生所有论文，同步 Thesis.title
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.Thesis> tw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            tw.eq(com.thesis.entity.Thesis::getStudentId, student.getId());
            java.util.List<com.thesis.entity.Thesis> theses = thesisMapper.selectList(tw);

            if (theses.isEmpty()) {
                skippedStudents++;
                continue;
            }

            boolean hasWork = false;

            for (com.thesis.entity.Thesis thesis : theses) {
                // 同步数据库中的论文标题
                if (!thesisTitle.equals(thesis.getTitle())) {
                    thesis.setTitle(thesisTitle);
                    thesisMapper.updateById(thesis);
                }

                // 2. 查询该论文所有版本，重命名物理文件
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.thesis.entity.ThesisVersion> vw =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                vw.eq(com.thesis.entity.ThesisVersion::getThesisId, thesis.getId());
                java.util.List<com.thesis.entity.ThesisVersion> versions = thesisVersionMapper.selectList(vw);

                for (com.thesis.entity.ThesisVersion version : versions) {
                    try {
                        java.io.File oldFile = new java.io.File(version.getFilePath());
                        if (!oldFile.exists()) continue;

                        String oldName = oldFile.getName();
                        String ext = oldName.substring(oldName.lastIndexOf('.'));

                        // 提取日期
                        java.util.regex.Matcher dateMatcher = datePattern.matcher(oldName);
                        String dateStr;
                        String suffix = "";
                        if (dateMatcher.find()) {
                            dateStr = dateMatcher.group(1);
                            if (dateMatcher.group(2) != null) {
                                suffix = "_" + dateMatcher.group(2);
                            }
                        } else {
                            java.time.LocalDateTime modTime = java.time.Instant.ofEpochMilli(oldFile.lastModified())
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                            dateStr = modTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                        }

                        // 构建新文件名
                        String newName = student.getRealName() + student.getUsername() + "_" + thesisTitle + "_" + dateStr + suffix + ext;
                        java.io.File newFile = new java.io.File(oldFile.getParent(), newName);

                        // 文件名相同则跳过
                        if (oldFile.getName().equals(newName)) continue;

                        // 目标文件已存在时追加序号
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

                        if (oldFile.renameTo(newFile)) {
                            version.setFilePath(newFile.getAbsolutePath());
                            thesisVersionMapper.updateById(version);
                            renamedFiles++;
                            hasWork = true;
                        } else {
                            errors.add(student.getRealName() + ": 重命名失败 " + oldName);
                            failedFiles++;
                        }
                    } catch (Exception e) {
                        errors.add(student.getRealName() + ": " + e.getMessage());
                        failedFiles++;
                    }
                }
            }

            if (hasWork) {
                processedStudents++;
            } else {
                skippedStudents++;
            }
        }

        // 3. 文件重命名后执行强制同步，确保数据库与文件系统完全一致
        //    （合并重复论文记录、清理旧路径、从新文件名提取标题）
        try {
            fileInitService.forceSyncFromFileSystem();
        } catch (Exception e) {
            errors.add("强制同步异常: " + e.getMessage());
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("students", processedStudents);
        result.put("renamed", renamedFiles);
        result.put("failed", failedFiles);
        result.put("skipped", skippedStudents);
        result.put("errors", errors);
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
