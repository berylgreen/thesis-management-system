# 毕业论文管理系统 - 精简版设计方案

> 本方案采用单体架构，适合个人开发者和校园低并发场景。

**创建日期**: 2026-01-09  
**版本类型**: 精简版 (Lite)

---

## 1. 架构对比

| 维度 | 原版 (微服务) | 精简版 (单体) |
|------|---------------|---------------|
| 服务数量 | 6 个 | 1 个 |
| 中间件 | Nacos/Sentinel/RocketMQ | 无 |
| 部署复杂度 | 高 | 低 |
| 开发周期 | 8 周 | 3-4 周 |
| 适用场景 | 多团队/高并发 | 个人开发/校园系统 |

---

## 2. 技术选型

| 层级 | 选型 | 理由 |
|------|------|------|
| 后端框架 | Spring Boot 3.x | 单体，无需服务发现 |
| ORM | MyBatis-Plus | 开发效率高 |
| 认证 | JWT (jjwt) | 无状态，无需 Session |
| 文档对比 | Java Diff Utils | 开源，Myers 算法 |
| 文件存储 | 本地文件系统 / MinIO | 初期本地，后期可迁移 |
| AI 辅助 | Python 脚本直接调用 | 无需 MCP 协议开销 |

---

## 3. 项目结构

```
thesis-system/
├── backend/
│   └── src/main/java/com/thesis/
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── ThesisController.java
│       │   └── ReviewController.java
│       ├── service/
│       │   ├── UserService.java
│       │   ├── ThesisService.java
│       │   ├── ReviewService.java
│       │   └── DiffService.java
│       ├── entity/
│       ├── mapper/
│       └── util/
│           └── DiffUtil.java
├── frontend/                 # Vue 3
├── scripts/                  # 可选 Python AI 脚本
│   └── format_check.py
├── sql/init.sql
└── docker-compose.yml
```

---

## 4. 数据库设计

- t_user: 用户表 (id, username, password_hash, role, created_at)
- t_thesis: 论文表 (id, student_id, title, status, current_version)
- t_thesis_version: 版本表 (id, thesis_id, version_num, file_path, content_hash)
- t_review: 批改表 (id, version_id, teacher_id, comment, score)

---

## 5. AI 辅助 (精简方案)

无需 MCP Server，直接通过 HTTP 调用 Ollama/Qwen 本地模型或 Python 脚本。

---

## 6. 部署

docker-compose: MySQL 8.0 + Redis 7 + Backend + Frontend

---

## 7. 开发路线图

| 阶段 | 周期 | 任务 |
|------|------|------|
| Phase 1 | Week 1 | Spring Boot + JWT + 用户模块 |
| Phase 2 | Week 2 | 论文上传下载 + 版本管理 |
| Phase 3 | Week 3 | 批改流程 + 版本对比 |
| Phase 4 | Week 4 | 前端完善 + Docker 部署 |
