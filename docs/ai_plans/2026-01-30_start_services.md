# 启动服务实施计划

**目标**: 启动基于 Docker 的论文管理系统服务 (MySQL, Redis, Backend, Frontend)。
**日期**: 2026-01-30

## 1. 预检与分析

- **文件检查**: `docker-compose.yml` 存在且配置完整。
- **端口检查**:
  - MySQL: 13306 -> 3306
  - Redis: 6379 -> 6379 (注意：本地可能已运行 Redis，若冲突需处理，但在 Docker 内通常通常映射到不同端口或依赖容器IP，此处映射到宿主机 6379 可能冲突)
  - Backend: 8080 -> 8080
  - Frontend: 8888 -> 80 (Windows 端口保留/权限问题: 3000/3001 不可用)

## 2. 启动步骤

1. **构建并启动**: 使用 `docker-compose up -d --build` 强制重新构建镜像以包含最新更改。
2. **健康检查**: 查看容器运行状态 `docker-compose ps`。
3. **日志监控**: 若有服务启动失败，查看日志 `docker-compose logs -f [service_name]`。

## 3. 风险评估

- **端口冲突**:
  - 本地若运行了 MySQL (3306) 或 Redis (6379)，Docker 映射可能会失败。
  - `docker-compose.yml` 将 MySQL 映射到 13306，风险较低。
  - Redis 映射到 6379，**高风险**。若本地有 Redis，需修改映射或停止本地 Redis。
  - Backend 8080 端口常见冲突。
- **构建失败**: 网络问题（Maven/NPM 依赖下载）。

## 4. 验证标准

- 所有容器状态为 `Up`。
- 前端可通过 `http://localhost:8888` 访问。
- 后端 API 可通过 `http://localhost:8080` 访问。
