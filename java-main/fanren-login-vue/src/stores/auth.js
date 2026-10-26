import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  login as loginApi,
  adminLogin as adminLoginApi,
  register as registerApi,
  getProfile as getProfileApi
} from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('fx_token') || '')
  const user = ref(localStorage.getItem('fx_user') || '')
  // 权限：0=管理员，1=默认(普通用户)；未登录时默认 1（看不到后台入口）
  const permission = ref(localStorage.getItem('fx_permission') != null ? Number(localStorage.getItem('fx_permission')) : 1)
  const profile = ref(null)

  async function login(username, password, { remember = true } = {}) {
    const data = await loginApi(username, password)
    token.value = data.token
    user.value = data.username || username
    const perm = (data.permission != null) ? Number(data.permission) : 1
    permission.value = perm
    localStorage.setItem('fx_token', data.token)
    localStorage.setItem('fx_permission', String(perm))
    if (remember) localStorage.setItem('fx_user', username)
    else localStorage.removeItem('fx_user')
    return data
  }

  // 后台管理员登录：仅管理员(permission=0)可成功
  async function adminLogin(username, password, { remember = true } = {}) {
    const data = await adminLoginApi(username, password)
    token.value = data.token
    user.value = data.username || username
    const perm = (data.permission != null) ? Number(data.permission) : 0
    permission.value = perm
    localStorage.setItem('fx_token', data.token)
    localStorage.setItem('fx_permission', String(perm))
    if (remember) localStorage.setItem('fx_user', username)
    else localStorage.removeItem('fx_user')
    return data
  }

  async function register(username, password, captchaId, captcha, profile = {}) {
    return registerApi(username, password, captchaId, captcha, profile)
  }

  async function fetchProfile() {
    const data = await getProfileApi()
    profile.value = data
    if (data.username) user.value = data.username
    if (data.permission != null) {
      const perm = Number(data.permission)
      permission.value = perm
      localStorage.setItem('fx_permission', String(perm))
    }
    return data
  }

  function logout() {
    token.value = ''
    user.value = ''
    profile.value = null
    permission.value = 1
    localStorage.removeItem('fx_token')
    localStorage.removeItem('fx_user')
    localStorage.removeItem('fx_permission')
  }

  return { token, user, permission, profile, login, adminLogin, register, fetchProfile, logout }
})
