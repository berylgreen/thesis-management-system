-- 初始数据（与 init.sql 一致）

-- 管理员账号（密码：admin123，BCrypt 加密）
MERGE INTO t_user (id, username, password_hash, role, real_name, email) KEY(username) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', '系统管理员', 'admin@thesis.com');

-- 测试学生（密码：admin123）
MERGE INTO t_user (id, username, password_hash, role, real_name, email) KEY(username) VALUES
(2, 'student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'STUDENT', '张三', 'student1@thesis.com');

-- 测试教师（密码：admin123）
MERGE INTO t_user (id, username, password_hash, role, real_name, email) KEY(username) VALUES
(3, 'tt', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'TEACHER', '李老师', 'teacher1@thesis.com');
