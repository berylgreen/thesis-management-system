// frontend/src/tests/router.spec.js
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
// createMemoryHistory is designed for non-browser environments like tests
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '../store/user'

// 模拟 Pinia store
vi.mock('../store/user', () => ({
  useUserStore: vi.fn(),
}));

// 在测试文件中重新定义路由，组件只是无操作的占位符
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: { render: () => {} }
  },
  {
    path: '/',
    component: { render: () => {} },
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: { render: () => {} },
        meta: { breadcrumb: ['控制台'] }
      },
      {
        path: 'theses',
        alias: 'thesis',
        name: 'Thesis',
        component: { render: () => {} },
        meta: { breadcrumb: ['论文管理', '论文列表'] }
      },
      {
        path: 'thesis/:id',
        name: 'ThesisDetail',
        component: { render: () => {} },
        meta: { breadcrumb: ['论文管理', '论文详情'] }
      }
    ]
  }
];

describe('Vue Router Navigation Logic', () => {
  let router;

  beforeEach(() => {
    setActivePinia(createPinia());
    router = createRouter({
      history: createMemoryHistory(),
      routes: routes,
    });

    // 在测试中直接应用 beforeEach 守卫逻辑
    router.beforeEach((to, from, next) => {
      const userStore = useUserStore();
      const isAuthenticated = !!userStore.token;

      if (to.meta.requiresAuth && !isAuthenticated) {
        next('/login');
      } else if (to.path === '/login' && isAuthenticated) {
        next('/');
      } else {
        next();
      }
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('未登录访问受保护的 /theses 页面应该被重定向到 /login', async () => {
    useUserStore.mockReturnValue({ token: null });
    
    // 不再需要 mount, 直接与 router 交互
    await router.push('/theses');
    await router.isReady();

    expect(router.currentRoute.value.path).toBe('/login');
  });

  it('已登录访问 /login 页面应该被重定向到 /', async () => {
    useUserStore.mockReturnValue({ token: 'fake-token-123' });
    
    await router.push('/');
    await router.isReady();

    await router.push('/login');
    await router.isReady();

    expect(router.currentRoute.value.path).toBe('/');
  });

  it('访问别名 /thesis 应该能正确解析路由', async () => {
    useUserStore.mockReturnValue({ token: 'fake-token-for-alias' });

    await router.push('/thesis');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('Thesis');
  });

  it('导航到 /theses 应该有正确的面包屑 (breadcrumb) meta 数据', async () => {
    useUserStore.mockReturnValue({ token: 'fake-token-for-meta' });

    await router.push('/theses');
    await router.isReady();

    expect(router.currentRoute.value.meta.breadcrumb).toEqual(['论文管理', '论文列表']);
  });

  it('从 /theses 导航到 /thesis/:id, 面包屑应该被正确更新', async () => {
    useUserStore.mockReturnValue({ token: 'fake-token-for-meta-update' });

    await router.push('/theses');
    await router.isReady();
    expect(router.currentRoute.value.meta.breadcrumb).toEqual(['论文管理', '论文列表']);

    await router.push('/thesis/123');
    await router.isReady();
    expect(router.currentRoute.value.meta.breadcrumb).toEqual(['论文管理', '论文详情']);
  });
});
