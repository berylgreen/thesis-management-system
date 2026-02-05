[根目录](../CLAUDE.md) > **backend**

# Backend 模块文档

## 变更记录 (Changelog)

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-01-30 | 1.0.0 | 初始化后端模块文档 |

---

## 模块职责

后端服务模块，基于 Spring Boot 3.2.1 构建，提供论文管理系统的所有 RESTful API 接口，包括用户认证、论文管理、版本控制、批改流程和版本对比功能。

---

## 入口与启动

### 主类
- **文件**: `src/main/java/com/thesis/ThesisApplication.java`
- **注解**: `@SpringBootApplication`, `@MapperScan("com.thesis.mapper")`
- **启动命令**: `mvn spring-boot:run`

### 配置文件
- **文件**: `src/main/resources/application.yml`
- **端口**: 8080
- **数据源**: MySQL (localhost:3306/thesis_system)
- **Redis**: localhost:6379
- **文件上传**: 最大 50MB

### Docker 启动
- **Dockerfile**: 多阶段构建（Maven 编译 + JRE 运行）
- **暴露端口**: 8080
- **依赖服务**: MySQL, Redis

---

## 对外接口

### API 控制器层

| 控制器 | 路径前缀 | 职责 | 文件路径 |
|--------|---------|------|---------|
| AuthController | /api/auth | 用户注册、登录 | controller/AuthController.java |
| ThesisController | /api/thesis | 论文 CRUD、版本上传/下载 | controller/ThesisController.java |
| ReviewController | /api/review | 批改记录管理 | controller/ReviewController.java |
| DiffController | /api/diff | 版本对比 | controller/DiffController.java |

### 关键端点

**认证接口**
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

**论文接口**
- `POST /api/thesis/create` - 创建论文
- `POST /api/thesis/{thesisId}/upload` - 上传论文版本
- `GET /api/thesis/my` - 查询当前用户论文
- `GET /api/thesis/{thesisId}/versions` - 获取论文版本列表
- `GET /api/thesis/version/{versionId}/download` - 下载版本文件

**批改接口**
- `POST /api/review/create` - 创建批改记录
- `GET /api/review/version/{id}` - 获取版本批改
- `GET /api/review/my` - 查询我的批改记录

**对比接口**
- `GET /api/diff/compare?version1Id={id1}&version2Id={id2}` - 版本对比

---

## 关键依赖与配置

### Maven 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.1 | 基础框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL Connector | - | 数据库驱动 |
| Spring Security | - | 安全框架 |
| JJWT | 0.12.5 | JWT 令牌生成/解析 |
| Java Diff Utils | 4.12 | 文本差异对比 |
| Lombok | - | 代码简化 |
| Spring Data Redis | - | Redis 集成 |

### 配置说明

**JWT 配置**
```yaml
jwt:
  secret: your-256-bit-secret-key-change-this-in-production-environment
  expiration: 86400000  # 24小时
```

**文件上传配置**
```yaml
file:
  upload-path: ./uploads/
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

**MyBatis-Plus 配置**
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
```

---

## 数据模型

### 实体类（Entity）

| 实体 | 表名 | 文件路径 | 主要字段 |
|------|------|---------|---------|
| User | t_user | entity/User.java | id, username, passwordHash, role, realName, email |
| Thesis | t_thesis | entity/Thesis.java | id, studentId, title, status, currentVersion |
| ThesisVersion | t_thesis_version | entity/ThesisVersion.java | id, thesisId, versionNum, filePath, contentHash |
| Review | t_review | entity/Review.java | id, versionId, teacherId, comment, score, status |

### Mapper 层

| Mapper | 继承 | 文件路径 |
|--------|------|---------|
| UserMapper | BaseMapper<User> | mapper/UserMapper.java |
| ThesisMapper | BaseMapper<Thesis> | mapper/ThesisMapper.java |
| ThesisVersionMapper | BaseMapper<ThesisVersion> | mapper/ThesisVersionMapper.java |
| ReviewMapper | BaseMapper<Review> | mapper/ReviewMapper.java |

---

## 安全与认证

### Spring Security 配置

