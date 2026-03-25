package com.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thesis.entity.Thesis;
import com.thesis.entity.ThesisVersion;
import com.thesis.entity.User;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FileInitService implements CommandLineRunner {

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private ThesisVersionMapper thesisVersionMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    // 文件名格式: 姓名学号_论文标题_日期.docx
    private static final Pattern FILE_PATTERN = Pattern.compile("^(.+?)(\\d{12})_(.+?)_(\\d{8})(?:_(\\d+))?\\.(docx?|pdf)$");

    @Override
    public void run(String... args) {
        log.info("开始扫描 uploads 目录并初始化数据库...");
        try {
            scanAndInitialize();
            initThesisTitles();
            log.info("文件扫描和初始化完成!");
        } catch (Exception e) {
            log.error("文件扫描初始化失败", e);
        }
    }

    /**
     * 为 thesis_title 为空的学生，从最新文件名中提取论文题目
     */
    private void initThesisTitles() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "STUDENT")
               .isNull(User::getThesisTitle);
        List<User> students = userMapper.selectList(wrapper);

        if (students.isEmpty()) {
            log.info("没有需要初始化论文题目的学生");
            return;
        }

        File uploadDir = new File(uploadPath);
        int updated = 0;

        for (User student : students) {
            String title = extractLatestTitle(uploadDir, student.getUsername());
            if (title != null && !title.isEmpty()) {
                student.setThesisTitle(title);
                userMapper.updateById(student);
                updated++;
                log.info("初始化论文题目: {} -> {}", student.getUsername(), title);
            }
        }

        log.info("论文题目初始化完成，更新 {} 个学生", updated);
    }

    /**
     * 从 uploads 根目录下按学号筛选文件，从最新文件名中提取论文题目
     */
    private String extractLatestTitle(File uploadDir, String username) {
        if (!uploadDir.exists() || !uploadDir.isDirectory()) {
            return null;
        }

        // 直接在 uploads 根目录按文件名包含学号筛选
        File[] files = uploadDir.listFiles(file ->
            file.isFile() && file.getName().contains(username) &&
            (file.getName().endsWith(".docx") ||
             file.getName().endsWith(".doc") ||
             file.getName().endsWith(".pdf")));

        if (files == null || files.length == 0) {
            return null;
        }

        // 按修改时间倒序，取最新文件
        File latestFile = files[0];
        for (File f : files) {
            if (f.lastModified() > latestFile.lastModified()) {
                latestFile = f;
            }
        }

        Matcher matcher = FILE_PATTERN.matcher(latestFile.getName());
        if (matcher.matches()) {
            String title = matcher.group(3);
            // 排除「未命名论文」之类的占位标题
            if (!"未命名论文".equals(title)) {
                return title;
            }
        }
        return null;
    }

    @Transactional
    public void scanAndInitialize() throws IOException {
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists() || !uploadDir.isDirectory()) {
            log.warn("uploads 目录不存在: {}", uploadPath);
            return;
        }

        // 直接扫描 uploads 根目录下的论文文件（扁平结构，无学号子目录）
        File[] files = uploadDir.listFiles(file ->
            file.isFile() && (file.getName().endsWith(".docx") ||
                              file.getName().endsWith(".doc") ||
                              file.getName().endsWith(".pdf")));

        if (files == null || files.length == 0) {
            log.info("uploads 目录下没有论文文件");
            return;
        }

        int processedFiles = 0;
        int skippedFiles = 0;

        for (File file : files) {
            try {
                // 从文件名正则提取学号和姓名
                Matcher matcher = FILE_PATTERN.matcher(file.getName());
                if (!matcher.matches()) {
                    log.warn("文件名格式不匹配，无法提取学号: {}", file.getName());
                    skippedFiles++;
                    continue;
                }

                String realName = matcher.group(1);        // 姓名
                String studentUsername = matcher.group(2);  // 学号
                User student = findOrCreateStudent(studentUsername, realName);
                if (student == null) {
                    log.warn("无法为学号 {} 创建用户，跳过文件: {}", studentUsername, file.getName());
                    skippedFiles++;
                    continue;
                }

                if (processFile(student, file)) {
                    processedFiles++;
                } else {
                    skippedFiles++;
                }
            } catch (Exception e) {
                log.error("处理文件失败: {}", file.getName(), e);
                skippedFiles++;
            }
        }

        log.info("扫描完成! 处理: {} 个文件, 跳过: {} 个文件", processedFiles, skippedFiles);
    }

    private User findOrCreateStudent(String username, String realName) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.info("用户 {} 不存在，创建新用户（姓名: {}）", username, realName);
            user = new User();
            user.setUsername(username);
            user.setPasswordHash("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"); // 默认密码 admin123
            user.setRole("STUDENT");
            user.setRealName(realName != null && !realName.isEmpty() ? realName : username);
            user.setEmail(username + "@student.edu.cn");
            userMapper.insert(user);
        } else if (realName != null && !realName.isEmpty() && username.equals(user.getRealName())) {
            // 已有用户的 realName 仍是学号，用文件名中的姓名修正
            log.info("修正用户 {} 的姓名: {} -> {}", username, user.getRealName(), realName);
            user.setRealName(realName);
            userMapper.updateById(user);
        }

        return user;
    }

    private boolean processFile(User student, File file) throws IOException {
        String fileName = file.getName();
        Matcher matcher = FILE_PATTERN.matcher(fileName);

        String title;
        String dateStr = null;

        if (matcher.matches()) {
            // 格式: 姓名学号_论文标题_日期[_后缀].docx
            String realName = matcher.group(1);
            String studentNo = matcher.group(2);
            title = matcher.group(3);
            dateStr = matcher.group(4);
            // group(5) 是后缀数字，我们暂时不需要存，但正则支持了它使得匹配成功

            log.debug("解析文件: 姓名={}, 学号={}, 标题={}, 日期={}", realName, studentNo, title, dateStr);
        } else {
            // 如果文件名不匹配规则，使用文件名作为标题
            title = fileName.substring(0, fileName.lastIndexOf('.'));
            log.warn("文件名格式不匹配: {}, 使用文件名作为标题", fileName);
        }

        // 计算文件哈希值
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String contentHash = calculateHash(fileBytes);

        // 检查文件路径是否已存在（避免重复导入相同路径的文件）
        LambdaQueryWrapper<ThesisVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(ThesisVersion::getFilePath, file.getAbsolutePath());
        if (thesisVersionMapper.exists(versionWrapper)) {
            log.debug("文件路径已存在，跳过: {}", fileName);
            return false;
        }

        // 查找或创建论文记录
        LambdaQueryWrapper<Thesis> thesisWrapper = new LambdaQueryWrapper<>();
        thesisWrapper.eq(Thesis::getStudentId, student.getId())
                     .eq(Thesis::getTitle, title);
        Thesis thesis = thesisMapper.selectOne(thesisWrapper);

        if (thesis == null) {
            // 创建新论文
            thesis = new Thesis();
            thesis.setStudentId(student.getId());
            thesis.setTitle(title);
            thesis.setStatus("SUBMITTED");
            thesis.setCurrentVersion(0);
            thesisMapper.insert(thesis);
            log.info("创建新论文: {} (学生: {})", title, student.getUsername());
        }

        // 创建新版本
        int newVersionNum = thesis.getCurrentVersion() + 1;
        ThesisVersion version = new ThesisVersion();
        version.setThesisId(thesis.getId());
        version.setVersionNum(newVersionNum);
        version.setFilePath(file.getAbsolutePath());
        version.setContentHash(contentHash);
        version.setFileSize(file.length());
        version.setRemark("系统自动导入" + (dateStr != null ? " (文件日期: " + dateStr + ")" : ""));
        thesisVersionMapper.insert(version);

        // 更新论文版本号
        thesis.setCurrentVersion(newVersionNum);
        thesis.setStatus("SUBMITTED");
        thesisMapper.updateById(thesis);

        log.info("导入文件成功: {} -> 论文ID: {}, 版本: {}", fileName, thesis.getId(), newVersionNum);
        return true;
    }

    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算哈希失败", e);
        }
    }

    /**
     * 强制同步：以文件系统为准更新数据库
     * 1. 删除数据库中文件已不存在的版本记录
     * 2. 添加新发现的文件
     * @return 同步统计结果
     */
    @Transactional
    public java.util.Map<String, Integer> forceSyncFromFileSystem() throws IOException {
        log.info("开始强制同步文件系统到数据库...");

        // 0. 物理删除软删除的版本记录（清除唯一约束中的僵尸数据）
        int purged = thesisVersionMapper.physicalDeleteSoftDeleted();
        if (purged > 0) {
            log.info("物理清除 {} 条软删除版本记录", purged);
        }
        
        int deletedVersions = 0;
        int deletedTheses = 0;
        
        // 1. 获取数据库中所有版本记录
        List<ThesisVersion> allVersions = thesisVersionMapper.selectList(null);
        
        for (ThesisVersion version : allVersions) {
            String filePath = version.getFilePath();
            if (filePath == null || filePath.isEmpty()) continue;
            
            File file = new File(filePath);
            if (!file.exists()) {
                log.info("文件不存在，删除版本记录: {} (ID: {})", filePath, version.getId());
                thesisVersionMapper.deleteById(version.getId());
                deletedVersions++;
            }
        }
        
        // 2. 检查没有任何版本的论文，删除它们
        List<Thesis> allTheses = thesisMapper.selectList(null);
        for (Thesis thesis : allTheses) {
            LambdaQueryWrapper<ThesisVersion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ThesisVersion::getThesisId, thesis.getId());
            Long versionCount = thesisVersionMapper.selectCount(wrapper);
            
            if (versionCount == 0) {
                log.info("论文无版本记录，删除: {} (ID: {})", thesis.getTitle(), thesis.getId());
                thesisMapper.deleteById(thesis.getId());
                deletedTheses++;
            } else {
                // 更新当前版本号为实际最大版本
                wrapper.orderByDesc(ThesisVersion::getVersionNum);
                ThesisVersion latestVersion = thesisVersionMapper.selectOne(wrapper.last("LIMIT 1"));
                boolean needUpdate = false;

                if (latestVersion != null && !latestVersion.getVersionNum().equals(thesis.getCurrentVersion())) {
                    thesis.setCurrentVersion(latestVersion.getVersionNum());
                    needUpdate = true;
                }

                // 从最新版本文件名提取标题，同步更新论文标题
                if (latestVersion != null) {
                    String newTitle = extractTitleFromFilePath(latestVersion.getFilePath());
                    if (newTitle != null && !newTitle.equals(thesis.getTitle())) {
                        log.info("更新论文标题: {} -> {} (ID: {})", thesis.getTitle(), newTitle, thesis.getId());
                        thesis.setTitle(newTitle);
                        needUpdate = true;
                    }
                }

                if (needUpdate) {
                    thesisMapper.updateById(thesis);
                }
            }
        }
        
        log.info("清理完成: 删除 {} 个版本记录, {} 篇空论文", deletedVersions, deletedTheses);

        // 2.5 合并同学生同标题的重复论文
        int mergedTheses = mergeDuplicateTheses();

        // 3. 调用现有扫描逻辑添加新文件
        scanAndInitialize();
        
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        result.put("deletedVersions", deletedVersions);
        result.put("deletedTheses", deletedTheses);
        result.put("mergedTheses", mergedTheses);
        
        log.info("强制同步完成!");
        return result;
    }

    /**
     * 合并同一学生、同一标题的重复论文记录
     * 保留每组中最早创建的记录，将其他记录的版本迁移过来并重排版本号
     * @return 被合并删除的论文数量
     */
    private int mergeDuplicateTheses() {
        List<Thesis> allTheses = thesisMapper.selectList(null);
        if (allTheses == null || allTheses.isEmpty()) {
            return 0;
        }

        // 按 (studentId, title) 分组
        java.util.Map<String, java.util.List<Thesis>> groups = new java.util.LinkedHashMap<>();
        for (Thesis t : allTheses) {
            String key = t.getStudentId() + "::" + t.getTitle();
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(t);
        }

        int merged = 0;
        for (java.util.Map.Entry<String, java.util.List<Thesis>> entry : groups.entrySet()) {
            java.util.List<Thesis> group = entry.getValue();
            if (group.size() <= 1) {
                continue;
            }

            // 按 ID 排序，保留最小 ID（最早创建）的记录
            group.sort((a, b) -> Long.compare(a.getId(), b.getId()));
            Thesis primary = group.get(0);

            log.info("合并重复论文: 标题='{}', 保留ID={}, 合并 {} 条",
                    primary.getTitle(), primary.getId(), group.size() - 1);

            // 将其他论文的版本迁移到 primary（使用临时高版本号避免唯一约束冲突）
            int tempVersionBase = 10000;
            for (int i = 1; i < group.size(); i++) {
                Thesis duplicate = group.get(i);
                LambdaQueryWrapper<ThesisVersion> vw = new LambdaQueryWrapper<>();
                vw.eq(ThesisVersion::getThesisId, duplicate.getId());
                List<ThesisVersion> versions = thesisVersionMapper.selectList(vw);

                for (ThesisVersion v : versions) {
                    v.setThesisId(primary.getId());
                    v.setVersionNum(tempVersionBase++); // 临时版本号，renumberVersions 会重排
                    thesisVersionMapper.updateById(v);
                }

                // 删除重复的论文记录
                thesisMapper.deleteById(duplicate.getId());
                merged++;
            }

            // 重排 primary 的版本号（按文件创建时间排序）
            renumberVersions(primary);
        }

        if (merged > 0) {
            log.info("论文合并完成: 合并删除 {} 篇重复论文", merged);
        }
        return merged;
    }

    /**
     * 按创建时间重排论文的版本号
     */
    private void renumberVersions(Thesis thesis) {
        LambdaQueryWrapper<ThesisVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ThesisVersion::getThesisId, thesis.getId())
               .orderByAsc(ThesisVersion::getCreatedAt);
        List<ThesisVersion> versions = thesisVersionMapper.selectList(wrapper);

        // 第一遍：全部设置为临时高版本号，清除唯一约束冲突空间
        for (int i = 0; i < versions.size(); i++) {
            ThesisVersion v = versions.get(i);
            v.setVersionNum(20000 + i);
            thesisVersionMapper.updateById(v);
        }

        // 第二遍：从 1 开始重排
        for (int i = 0; i < versions.size(); i++) {
            ThesisVersion v = versions.get(i);
            v.setVersionNum(i + 1);
            thesisVersionMapper.updateById(v);
        }

        // 更新论文的当前版本号
        thesis.setCurrentVersion(versions.size());
        thesisMapper.updateById(thesis);
    }

    /**
     * 从文件路径中提取论文标题（复用 FILE_PATTERN 正则）
     * @param filePath 文件绝对路径
     * @return 提取到的标题，无法提取时返回 null
     */
    private String extractTitleFromFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        String fileName = new File(filePath).getName();
        Matcher matcher = FILE_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            String title = matcher.group(3);
            if (!"未命名论文".equals(title)) {
                return title;
            }
        }
        return null;
    }
}
