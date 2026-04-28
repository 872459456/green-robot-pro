import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/iterations',
    name: 'Iterations',
    component: () => import('../views/Iterations.vue')
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('../views/Monitor.vue')
  },
  {
    path: '/leaves',
    name: 'Leaves',
    component: () => import('../views/Leaves.vue')
  },
  {
    path: '/leaves/:leafId',
    name: 'LeafDetail',
    component: () => import('../views/LeafDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
