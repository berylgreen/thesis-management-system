[根目录](../CLAUDE.md) > **frontend**

# Frontend 模块文档

## 变更记录 (Changelog)

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-01-30 | 1.0.0 | 初始化前端模块文档 |

---

## 模块职责

前端 SPA（单页应用）模块，基于 Vue 3.4 + Vite 5.0 构建，提供论文管理系统的用户界面，包括登录注册、论文列表、版本管理、批改查看等功能。

---

## 入口与启动

### 主入口
- **文件**: `src/main.js`
- **职责**: 应用初始化，挂载 Vue 实例
- **依赖**: Pinia (状态管理), Vue Router, Element Plus

### 开发启动
```bash
npm install      # 安装依赖
npm run dev      # 开发模式（默认端口 3000）
npm run build    # 生产构建
npm run preview  # 预览构建结果
```

### Vite 配置
- **文件**: `vite.config.js`
- **开发端口**: 3000
- **代理配置**: `/api` -> `http://localhost:8080`

### Docker 启动
- **Dockerfile**: 多阶段构建（Node 构建 + Nginx 服务）
- **Nginx 配置**: `nginx.conf`
- **暴露端口**: 80

---

## 对外接口

### 页面路由

| 路径 | 组件 | 认证 | 说明 |
|------|------|------|------|
| /login | Login.vue | 否 | 登录/注册页面 |
| / | Home.vue | 是 | 首页 |
| /thesis | ThesisList.vue | 是 | 论文列表 |
| /thesis/:id | ThesisDetail.vue | 是 | 论文详情（版本管理） |

### 路由守卫

- **文件**: `src/router/index.js`
- **逻辑**:
  - 需认证路由（meta.requiresAuth）未登录时跳转 /login
  - 已登录用户访问 /login 时跳转首页

---

## 关键依赖与配置

### 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.4.15 | 核心框架 |
| vue-router | ^4.2.5 | 路由管理 |
| pinia | ^2.1.7 | 状态管理 |
| axios | ^1.6.5 | HTTP 请求 |
| element-plus | ^2.5.4 | UI 组件库 |

### 开发依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| vite | ^5.0.11 | 构建工具 |
| @vitejs/plugin-vue | ^5.0.3 | Vue 3 支持 |

### 请求封装

- **文件**: `src/utils/request.js`
- **特性**:
  - 请求拦截器：自动添加 `Authorization: Bearer <token>`
  - 响应拦截器：统一错误处理（Element Plus 消息提示）
  - 基础 URL: `/api`
  - 超时: 10000ms

---

## 数据模型

### 状态管理（Pinia）

| Store | 文件 | 状态字段 | 持久化 |
|-------|------|---------|--------|
| user | store/user.js | token, userInfo | 推测使用 localStorage |

---

## 视图组件

### 页面组件

| 组件 | 文件路径 | 职责 |
|------|---------|------|
| Login | views/Login.vue | 用户登录/注册表单 |
| Home | views/Home.vue | 首页/欢迎页 |
| ThesisList | views/ThesisList.vue | 论文列表展示，支持创建论文 |
| ThesisDetail | views/ThesisDetail.vue | 论文详情，版本上传/下载 |

### API 封装

| 模块 | 文件路径 | 方法 |
|------|---------|------|
| auth | api/auth.js | login(), register() |
| thesis | api/thesis.js | getMyTheses(), createThesis(), uploadVersion(), getVersions(), downloadVersion() |

---

## 测试与质量

### 当前状态
- 已配置 Vitest（见 `vite.config.js`）
- 已包含测试用例（`src/tests/VersionComparer.spec.js`）

### 推荐测试策略
1. **单元测试**: 覆盖工具函数与请求封装
2. **组件测试**: 覆盖核心表单与列表组件
3. **E2E 测试**: 使用 Playwright 或 Cypress
4. **代码质量**: 引入 ESLint + Prettier

---

## 常见问题 (FAQ)

### Q1: 如何添加新页面
**A**:
1. 在 `views/` 下创建组件文件（如 MyPage.vue）
2. 在 `router/index.js` 添加路由配置
3. 如需认证，设置 `meta: { requiresAuth: true }`

### Q2: 如何调用后端 API
**A**:
1. 在 `api/` 目录下创建或编辑模块文件
2. 使用封装的 `request` 方法（自动处理认证和错误）
3. 示例：
```javascript
import request from '../utils/request'
export function getData() {
  return request({ url: '/data', method: 'get' })
}
```

### Q3: 如何使用 Element Plus 组件
**A**: 已全局引入 Element Plus，直接在模板中使用组件即可（如 `<el-button>`）。

### Q4: Token 如何持久化
**A**: 推测使用 Pinia 插件（如 pinia-plugin-persistedstate）或手动在 store 中操作 localStorage。

---

## 相关文件清单

### 核心代码文件
```
frontend/
├── src/
│   ├── main.js                  # 应用入口
│   ├── App.vue                  # 根组件
│   ├── views/
│   │   ├── Login.vue            # 登录页
│   │   ├── Home.vue             # 首页
│   │   ├── ThesisList.vue       # 论文列表
│   │   └── ThesisDetail.vue     # 论文详情
│   ├── api/
│   │   ├── auth.js              # 认证 API
│   │   └── thesis.js            # 论文 API
│   ├── router/
│   │   └── index.js             # 路由配置
│   ├── store/
│   │   └── user.js              # 用户状态
│   └── utils/
│       └── request.js           # Axios 封装
├── index.html                   # HTML 模板
├── vite.config.js               # Vite 配置
├── package.json                 # 项目依赖
├── nginx.conf                   # Nginx 配置
└── Dockerfile                   # Docker 镜像
```

### 配置文件
- `package.json` - 项目依赖和脚本
- `vite.config.js` - Vite 构建配置
- `nginx.conf` - Nginx 服务配置
- `Dockerfile` - Docker 镜像构建脚本

---

## 下一步开发建议

1. **添加测试**: 引入 Vitest，覆盖核心组件
2. **代码规范**: 配置 ESLint + Prettier
3. **性能优化**: 路由懒加载、组件按需引入
4. **状态持久化**: 明确 Token 持久化方案（推荐 pinia-plugin-persistedstate）
5. **错误处理**: 完善网络错误、Token 过期的处理逻辑
6. **国际化**: 引入 vue-i18n（如需多语言支持）
7. **主题定制**: Element Plus 主题定制（CSS 变量）
