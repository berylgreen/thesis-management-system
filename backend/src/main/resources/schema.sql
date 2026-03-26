-- H2 兼容版 schema（MODE=MySQL）
-- 原始 MySQL DDL 移除了 ENGINE=InnoDB、CHARSET、COLLATE、COMMENT 等 MySQL 专属语法

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    thesis_title VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_user_username ON t_user(username);
CREATE INDEX IF NOT EXISTS idx_user_role ON t_user(role);

-- 论文表
CREATE TABLE IF NOT EXISTS t_thesis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES t_user(id)
);
CREATE INDEX IF NOT EXISTS idx_thesis_student_id ON t_thesis(student_id);

-- 论文版本表
CREATE TABLE IF NOT EXISTS t_thesis_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thesis_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_hash VARCHAR(64),
    file_size BIGINT,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (thesis_id) REFERENCES t_thesis(id)
);
CREATE INDEX IF NOT EXISTS idx_version_thesis_id ON t_thesis_version(thesis_id);

-- 批改表
CREATE TABLE IF NOT EXISTS t_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    comment TEXT,
    score DECIMAL(5,2),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (version_id) REFERENCES t_thesis_version(id),
    FOREIGN KEY (teacher_id) REFERENCES t_user(id)
);
CREATE INDEX IF NOT EXISTS idx_review_version_id ON t_review(version_id);
CREATE INDEX IF NOT EXISTS idx_review_teacher_id ON t_review(teacher_id);
