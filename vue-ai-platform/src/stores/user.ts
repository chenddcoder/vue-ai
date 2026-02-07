import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface User {
  id: number
  username: string
  avatar?: string
}

const USER_STORAGE_KEY = 'vueai_user_info'

function getStoredUser(): User | null {
  try {
    const stored = localStorage.getItem(USER_STORAGE_KEY)
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<User | null>(getStoredUser())
  const isGuest = ref<boolean>(localStorage.getItem('isGuest') === 'true')

  const isLoggedIn = computed(() => !!token.value && !isGuest.value)
  const currentUser = computed(() => user.value)
  const userInfo = computed(() => user.value)

  // 设置token并保存到localStorage
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    isGuest.value = false
    localStorage.removeItem('isGuest')
  }

  // 设置用户信息
  function setUser(userInfo: User) {
    user.value = userInfo
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userInfo))
  }

  // 设置游客模式
  function setGuestMode() {
    isGuest.value = true
    localStorage.setItem('isGuest', 'true')
    token.value = 'guest-token'
    localStorage.setItem('token', 'guest-token')
    user.value = { id: 0, username: '游客用户' }
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user.value))
  }

  // 退出登录
  function logout() {
    token.value = null
    user.value = null
    isGuest.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('isGuest')
    localStorage.removeItem(USER_STORAGE_KEY)
  }

  // 清除游客模式
  function clearGuestMode() {
    isGuest.value = false
    localStorage.removeItem('isGuest')
  }

  return {
    token,
    user,
    isGuest,
    isLoggedIn,
    currentUser,
    userInfo,
    setToken,
    setUser,
    setGuestMode,
    logout,
    clearGuestMode
  }
})
