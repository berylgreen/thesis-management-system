# 毕业论文管理系统 - 技术规划方案

> **创建日期**: 2026-01-09  
> **状态**: 待审批

---

## 1. 系统概述

### 1.1 核心业务目标

- **论文提交**：学生端上传多版本论文（Word/PDF）
- **论文批改**：教师端在线批注、评分、反馈
- **版本对比**：Myers Diff 算法实现论文版本间差异可视化
- **AI 辅助**：通过 MCP 集成 LLM 实现查重预检、格式校验、智能建议

### 1.2 用户角色

| 角色 | 权限范围 |
|------|----------|
| 学生 | 提交论文、查看批改意见、查看版本历史 |
| 教师 | 批改论文、评分、查看学生提交列表 |
| 管理员 | 用户管理、系统配置、数据统计 |

---

## 2. 技术选型

### 2.1 核心技术栈

| 层级 | 选型 | 选择理由 |
|------|------|----------|
| 服务框架 | Spring Cloud Alibaba 2023.x | Netflix 组件已停更，Alibaba 生态活跃 |
| 注册中心 | Nacos 2.x | 支持动态配置 + 服务发现一体化 |
| 网关 | Spring Cloud Gateway | 基于 Reactor，非阻塞性能更优 |
| 流控 | Sentinel | Hystrix 停更，Sentinel 持续迭代 |
| 对象存储 | MinIO | 私有化部署，无云厂商锁定 |
| 消息队列 | RocketMQ | 与 Alibaba 生态集成度高 |
| 文档对比 | Java Diff Utils + difflib | 开源免费，Myers 算法成熟 |

---

## 3. 微服务拆分

### 3.1 服务清单

| 服务名 | 端口 | 语言 | 职责 |
|--------|------|------|------|
| thesis-gateway | 8080 | Java | API 路由/限流/鉴权 |
| thesis-user | 9001 | Java | 用户认证/JWT/RBAC |
| thesis-doc | 9002 | Java | 文档上传下载/版本管理 |
| thesis-review | 9003 | Java | 批改/评分/批注 |
| thesis-diff | 9004 | Java | 版本对比/差异计算 |
| thesis-mcp | 9010 | Python | MCP Server/AI 辅助 |

---

## 4. 数据库设计

### 4.1 核心表结构

**用户表 (t_user)**

- id, username, password_hash, role, created_at

**论文表 (t_thesis)**

- id, student_id, title, status, current_version, created_at

**论文版本表 (t_thesis_version)**

- id, thesis_id, version_num, file_path, content_hash, uploaded_at

**批改记录表 (t_review)**

- id, version_id, teacher_id, comment, score, reviewed_at

---

## 5. MCP 集成方案

### 5.1 MCP 应用场景

| 场景 | 触发时机 | MCP 能力 |
|------|----------|----------|
| 格式预检 | 论文上传后 | 检查标题、目录、参考文献格式 |
| 查重预检 | 提交前 | 调用 LLM 进行语义相似度分析 |
| 智能批注建议 | 教师批改时 | 根据上下文推荐批改意见 |
| 版本摘要 | 版本对比时 | 自动生成修改内容摘要 |

### 5.2 MCP Server 结构 (Python)

```
thesis-mcp/
├── server.py           # MCP Server 入口
├── tools/
│   ├── format_check.py # 格式检查工具
│   ├── similarity.py   # 相似度分析工具
│   └── summarize.py    # 摘要生成工具
└── requirements.txt
```

---

## 6. 部署架构

### 6.1 Docker Compose 编排

```
基础设施: MySQL 8.0 + Redis 7 + Nacos 2.x + MinIO
Java 服务: gateway, user, doc, review, diff
Python 服务: mcp
前端: Vue 3 + Nginx
```

---

## 7. 开发路线图

| 阶段 | 周期 | 任务 |
|------|------|------|
| Phase 1 | Week 1-2 | 基础框架搭建、Nacos 配置、JWT 认证 |
| Phase 2 | Week 3-4 | 核心业务：文档上传、批改流程 |
| Phase 3 | Week 5 | 版本对比服务实现 |
| Phase 4 | Week 6 | MCP Server 开发与集成 |
| Phase 5 | Week 7-8 | Docker 部署、集成测试 |

---

## 8. 风险评估

| 风险项 | 影响 | 缓解策略 |
|--------|------|----------|
| 大文件上传超时 | 高 | 分片上传 + 断点续传 |
| 版本对比内存溢出 | 中 | 流式处理 + 分页加载 |
| MCP 服务不可用 | 中 | 降级策略，非核心功能 |
| 国内 LLM API 访问受限 | 高 | 预留 Ollama + Qwen 本地模型接口 |

---

## 9. 项目目录结构

```
thesis-system/
├── thesis-gateway/          # Spring Cloud Gateway
├── thesis-user/             # 用户认证服务 (Java)
├── thesis-doc/              # 文档管理服务 (Java)
├── thesis-review/           # 批改评审服务 (Java)
├── thesis-diff/             # 版本对比服务 (Java)
├── thesis-mcp/              # MCP Server (Python)
├── thesis-web/              # 前端 (Vue 3)
├── thesis-common/           # 公共模块 (Java)
├── docker-compose.yml
└── docs/ai_plans/
```
