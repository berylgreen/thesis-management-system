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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            log.info("文件扫描和初始化完成!");
        } catch (Exception e) {
            log.error("文件扫描初始化失败", e);
        }
    }

    @Transactional
    public void scanAndInitialize() throws IOException {
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists() || !uploadDir.isDirectory()) {
            log.warn("uploads 目录不存在: {}", uploadPath);
            return;
        }

        File[] studentDirs = uploadDir.listFiles(File::isDirectory);
        if (studentDirs == null || studentDirs.length == 0) {
            log.info("uploads 目录为空");
            return;
        }

        int processedFiles = 0;
        int skippedFiles = 0;

        for (File studentDir : studentDirs) {
            String studentUsername = studentDir.getName();
            log.info("处理学生文件夹: {}", studentUsername);

            // 查找或创建学生用户
            User student = findOrCreateStudent(studentUsername);
            if (student == null) {
                log.warn("无法为学号 {} 创建用户，跳过", studentUsername);
                continue;
            }

            // 扫描该学生的所有论文文件
            File[] files = studentDir.listFiles(file ->
                file.isFile() && (file.getName().endsWith(".docx") ||
                                  file.getName().endsWith(".doc") ||
                                  file.getName().endsWith(".pdf")));

            if (files == null || files.length == 0) {
                log.info("学生 {} 没有论文文件", studentUsername);
                continue;
            }

            for (File file : files) {
                try {
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
        }

        log.info("扫描完成! 处理: {} 个文件, 跳过: {} 个文件", processedFiles, skippedFiles);
    }

    private User findOrCreateStudent(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.info("用户 {} 不存在，创建新用户", username);
            user = new User();
            user.setUsername(username);
            user.setPasswordHash("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"); // 默认密码 admin123
            user.setRole("STUDENT");
            user.setRealName(username);
            user.setEmail(username + "@student.edu.cn");
            userMapper.insert(user);
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
                if (latestVersion != null && !latestVersion.getVersionNum().equals(thesis.getCurrentVersion())) {
                    thesis.setCurrentVersion(latestVersion.getVersionNum());
                    thesisMapper.updateById(thesis);
                }
            }
        }
        
        log.info("清理完成: 删除 {} 个版本记录, {} 篇空论文", deletedVersions, deletedTheses);
        
        // 3. 调用现有扫描逻辑添加新文件
        scanAndInitialize();
        
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        result.put("deletedVersions", deletedVersions);
        result.put("deletedTheses", deletedTheses);
        
        log.info("强制同步完成!");
        return result;
    }
}
