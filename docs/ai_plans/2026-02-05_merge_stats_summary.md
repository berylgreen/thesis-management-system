---
type: plan
created_at: 2026-02-05
status: in_progress
target: VersionComparer.vue
---

# 计划：合并统计摘要到完整内容页面

## 目标

将独立的“统计摘要”标签页内容合并到“完整内容”视图顶部，精简 UI 布局，直接展示核心数据。

## 影响范围

- `frontend/src/components/VersionComparer.vue`
- `frontend/src/tests/VersionComparer.spec.js`

## 步骤

1. **修改 UI 组件** (`VersionComparer.vue`)
    - 将统计数据的 `<el-row>` 移动到 `full-content` 标签页的顶部。
    - 删除 `summary` 标签页。
    - 将默认 `activeTab` 修改为 `'full-content'`。
2. **更新测试** (`VersionComparer.spec.js`)
    - 修正断言：桌面端标签页数量从 3 改为 2。

## 风险评估

- **低风险**：纯 UI 调整，不涉及后端逻辑。
- **注意**：需确认移动端适配（响应式布局）。

## 验证标准

- 运行单元测试 `npm run test:unit frontend/src/tests/VersionComparer.spec.js` 通过。
- 确认“完整内容”页顶部显示统计数据，且无单独的“统计摘要”页。
