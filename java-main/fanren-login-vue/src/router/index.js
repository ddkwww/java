import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import RegisterView from '../views/RegisterView.vue'
import SutraDetailView from '../views/SutraDetailView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true } },
  { path: '/manual/:id', name: 'manual', component: SutraDetailView, meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: RegisterView },
  { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
  { path: '/admin', name: 'admin', component: AdminDashboardView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 未登录拦截；已登录访问登录页则跳首页/后台
router.beforeEach((to) => {
  const token = localStorage.getItem('fx_token')
  if (to.meta.requiresAuth && !token) {
    // 后台管理相关路由未登录 → 跳后台登录页；其余 → 普通登录页
    const target = to.path.startsWith('/admin') ? 'admin-login' : 'login'
    return { name: target, query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && token) return { name: 'home' }
  if (to.name === 'admin-login' && token) return { name: 'admin' }
})

export default router
