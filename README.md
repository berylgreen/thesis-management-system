# 毕业论文管理系统（精简版）

基于 Spring Boot 3.x + Vue 3 的单体架构论文管理系统，提供论文提交、版本管理、批改与对比功能。

## 快速开始

### 前置条件

- **Java**: JDK 17+
- **Node.js**: v18+ (推荐 LTS 版本)
- **Maven**: 3.6+

> 无需安装数据库，开发环境使用 H2 内嵌数据库，零外部依赖。

### 开发模式

```powershell
# 一键启动（后端 + 前端）
.\dev.bat start

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
| H2 Console | http://localhost:8080/h2-console |

日志文件位于项目根目录：`backend.log` / `frontend.log`

## 文档索引

- `docs/PROJECT_GUIDE.md` 项目整合说明（推荐先读）
- `docs/ENVIRONMENT_SETUP.md` 环境变量与部署配置
- `docs/用户使用手册.md` 用户使用说明
- `backend/CLAUDE.md` 后端模块文档
- `frontend/CLAUDE.md` 前端模块文档

## 技术栈

- Spring Boot 3.2.1 / MyBatis-Plus 3.5.5 / Spring Security + JWT
- Vue 3.4 / Vite 5 / Element Plus 2.5 / Pinia / Vue Router
- H2 (开发) / MySQL 8.0 (生产)

## 许可证

MIT License
