---
created: 2026-02-05
type: plan
---

# 2026-02-05 文件归整计划

## 1. 目标

将 `source/sourseFilis` 目录下的新原始文件，依据 `source/stu.md` 中的学生名单，自动归整到 `uploads` 目录下的对应学生文件夹中。

## 2. 影响范围

- **源目录**: `e:\7antigravity\test16\source\sourseFilis` (将被读取并移动)
- **目标目录**: `e:\7antigravity\test16\uploads` (将新增文件)

## 3. 执行步骤

1. 执行脚本: `python scripts/organize_files.py`
2. 脚本逻辑:
   - 读取 `stu.md` 建立名册。
   - 扫描 `source/sourseFilis`。
   - 匹配文件名中的学生姓名。
   - 格式化重命名并移动到 `uploads/{学号}/`。

## 4. 风险评估

- **文件名识别失败**: 如果文件名中不包含学生姓名，脚本(line 120)将跳过该文件并打印 SKIP。
- **文件重名**: 脚本(line 159)包含自动重命名逻辑 (base_1, base_2)，风险较低。

## 5. 验证标准

1. `source/sourseFilis` 目录下的目标文件被清空（或移动）。
2. `uploads` 目录下对应学生的文件夹中出现新文件。
3. 新文件名符合 `{姓名}{学号}_{题目}_{日期}.{ext}` 格式。