- **文件**: `config/SecurityConfig.java`
- **策略**: 无状态（SessionCreationPolicy.STATELESS）
- **密码编码**: BCryptPasswordEncoder
- **公开路径**: `/api/auth/**`
- **认证方式**: JWT 令牌（Header: `Authorization: Bearer <token>`）

### JWT 过滤器

- **文件**: `config/JwtAuthenticationFilter.java`
- **位置**: UsernamePasswordAuthenticationFilter 之前
- **逻辑**:
  1. 从请求头提取 Bearer 令牌
  2. 验证令牌有效性
  3. 解析用户 ID 并设置到 SecurityContext

### JWT 工具类

- **文件**: `util/JwtUtil.java`
- **方法**:
  - `generateToken(userId, username, role)`: 生成令牌
  - `parseToken(token)`: 解析令牌
  - `validateToken(token)`: 验证令牌
  - `getUserIdFromToken(token)`: 获取用户 ID

---

## 核心业务逻辑

### Service 层

| Service | 文件路径 | 主要方法 |
|---------|---------|---------|
| UserService | service/UserService.java | register(), login() |
| ThesisService | service/ThesisService.java | createThesis(), uploadVersion(), getVersionFile() |
| ReviewService | service/ReviewService.java | createReview(), getReviews() |
| DiffService | util/DiffUtil.java | compareVersions() (基于 Java Diff Utils) |

---

## 测试与质量

### 当前状态
- 已引入 `spring-boot-starter-test` 依赖（scope: test）
- 未发现测试用例文件

### 推荐测试策略
1. **单元测试**: Service 层使用 JUnit 5 + Mockito
2. **集成测试**: Controller 层使用 @SpringBootTest + MockMvc
3. **安全测试**: 验证 JWT 过滤器、角色权限
4. **文件上传测试**: 测试大文件、异常文件类型

---

## 常见问题 (FAQ)

### Q1: JWT 密钥安全性问题
**A**: 当前密钥为示例值，生产环境必须替换为随机生成的 256 位密钥，建议存储在环境变量或配置中心。

### Q2: 文件存储方案
**A**: 当前使用本地文件系统（./uploads/），生产环境建议迁移到对象存储（MinIO、OSS、S3）。

### Q3: 如何添加新的 API 端点
**A**:
1. 在 Controller 层添加方法，使用 @RequestMapping 注解
2. 在 Service 层实现业务逻辑
3. 如需数据库操作，在 Mapper 层添加方法
4. 更新接口文档

### Q4: 如何调试 MyBatis SQL
**A**: application.yml 中已配置 `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`，控制台会输出 SQL 语句。

---

## 相关文件清单

### 核心代码文件
```
backend/
├── src/main/java/com/thesis/
│   ├── ThesisApplication.java          # 启动类
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ThesisController.java
│   │   ├── ReviewController.java
│   │   └── DiffController.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ThesisService.java
│   │   └── ReviewService.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Thesis.java
│   │   ├── ThesisVersion.java
│   │   └── Review.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── ThesisMapper.java
│   │   ├── ThesisVersionMapper.java
│   │   └── ReviewMapper.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── WebConfig.java
│   │   └── MyBatisPlusMetaObjectHandler.java
│   ├── util/
│   │   ├── JwtUtil.java
│   │   ├── DiffUtil.java
│   │   └── Result.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── LoginResponse.java
│       └── RegisterRequest.java
├── src/main/resources/
│   └── application.yml
├── pom.xml
└── Dockerfile
```

### 配置文件
- `pom.xml` - Maven 项目配置
- `application.yml` - Spring Boot 应用配置
- `Dockerfile` - Docker 镜像构建脚本

---

## 下一步开发建议

1. **添加全局异常处理器**: 使用 @ControllerAdvice 统一处理异常
2. **完善日志记录**: 引入 AOP 切面记录操作日志
3. **添加接口文档**: 集成 SpringDoc (OpenAPI 3)
4. **优化查询性能**: 使用 Redis 缓存论文列表
5. **添加单元测试**: 覆盖核心业务逻辑
6. **角色权限细化**: 使用 @PreAuthorize 注解控制接口权限
