import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

type Theme = 'light' | 'dark' | 'auto'

export const useThemeStore = defineStore('theme', () => {
  // 从localStorage读取保存的主题设置
  const savedTheme = localStorage.getItem('app-theme') as Theme || 'auto'
  const theme = ref<Theme>(savedTheme)
  const isDark = ref(false)

  // 检测系统主题偏好
  const getSystemTheme = (): boolean => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  // 更新主题
  const updateTheme = () => {
    if (theme.value === 'auto') {
      isDark.value = getSystemTheme()
    } else {
      isDark.value = theme.value === 'dark'
    }
    
    // 应用到document
    if (isDark.value) {
      document.documentElement.classList.add('dark-theme')
      document.documentElement.classList.remove('light-theme')
    } else {
      document.documentElement.classList.add('light-theme')
      document.documentElement.classList.remove('dark-theme')
    }
  }

  // 设置主题
  const setTheme = (newTheme: Theme) => {
    theme.value = newTheme
    localStorage.setItem('app-theme', newTheme)
    updateTheme()
  }

  // 初始化
  updateTheme()

  // 监听系统主题变化
  if (window.matchMedia) {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', () => {
      if (theme.value === 'auto') {
        updateTheme()
      }
    })
  }

  // 监听theme变化
  watch(theme, updateTheme)

  return {
    theme,
    isDark,
    setTheme,
    updateTheme
  }
})
