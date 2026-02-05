-- 创建数据库
CREATE DATABASE IF NOT EXISTS thesis_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE thesis_system;

-- 用户表
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    role VARCHAR(20) NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 论文表
CREATE TABLE t_thesis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '论文ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    title VARCHAR(200) NOT NULL COMMENT '论文标题',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SUBMITTED/REVIEWED/APPROVED',
    current_version INT DEFAULT 1 COMMENT '当前版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_student_id (student_id),
    INDEX idx_status (status),
    FOREIGN KEY (student_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论文表';

-- 论文版本表
CREATE TABLE t_thesis_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    thesis_id BIGINT NOT NULL COMMENT '论文ID',
    version_num INT NOT NULL COMMENT '版本号',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    content_hash VARCHAR(64) COMMENT '内容哈希（SHA-256）',
    file_size BIGINT COMMENT '文件大小（字节）',
    remark VARCHAR(500) COMMENT '版本说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_thesis_version (thesis_id, version_num),
    INDEX idx_thesis_id (thesis_id),
    FOREIGN KEY (thesis_id) REFERENCES t_thesis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论文版本表';

-- 批改表
CREATE TABLE t_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '批改ID',
    version_id BIGINT NOT NULL COMMENT '版本ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    comment TEXT COMMENT '批改意见',
    score DECIMAL(5,2) COMMENT '评分',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_version_id (version_id),
    INDEX idx_teacher_id (teacher_id),
    FOREIGN KEY (version_id) REFERENCES t_thesis_version(id),
    FOREIGN KEY (teacher_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批改表';

-- 插入初始管理员账号（密码：admin123，需要在应用中使用 BCrypt 加密）
INSERT INTO t_user (username, password_hash, role, real_name, email) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', '系统管理员', 'admin@thesis.com');

-- 插入测试数据（可选）
INSERT INTO t_user (username, password_hash, role, real_name, email) VALUES
('student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'STUDENT', '张三', 'student1@thesis.com'),
('tt', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'TEACHER', '李老师', 'teacher1@thesis.com');
