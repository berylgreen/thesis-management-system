---
created: 2026-02-01
target: 修正 Java 版本优先级问题 (Target 17 vs Actual 25)
risk_level: medium (Path 变量修改需谨慎)
---

# 目标

解决用户修改 `JAVA_HOME` 为 17 后，由于 Path 优先级问题导致 `java -version` 仍显示为 25 的情况。

# 根本原因分析

1. **Oracle 自动路径劫持**：Oracle 安装程序将 `C:\Program Files\Common Files\Oracle\Java\javapath` 置于 Path 最前端。
2. **会话缓存**：当前 PowerShell 会话可能仍加载旧环境变量（检测到 `JAVA_HOME=1.8`，而非用户声称的 17）。

# 修复步骤

1. **备份**：导出当前环境变量以防误删。
2. **清理干扰项**：从系统环境变量 Path 中移除或下移以下路径：
   - `C:\Program Files\Common Files\Oracle\Java\javapath`
   - `C:\ProgramData\Oracle\Java\javapath`
3. **置顶正确路径**：确保 `%JAVA_HOME%\bin` 位于 Path 列表的最顶部。
4. **验证**：重启终端，运行 `java -version` 确认。

# 验证标准

- `java -version` 输出包含 "17"。
- `echo %JAVA_HOME%` 输出指向 JDK 17 安装目录。
