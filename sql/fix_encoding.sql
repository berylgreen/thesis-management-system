-- 修复 real_name 字段的双重 UTF-8 编码问题
-- 问题：数据在导入时被错误地进行了二次 UTF-8 编码（UTF-8 字节被当作 Latin1 读取，再次以 UTF-8 编码存储）
-- 解决方案：将数据转换为二进制，然后使用 Latin1 解码，再用 UTF-8 编码

USE thesis_system;

-- 备份当前数据（可选，谨慎起见）
-- CREATE TABLE t_user_backup AS SELECT * FROM t_user;

-- 修复 real_name 字段
UPDATE t_user
SET real_name = CONVERT(CONVERT(CONVERT(real_name USING BINARY) USING latin1) USING utf8mb4)
WHERE real_name IS NOT NULL
  AND role = 'STUDENT'
  AND LENGTH(real_name) != CHAR_LENGTH(real_name);

-- 验证修复结果
SELECT id, username, real_name,
       LENGTH(real_name) as byte_len,
       CHAR_LENGTH(real_name) as char_len
FROM t_user
WHERE real_name IS NOT NULL AND role='STUDENT'
LIMIT 10;
