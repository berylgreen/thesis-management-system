# 毕业论文管理系统 - 项目整合说明

最后更新: 2026-02-02

## 项目概览

- 目标: 管理毕业论文的提交、版本、批改与对比
- 架构: 单体应用（Spring Boot + Vue 3），前后端分离部署
- 典型场景: 个人开发或校园低并发环境

## 技术栈

**后端**: Spring Boot 3.2.1, MyBatis-Plus 3.5.5, Spring Security + JWT 0.12.5, MySQL 8.0, Redis 7.0, Java Diff Utils 4.12

**前端**: Vue 3.4, Vite 5, Element Plus 2.5, Pinia, Vue Router 4, Axios

## 目录结构

```
test16/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── docs/             # 文档
├── sql/              # 数据库脚本
├── uploads/          # 文件上传目录（运行时）
├── start-dev.ps1     # 启动脚本
├── stop-dev.ps1      # 停止脚本
├── docker-compose.yml        # 全栈 Docker
└── docker-compose.dev.yml    # 本地数据库（MySQL/Redis）
```

## 运行方式

### 开发模式（推荐）

1. 启动本地开发环境

```powershell
.\start-dev.ps1
```

2. 停止服务

```powershell
.\stop-dev.ps1
```

开发环境端口:
- 前端: http://localhost:5173
- 后端 API: http://localhost:8080
- MySQL: localhost:23306
- Redis: localhost:6379

### Docker Compose（全栈）

```bash
docker-compose up -d
```

服务端口:
- 前端 (Nginx): http://localhost:8888
- 后端 API: http://localhost:8080/api
- MySQL: localhost:23306
- Redis: localhost:6379

## 环境变量

后端配置读取自 `backend/src/main/resources/application.yml`，主要环境变量如下:

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DB_URL | MySQL 连接串 | jdbc:mysql://localhost:3306/thesis_system... |
| DB_USERNAME | MySQL 用户名 | root |
| DB_PASSWORD | MySQL 密码 | root |
| REDIS_HOST | Redis 主机 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| REDIS_DATABASE | Redis 库号 | 0 |
| REDIS_PASSWORD | Redis 密码 | 空 |
| FILE_UPLOAD_PATH | 上传目录 | ./uploads/ |
| JWT_SECRET | JWT 签名密钥 | 示例值（生产必须替换） |
| JWT_EXPIRATION | JWT 过期时间（毫秒） | 86400000 |

## 认证与安全

- 认证方式: JWT Bearer Token
- Header: `Authorization: Bearer <token>`
- 后端 CORS 默认允许: http://localhost:8888、http://localhost:5173、http://localhost:3001

## 接口约定

统一响应结构 `Result<T>`:

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

API 速览:
- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录
- `POST /api/thesis/create` 创建论文
- `POST /api/thesis/{thesisId}/upload` 上传版本
- `GET /api/thesis/my` 我的论文
- `GET /api/thesis/{thesisId}/versions` 版本列表
- `GET /api/thesis/version/{versionId}/download` 下载版本
- `POST /api/review/create` 创建批改
- `GET /api/review/version/{versionId}` 版本批改列表
- `GET /api/review/my` 我的批改
- `GET /api/diff/compare?version1Id=...&version2Id=...` 版本对比

## 数据库

初始化脚本: `sql/init.sql`

核心表:
- `t_user`: 用户（STUDENT/TEACHER/ADMIN）
- `t_thesis`: 论文
- `t_thesis_version`: 论文版本
- `t_review`: 批改记录

## 文件上传

- 默认目录: `./uploads/`
- 最大文件大小: 50MB

## 测试

后端测试: `backend/src/test/java`（AuthControllerTest、DiffControllerTest、ThesisServiceTest）

前端测试: `frontend/src/tests`（VersionComparer.spec.js）

常用命令:
- 后端: `mvn test`
- 前端: `npm run test`

## 默认账号

初始化脚本内置账号（密码均为 `admin123`）:
- 管理员: `admin`
- 学生: `student1`
- 教师: `teacher1`

## 文档索引

- `README.md` 项目入口说明
- `docs/ENVIRONMENT_SETUP.md` 环境变量与部署配置
- `docs/用户使用手册.md` 用户使用说明
- `backend/CLAUDE.md` 后端模块文档
- `frontend/CLAUDE.md` 前端模块文档
