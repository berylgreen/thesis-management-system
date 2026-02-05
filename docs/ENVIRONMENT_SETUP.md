# 环境变量配置指南

本项目已将敏感配置迁移到环境变量，提高安全性。

## 环境变量列表

### 数据库配置

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `DB_URL` | 数据库连接 URL | `jdbc:mysql://localhost:3306/thesis_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai` | `jdbc:mysql://prod-db:3306/thesis_system?useSSL=true` |
| `DB_USERNAME` | 数据库用户名 | `root` | `thesis_user` |
| `DB_PASSWORD` | 数据库密码 | `root` | `your_secure_password` |

### Redis 配置

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `REDIS_HOST` | Redis 主机地址 | `localhost` | `redis.example.com` |
| `REDIS_PORT` | Redis 端口 | `6379` | `6379` |
| `REDIS_DATABASE` | Redis 数据库编号 | `0` | `0` |
| `REDIS_PASSWORD` | Redis 密码 | 空 | `your_redis_password` |

### JWT 配置

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `JWT_SECRET` | JWT 签名密钥（⚠️ 必须修改） | 示例值 | 见下方生成方法 |
| `JWT_EXPIRATION` | JWT 过期时间（毫秒） | `86400000` (24小时) | `3600000` (1小时) |

### 文件存储配置

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `FILE_UPLOAD_PATH` | 文件上传路径 | `./uploads/` | `/var/thesis/uploads/` |

## 配置方法

### 方法 1: 使用 .env 文件（开发环境）

1. 在后端项目根目录创建 `.env` 文件：

```bash
cd backend
cat > .env << 'EOF'
# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/thesis_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
REDIS_PASSWORD=

# JWT 配置（⚠️ 请替换为强密钥）
JWT_SECRET=your-generated-256-bit-secret-key-here
JWT_EXPIRATION=86400000

# 文件存储
FILE_UPLOAD_PATH=./uploads/
EOF
```

2. 使用 `spring-boot-dotenv` 或手动加载环境变量

### 方法 2: 使用环境变量（生产环境）

**Linux/macOS:**

```bash
export DB_URL="jdbc:mysql://prod-db:3306/thesis_system?useSSL=true"
export DB_USERNAME="thesis_user"
export DB_PASSWORD="your_secure_password"
export JWT_SECRET="your-generated-256-bit-secret-key-here"
# ... 其他变量
```

**Windows (PowerShell):**

```powershell
$env:DB_URL="jdbc:mysql://prod-db:3306/thesis_system?useSSL=true"
$env:DB_USERNAME="thesis_user"
$env:DB_PASSWORD="your_secure_password"
$env:JWT_SECRET="your-generated-256-bit-secret-key-here"
# ... 其他变量
```

### 方法 3: Docker Compose

在 `docker-compose.yml` 中添加：

```yaml
services:
  backend:
    environment:
      - DB_URL=jdbc:mysql://mysql:3306/thesis_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      - DB_USERNAME=root
      - DB_PASSWORD=${DB_PASSWORD}  # 从主机环境变量读取
      - REDIS_HOST=redis
      - JWT_SECRET=${JWT_SECRET}    # 从主机环境变量读取
      - JWT_EXPIRATION=86400000
      - FILE_UPLOAD_PATH=/app/uploads/
```

### 方法 4: Kubernetes ConfigMap/Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: thesis-secrets
type: Opaque
stringData:
  DB_PASSWORD: your_db_password
  JWT_SECRET: your_jwt_secret
  REDIS_PASSWORD: your_redis_password
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: thesis-config
data:
  DB_URL: "jdbc:mysql://mysql-service:3306/thesis_system"
  DB_USERNAME: "thesis_user"
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
```

## 🔐 生成安全的 JWT 密钥

**重要**: 生产环境必须使用强随机密钥（至少 256 位）。

### 方法 1: 使用 OpenSSL

```bash
openssl rand -base64 32
```

### 方法 2: 使用 Java

```java
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

public class GenerateSecret {
    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("JWT_SECRET=" + base64Key);
    }
}
```

### 方法 3: 使用在线工具

访问 https://generate-secret.vercel.app/32 （确保使用 HTTPS）

## 安全最佳实践

1. **绝不提交敏感配置到 Git**
   - `.env` 文件已加入 `.gitignore`
   - 使用 `application-example.yml` 作为模板

2. **定期轮换密钥**
   - JWT 密钥应定期更换（建议每 90 天）
   - 数据库密码应遵循组织安全策略

3. **使用密钥管理服务**
   - AWS Secrets Manager
   - Azure Key Vault
   - HashiCorp Vault

4. **生产环境检查清单**
   - [ ] JWT_SECRET 已替换为强随机密钥
   - [ ] 数据库密码已修改
   - [ ] Redis 已启用密码认证
   - [ ] 文件上传路径已配置为持久化存储
   - [ ] 环境变量已在部署平台配置

## 故障排查

### 应用启动失败

如果看到以下错误：

```
Caused by: java.lang.IllegalArgumentException: The specified key byte array is 64 bits which is not secure enough
```

**解决方法**: JWT_SECRET 长度不足，请使用上述方法生成至少 256 位的密钥。

### 数据库连接失败

检查：
1. `DB_URL` 格式是否正确
2. 数据库服务是否运行
3. `DB_USERNAME` 和 `DB_PASSWORD` 是否正确
4. 网络连接是否正常

## 开发环境快速启动

```bash
# 1. 复制示例配置
cp backend/src/main/resources/application-example.yml backend/src/main/resources/application-local.yml

# 2. 编辑配置（修改密码和密钥）
# 编辑 application-local.yml

# 3. 使用指定配置启动
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 相关文件

- `backend/src/main/resources/application.yml` - 主配置（使用环境变量）
- `backend/src/main/resources/application-example.yml` - 示例配置模板
- `.gitignore` - 忽略敏感配置文件
- `backend/.env` - 本地环境变量（不提交到 Git）
