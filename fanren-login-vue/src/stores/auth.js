import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  login as loginApi,
  register as registerApi,
  getProfile as getProfileApi
} from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('fx_token') || '')
  const user = ref(localStorage.getItem('fx_user') || '')
  const profile = ref(null)

  async function login(username, password, { remember = true } = {}) {
    const data = await loginApi(username, password)
    token.value = data.token
    user.value = data.username || username
    localStorage.setItem('fx_token', data.token)
    if (remember) localStorage.setItem('fx_user', username)
    else localStorage.removeItem('fx_user')
    return data
  }

  async function register(username, password) {
    return registerApi(username, password)
  }

  async function fetchProfile() {
    const data = await getProfileApi()
    profile.value = data
    if (data.username) user.value = data.username
    return data
  }

  function logout() {
    token.value = ''
    user.value = ''
    profile.value = null
    localStorage.removeItem('fx_token')
    localStorage.removeItem('fx_user')
  }

  return { token, user, profile, login, register, fetchProfile, logout }
})
