# 更新教师账号名为 tt

本计划旨在将系统中预置的教师账号名从 `teacher1` 更新为 `tt`，包括修改初始化脚本和更新 Docker 容器中运行的数据库数据。

## 用户评审确认

- **账号覆盖范围**：仅修改 `username` 为 `tt`。
- **关联数据**：修改 `username` 不影响基于 `id` 的外键关联。

## 方案设计

### 1. 数据库脚本更新

修改 `sql/init.sql`，将 `teacher1` 的插入语句更新为 `tt`。

### 2. Docker 数据库数据更新

直接在正在运行的 `thesis-mysql` 容器中执行 SQL 更新语句。

## 验证计划

1. 执行 `SELECT username FROM t_user WHERE role='TEACHER';` 确认结果。
