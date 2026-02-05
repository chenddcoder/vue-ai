import { createRouter, createWebHistory } from 'vue-router'
import EditorLayout from '@/views/EditorLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/project/new'
    },
    {
      path: '/project/:id',
      name: 'editor',
      component: EditorLayout
    },
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/Login.vue')
    }
  ]
})

export default router
