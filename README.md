# 毕业论文管理系统（精简版）

基于 Spring Boot 3.x + Vue 3 的单体架构论文管理系统，提供论文提交、版本管理、批改与对比功能。

## 快速开始

### ⚠️ 前置条件

> **必须先启动 Docker Desktop！** 本项目依赖 Docker 运行 MySQL 和 Redis 容器。
> 启动前请确认 Docker Desktop 已运行（系统托盘可见鲸鱼图标）。

### 开发模式（推荐）

```powershell
# 一键启动（Docker 容器 + 后端 + 前端）
.\dev.bat start
# 或
.\dev.ps1 -Action start

# 查看服务状态
.\dev.bat status

# 停止所有服务
.\dev.bat stop

# 重启所有服务
.\dev.bat restart
```

开发环境端口:

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| MySQL | localhost:23306 |
| Redis | localhost:6379 |

日志文件位于项目根目录：`backend.log` / `frontend.log`

### Docker Compose 全栈部署

```bash
docker-compose up -d
```

| 服务 | 地址 |
|------|------|
| 前端 (Nginx) | http://localhost:8888 |
| 后端 API | http://localhost:8080/api |

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
