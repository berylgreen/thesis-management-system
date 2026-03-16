import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import MainLayout from '../layouts/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { breadcrumb: [{ label: '控制台' }] }
      },
      {
        path: 'theses',
        alias: 'thesis',
        name: 'Thesis',
        component: () => import('../views/ThesisList.vue'),
        meta: { breadcrumb: [{ label: '论文管理', to: '/theses' }, { label: '论文列表' }] }
      },
      {
        path: 'thesis/:id',
        name: 'ThesisDetail',
        component: () => import('../views/ThesisDetail.vue'),
        meta: { breadcrumb: [{ label: '论文管理', to: '/theses' }, { label: '论文详情' }] }
      },
      {
        path: 'students',
        name: 'StudentList',
        component: () => import('../views/StudentList.vue'),
        meta: { breadcrumb: [{ label: '学生管理', to: '/students' }, { label: '学生列表' }] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isAuthenticated = !!userStore.token

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
