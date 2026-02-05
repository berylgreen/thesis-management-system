# JDK 17 安装指南

**目标**: 安装 JDK 17 以执行后端测试
**预计时间**: 10-15 分钟
**当前系统**: Windows 10

---

## 📥 步骤 1: 下载 JDK 17

### 选项 A: Oracle JDK 17 (官方版本)

**下载地址**: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

**选择版本**:
- 操作系统: Windows
- 架构: x64
- 文件类型: Installer (`.exe`)
- 推荐文件: `jdk-17_windows-x64_bin.exe` (约 160 MB)

**注意**: 需要登录 Oracle 账号(免费注册)

---

### 选项 B: Adoptium (OpenJDK,推荐)

**下载地址**: https://adoptium.net/temurin/releases/?version=17

**步骤**:
1. 访问上述网址
2. 选择:
   - **Operating System**: Windows
   - **Architecture**: x64
   - **Package Type**: JDK
   - **Version**: 17 - LTS
3. 点击 `.msi` 文件下载 (约 180 MB)

**推荐理由**:
- ✅ 完全免费,无需注册
- ✅ 开源,社区支持
- ✅ 与 Oracle JDK 完全兼容

---

## 🔧 步骤 2: 安装 JDK 17

### 使用安装程序 (.exe 或 .msi)

1. **运行安装程序**
   - 双击下载的文件
   - 如果出现 UAC 提示,点击"是"

2. **选择安装路径** (重要!)
   - **推荐路径**: `C:\Program Files\Java\jdk-17`
   - **避免**: 包含中文或空格的路径
   - 记住这个路径,后面会用到

3. **安装选项**
   - 保持默认选择即可
   - 确保勾选 "Set JAVA_HOME variable" (如果有此选项)
   - 点击 "Next" → "Install"

4. **等待安装完成**
   - 通常需要 2-3 分钟
   - 完成后点击 "Close"

---

## 🌐 步骤 3: 配置环境变量

### 3.1 设置 JAVA_HOME

1. **打开系统环境变量设置**
   - 按 `Win + X`,选择"系统"
   - 点击"高级系统设置"
   - 点击"环境变量"按钮

   **或者使用命令**:
   - 按 `Win + R`
   - 输入: `sysdm.cpl`
   - 点击"高级"选项卡 → "环境变量"

2. **添加 JAVA_HOME**
   - 在"系统变量"区域,点击"新建"
   - 变量名: `JAVA_HOME`
   - 变量值: `C:\Program Files\Java\jdk-17` (您的实际安装路径)
   - 点击"确定"

### 3.2 更新 PATH

1. **编辑 PATH 变量**
   - 在"系统变量"中找到 `Path`
   - 选中后点击"编辑"

2. **添加 JDK bin 目录**
   - 点击"新建"
   - 输入: `%JAVA_HOME%\bin`
   - 使用"上移"按钮,将此项移动到列表顶部 (重要!)
   - 这样可以确保使用 JDK 17,而不是其他版本

3. **保存设置**
   - 点击"确定" → "确定" → "确定"
   - 关闭所有窗口

---

## ✅ 步骤 4: 验证安装

### 4.1 打开新的命令提示符

**重要**: 必须打开新的命令提示符窗口,旧窗口不会读取新的环境变量

1. 按 `Win + R`
2. 输入 `cmd`
3. 按回车

### 4.2 验证 Java 版本

在命令提示符中执行:

```cmd
java -version
```

**预期输出**:
```
java version "17.0.x" 2024-xx-xx LTS
Java(TM) SE Runtime Environment (build 17.0.x+xx-LTS-xxx)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.x+xx-LTS-xxx, mixed mode, sharing)
```

或者 (Adoptium):
```
openjdk version "17.0.x" 2024-xx-xx
OpenJDK Runtime Environment Temurin-17.0.x+x (build 17.0.x+x)
OpenJDK 64-Bit Server VM Temurin-17.0.x+x (build 17.0.x+x, mixed mode, sharing)
```

### 4.3 验证编译器版本

```cmd
javac -version
```

**预期输出**:
```
javac 17.0.x
```

### 4.4 验证 JAVA_HOME

```cmd
echo %JAVA_HOME%
```

**预期输出**:
```
C:\Program Files\Java\jdk-17
```

---

## 🚨 常见问题

### Q1: java -version 仍显示 Java 25

**原因**: PATH 中 Java 25 的路径在 JDK 17 之前

**解决**:
1. 重新打开环境变量设置
2. 编辑 PATH
3. 将 `%JAVA_HOME%\bin` 移动到最顶部
4. 删除或禁用 Java 25 的路径
5. 重启命令提示符

### Q2: 找不到 java 命令

**原因**: PATH 未正确设置

**解决**:
1. 检查 JAVA_HOME 是否正确设置
2. 检查 PATH 中是否有 `%JAVA_HOME%\bin`
3. 确保重启了命令提示符

### Q3: 安装后仍然编译失败

**解决**:
1. 确认 java -version 显示 17.x
2. 清理 Maven 缓存:
   ```cmd
   cd E:\7antigravity\test16\backend
   mvn clean
   ```
3. 重新编译:
   ```cmd
   mvn compile
   ```

---

## 🎯 步骤 5: 执行测试

安装并验证成功后,立即执行测试:

### 5.1 后端单元测试

```cmd
cd E:\7antigravity\test16\backend
mvn clean test -Dtest=ThesisServiceTest
```

**预期输出**:
```
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 5.2 后端集成测试

```cmd
mvn test -Dtest=DiffControllerTest
```

**预期输出**:
```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 5.3 生成覆盖率报告

```cmd
mvn jacoco:report
```

**查看报告**:
```cmd
start target\site\jacoco\index.html
```

---

## 📋 安装检查清单

完成安装后,请确认以下各项:

- [ ] JDK 17 已下载 (约 160-180 MB)
- [ ] JDK 17 已安装到 `C:\Program Files\Java\jdk-17`
- [ ] JAVA_HOME 环境变量已设置
- [ ] PATH 中已添加 `%JAVA_HOME%\bin`
- [ ] PATH 中 JDK 17 在最前面 (优先级最高)
- [ ] 新命令提示符中 `java -version` 显示 17.x
- [ ] `javac -version` 显示 17.x
- [ ] `echo %JAVA_HOME%` 显示正确路径
- [ ] Maven 编译成功 (`mvn compile`)
- [ ] 测试执行成功

---

## 🔄 如果需要卸载 Java 25

**可选**: 如果不再需要 Java 25,可以卸载以避免冲突

1. 按 `Win + R`
2. 输入 `appwiz.cpl`
3. 找到 "Java(TM) SE Development Kit 25.x.x"
4. 右键 → 卸载
5. 重启计算机

---

## 📞 需要帮助?

如果安装过程中遇到问题:

1. **检查下载的文件**
   - 确保下载完整,文件大小正确
   - 验证文件完整性 (右键 → 属性)

2. **重新安装**
   - 完全卸载 JDK 17
   - 删除安装目录
   - 重新下载并安装

3. **使用备选方案**
   - 如果环境变量配置困难,可使用 IntelliJ IDEA
   - IDE 会自动管理 JDK,无需手动配置

---

**下一步**: 安装完成后,请在命令提示符中运行:
```cmd
java -version
```
将输出结果告诉我,我会确认安装是否成功,然后继续执行测试!

---

**文档版本**: 1.0.0
**创建时间**: 2026-02-01
**适用系统**: Windows 10/11
