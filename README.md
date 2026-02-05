# 毕业论文管理系统（精简版）

基于 Spring Boot 3.x + Vue 3 的单体架构论文管理系统，提供论文提交、版本管理、批改与对比功能。

## 快速开始

### 开发模式

```powershell
.\start-dev.ps1
```

停止服务:

```powershell
.\stop-dev.ps1
```

开发环境端口:
- 前端: http://localhost:3000
- 后端 API: http://localhost:8080
- MySQL: localhost:13306
- Redis: localhost:6379

### Docker Compose（全栈）

```bash
docker-compose up -d
```

服务端口:
- 前端 (Nginx): http://localhost:8888
- 后端 API: http://localhost:8080/api

## 文档索引

- `docs/PROJECT_GUIDE.md` 项目整合说明（推荐先读）
- `docs/ENVIRONMENT_SETUP.md` 环境变量与部署配置
- `docs/用户使用手册.md` 用户使用说明
- `backend/CLAUDE.md` 后端模块文档
- `frontend/CLAUDE.md` 前端模块文档

## 技术栈

- Spring Boot 3.2.1 / MyBatis-Plus 3.5.5 / Spring Security + JWT
- Vue 3.4 / Vite 5 / Element Plus 2.5 / Pinia / Vue Router
- MySQL 8.0 / Redis 7.0

## 许可证

MIT License
