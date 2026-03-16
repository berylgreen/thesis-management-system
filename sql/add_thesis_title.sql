-- 给用户表增加论文题目字段
USE thesis_system;

ALTER TABLE t_user ADD COLUMN thesis_title VARCHAR(200) DEFAULT NULL COMMENT '论文题目' AFTER email;
