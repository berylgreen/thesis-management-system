---
created_at: 2026-01-30
target: Import students from markdown to DB and file system
---

# 自动化实施计划：学生数据导入

## 1. 目标

解析 `source/stu.md` 学生名单，完成以下自动化操作：

1. 在 MySQL 数据库 `t_user` 表中注册学生账号。
2. 在 `uploads/` 目录下为每位学生建立独立文件夹。

## 2. 影响范围

- **Database**: `thesis_system.t_user` 表。
- **FileSystem**: `uploads/` 目录。

## 3. 实施步骤

1. **编写脚本** (`scripts/import_students.py`)：
    - 读取 Markdown 表格数据。
    - 清洗数据（去除空白、校验学号格式）。
    - 生成 SQL 文件 `sql/import_students.sql`。
    - 创建对应物理文件夹。
2. **执行导入**：
    - 将 SQL 推送至 Docker 容器执行。
3. **验证**：
    - 核对记录数与文件夹数是否一致。

## 4. 风险评估

- **重复导入**：SQL 使用 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE` 避免主键冲突。
- **编码问题**：中文姓名需确保 UTF-8 编码正确，避免乱码。
- **文件权限**：Docker 挂载卷的权限需确保 Java 后端容器可读写创建的目录（通常 `chmod 755`）。

## 5. 验证标准

- [ ] 数据库新增学生记录数 = Markdown 行数。
- [ ] `uploads/` 下文件夹数量增加数 = Markdown 行数。
- [ ] 任意抽取一账号可成功登录系统。
