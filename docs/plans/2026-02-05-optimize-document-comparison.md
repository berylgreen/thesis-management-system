# Optimize Document Comparison Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve document comparison accuracy and readability by refining block matching, increasing similarity thresholds, and applying semantic cleanup to diffs.

**Architecture:**
1. **Backend**: Modify `DiffUtil` to generate unique signatures for tables (row/column count + snippet) and images (content hash/length) instead of generic placeholders.
2. **Frontend**: Update `VersionComparer.vue` to increase the matching threshold (0.3 -> 0.6), implement `diff_cleanupSemantic` for human-readable highlighting, and optimize synchronization logic.

**Tech Stack:** Java (Apache POI, java-diff-utils), Vue 3 (diff-match-patch, Element Plus).

---

### Task 1: Backend - Enhance Block Signatures

**Files:**
- Modify: `backend/src/main/java/com/thesis/util/DiffUtil.java`
- Modify: `backend/src/main/java/com/thesis/service/ThesisService.java`

**Step 1: Update ContentBlock to include metadata/signature**
Add a `signature` field to the `ContentBlock` inner class in `DiffUtil.java`.

**Step 2: Implement signature generation in `readDocxAsContentBlocks`**
- For `TABLE`: Signature = "Table_" + rowCount + "x" + colCount + "_" + hash(content.substring(0, 50))
- For `IMAGE`: Signature = "Image_" + hash(contentBase64)
- For `TEXT`: Signature remains the text content.

**Step 3: Update `extractTextLines` to use signatures**
Instead of static `[表格]` or `[图片]`, use `block.getSignature()` for the diff-utils input.

**Step 4: Commit**
```bash
git add backend/src/main/java/com/thesis/util/DiffUtil.java
git commit -m "refactor(backend): add unique signatures to content blocks for better matching"
```

---

### Task 2: Frontend - Increase Matching Threshold & UI Polish

**Files:**
- Modify: `frontend/src/components/VersionComparer.vue`

**Step 1: Update similarity threshold**
Change `const threshold = 0.3` to `0.6` in `buildParagraphMatching` function to avoid false matches between unrelated short paragraphs.

**Step 2: Add Diff Cleanup Semantic**
In `getHighlightedText` and `renderDiffs`, call `dmp.diff_cleanupSemantic(diffs)` after generating diffs to merge fragmented changes into meaningful words/phrases.

**Step 3: Update CSS for clearer highlighting**
Refine `.diff-added` and `.diff-deleted` styles to be less jarring (e.g., using background-colors instead of just text colors).

**Step 4: Commit**
```bash
git add frontend/src/components/VersionComparer.vue
git commit -m "feat(frontend): improve matching accuracy and semantic highlighting"
```

---

### Task 3: Frontend - Optimize Scroll Sync & Navigation

**Files:**
- Modify: `frontend/src/components/VersionComparer.vue`

**Step 1: Implement Anchor-based Sync Scrolling**
Replace percentage-based scrolling with a logic that finds the nearest "Visible Block" in one side and scrolls the other side to its matched counterpart.

**Step 2: Add "Next/Prev Diff" Jump buttons**
Add floating navigation buttons to quickly jump to the next changed block.

**Step 3: Commit**
```bash
git add frontend/src/components/VersionComparer.vue
git commit -m "feat(frontend): implement anchor-based scroll sync and diff navigation"
```
