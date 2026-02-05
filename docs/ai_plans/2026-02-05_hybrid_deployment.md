# 部署计划存档

## 目标

- **后端**：本地运行 (Port 8080)
- **前端**：本地运行 (Port 3000)
- **数据库/缓存**：Docker 运行 (MySQL Port 13306, Redis Port 6379)

## 变更内容

### `dev.ps1`

- 修正前端端口检测：`5173` -> `3000` (适配 `vite.config.js`)
- 修正停止服务逻辑：增加对端口 `3000` 的清理

## 验证方法

1. PowerShell 运行 `.\dev.ps1 start`
2. 检查控制台输出是否全绿
3. 访问 `http://localhost:3000`
