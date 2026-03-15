# Modern Navigation Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refactor the global navigation into a modern "Sidebar + Top Header" framework to improve multitasking and navigation efficiency.

**Architecture:**
1. **Layout Store**: A Pinia store to manage global UI state (sidebar collapse, recent thesis list).
2. **Component Framework**: A `MainLayout.vue` container with nested routing, hosting a dark sidebar and a light header.
3. **Router Update**: Restructuring routes to wrap authenticated pages under the new layout.

**Tech Stack:** Vue 3, Pinia, Vue Router, Element Plus.

---

### Task 0: Backend - Add Get Thesis by ID API

**Files:**
- Modify: `backend/src/main/java/com/thesis/service/ThesisService.java`
- Modify: `backend/src/main/java/com/thesis/controller/ThesisController.java`
- Modify: `frontend/src/api/thesis.js`

**Step 1: Add method in `ThesisService`**
```java
public Thesis getThesisById(Long id) {
    return thesisMapper.selectById(id);
}
```

**Step 2: Add GET endpoint in `ThesisController`**
```java
@GetMapping("/{thesisId}")
public Result<Thesis> getThesis(@PathVariable Long thesisId) {
    try {
        Thesis thesis = thesisService.getThesisById(thesisId);
        return Result.success(thesis);
    } catch (Exception e) {
        return Result.error(e.getMessage());
    }
}
```

**Step 3: Add frontend API function**
```javascript
export function getThesis(id) {
  return request({
    url: `/thesis/${id}`,
    method: 'get'
  })
}
```

**Step 4: Commit**
```bash
git add backend/src/main/java/com/thesis/service/ThesisService.java backend/src/main/java/com/thesis/controller/ThesisController.java frontend/src/api/thesis.js
git commit -m "feat(api): add get thesis by id endpoint"
```

### Task 1: Setup Layout State Management

**Files:**
- Create: `frontend/src/store/layout.js`

**Step 1: Create Pinia store for layout**

```javascript
import { defineStore } from 'pinia'

export const useLayoutStore = defineStore('layout', {
  state: () => ({
    isSidebarCollapsed: false,
    recentTheses: JSON.parse(localStorage.getItem('recentTheses') || '[]')
  }),
  actions: {
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed
    },
    addRecentThesis(thesis) {
      const existingIndex = this.recentTheses.findIndex(t => t.id === thesis.id)
      if (existingIndex !== -1) {
        this.recentTheses.splice(existingIndex, 1)
      }
      this.recentTheses.unshift({ id: thesis.id, title: thesis.title })
      if (this.recentTheses.length > 3) {
        this.recentTheses.pop()
      }
      localStorage.setItem('recentTheses', JSON.stringify(this.recentTheses))
    }
  }
})
```

**Step 2: Commit**

```bash
git add frontend/src/store/layout.js
git commit -m "feat(store): add layout store for sidebar and recent items"
```

---

### Task 2: Create Layout Components

**Files:**
- Create: `frontend/src/layouts/MainLayout.vue`

**Step 1: Implement MainLayout with Sidebar and Header**
Use `el-container`, `el-aside`, `el-header`, and `el-main`.

**Step 2: Implement Sidebar logic**
Include menu items with icons and the "Recent Items" list.

**Step 3: Implement Header logic**
Include breadcrumbs (computed from route meta), global search placeholder, and user profile.

**Step 4: Commit**

```bash
git add frontend/src/layouts/MainLayout.vue
git commit -m "feat(layout): implement modern MainLayout with sidebar and header"
```

---

### Task 3: Update Router and Navigation Guard

**Files:**
- Modify: `frontend/src/router/index.js`

**Step 1: Update routes to use nested children**
Wrap `Home`, `ThesisList`, and `ThesisDetail` under `MainLayout`.

**Step 2: Add breadcrumb metadata to routes**
```javascript
{
  path: 'theses',
  name: 'Thesis',
  component: () => import('../views/ThesisList.vue'),
  meta: { breadcrumb: ['论文管理', '论文列表'] }
}
```

**Step 3: Commit**

```bash
git add frontend/src/router/index.js
git commit -m "refactor(router): switch to nested routes with MainLayout"
```

---

### Task 4: Integrate "Recent Items" Tracking

**Files:**
- Modify: `frontend/src/views/ThesisDetail.vue`

**Step 1: Record visit on mount**
```javascript
import { useLayoutStore } from '../store/layout'
const layoutStore = useLayoutStore()
// ... inside onMounted after loading thesis data
layoutStore.addRecentThesis({ id: thesisId, title: thesisTitle })
```

**Step 2: Commit**

```bash
git add frontend/src/views/ThesisDetail.vue
git commit -m "feat(navigation): track recent thesis visits"
```
