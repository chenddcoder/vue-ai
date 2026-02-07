import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { message } from 'ant-design-vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/project/new'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true }
    },
    {
      path: '/project/:id',
      name: 'editor',
      component: () => import('@/views/EditorLayout.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/market',
      name: 'market',
      component: () => import('@/views/Market.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/market/app/:id',
      name: 'marketApp',
      component: () => import('@/views/MarketAppDetail.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/my-apps',
      name: 'myApps',
      component: () => import('@/views/MyApps.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ai-config',
      name: 'aiConfig',
      component: () => import('@/views/AIConfig.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }
  
  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    message.warning('请先登录')
    next('/login')
    return
  }
  
  // 其他页面允许访问（包括游客）
  next()
})

export default router
