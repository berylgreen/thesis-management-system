# 整合启动/停止脚本计划

**日期**: 2026-02-04

## 目标

将现有的启动/停止脚本整合为一个脚本，支持参数控制（如 `start`, `stop`），并且**不打开新窗口**（后台运行服务）。

## 变更计划

### [NEW] `dev.ps1`

核心 PowerShell 脚本，接受一个参数 `$Action`。

#### Action: `start`

1. **启动 Docker**: `docker-compose up -d`
2. **启动后端 (Spring Boot)**:
   - 使用 `Start-Process` 后台运行 `mvn spring-boot:run`。
   - 窗口样式: `Hidden` (无窗口)。
   - 重定向输出: `backend.log` (标准输出) 和 `backend.err.log` (错误输出)。
3. **启动前端 (Vite)**:
   - 使用 `Start-Process` 后台运行 `npm run dev`。
   - 窗口样式: `Hidden` (无窗口)。
   - 重定向输出: `frontend.log` (标准输出) 和 `frontend.err.log` (错误输出)。
4. **输出**: 提示服务已在后台启动，并告知日志文件位置。

#### Action: `stop`

1. **停止 Docker**: `docker-compose down`
2. **停止应用进程**:
   - 复用之前的逻辑，通过端口 (8080, 5173) 查找 PID 并终止进程。

#### Action: `restart`

1. 执行 `stop`。
2. 执行 `start`。

### [NEW] `dev.bat`

批处理包装脚本，方便用户调用。
用法: `dev start`, `dev stop`, `dev restart`。
内容: 转发参数给 `dev.ps1`。

### [DELETE] 旧脚本

- `start-dev.ps1`, `stop-dev.ps1`
- `start.bat`, `stop.bat`

## 验证计划

1. 运行 `dev start`。
   - 验证没有新弹出窗口。
   - 验证 `backend.log` 和 `frontend.log` 有内容生成。
   - 验证 `http://localhost:5173` 和 `http://localhost:8080` 可访问。
2. 运行 `dev stop`。
   - 验证所有相关进程已结束。
