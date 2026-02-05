# 启动/停止脚本重写计划

**日期**: 2026-02-04

## 目标

创建 `start-dev.ps1` 和 `stop-dev.ps1` 脚本，用于一键启动和停止本地开发环境。

## 现状分析

- **依赖服务**: MySQL, Redis (通过 `docker-compose.dev.yml` 管理)
- **后端**: Java Spring Boot (位于 `backend/`)
- **前端**: Vue + Vite (位于 `frontend/`)
- **环境**: Windows (PowerShell)

## 变更计划

### [NEW] `start-dev.ps1`

该脚本将执行以下步骤：

1. **启动基础设施**: `docker-compose -f docker-compose.dev.yml up -d`
2. **启动后端**: 在新窗口中运行 `mvn spring-boot:run` (目录: `backend/`)
3. **启动前端**: 在新窗口中运行 `npm run dev` (目录: `frontend/`)
4. **输出**: 打印各服务启动状态和访问地址。

### [NEW] `stop-dev.ps1`

该脚本将执行以下步骤：

1. **停止基础设施**: `docker-compose -f docker-compose.dev.yml down`
2. **停止应用进程**:
   - 尝试通过端口（如 8080, 5173）查找并关闭相关进程。
   - 或者提示用户手动关闭终端窗口（较安全）。
   - *决定*: 脚本将尝试查找占用特定端口的进程并关闭。

## 验证计划 (Verification Plan)

### 自动化验证

- 运行脚本并检查端口是否监听。

### 手动验证

1. 运行 `.\start-dev.ps1`
2. 观察是否弹出两个新窗口（后端、前端）。
3. 检查 Docker 容器是否运行。
4. 访问 `http://localhost:5173` (前端) 和 `http://localhost:8080` (后端)。
5. 运行 `.\stop-dev.ps1`
6. 检查容器是否停止，端口是否释放。
