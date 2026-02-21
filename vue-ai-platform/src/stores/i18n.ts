import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useI18nStore = defineStore('i18n', () => {
  const locale = ref(localStorage.getItem('locale') || 'zh-CN')

  const messages = {
    'zh-CN': {
      app: {
        name: 'Vue AI Platform',
        editor: '编辑器',
        market: '应用市场',
        myApps: '我的应用',
        favorites: '我的收藏',
        aiConfig: 'AI配置',
        templates: '模板市场',
        snippets: '代码片段库',
        dashboard: '数据仪表盘',
        profile: '个人中心',
        logout: '退出',
        login: '登录'
      },
      editor: {
        save: '保存',
        publish: '发布',
        unsaved: '未保存',
        saved: '已保存'
      },
      market: {
        search: '搜索应用...',
        category: '分类',
        sort: '排序',
        latest: '最新',
        popular: '最热',
        trending: '趋势',
        recommend: '为你推荐',
        hot: '热门'
      },
      common: {
        confirm: '确认',
        cancel: '取消',
        delete: '删除',
        edit: '编辑',
        save: '保存',
        loading: '加载中...',
        noData: '暂无数据'
      }
    },
    'en-US': {
      app: {
        name: 'Vue AI Platform',
        editor: 'Editor',
        market: 'Market',
        myApps: 'My Apps',
        favorites: 'Favorites',
        aiConfig: 'AI Config',
        templates: 'Templates',
        snippets: 'Snippets',
        dashboard: 'Dashboard',
        profile: 'Profile',
        logout: 'Logout',
        login: 'Login'
      },
      editor: {
        save: 'Save',
        publish: 'Publish',
        unsaved: 'Unsaved',
        saved: 'Saved'
      },
      market: {
        search: 'Search apps...',
        category: 'Category',
        sort: 'Sort',
        latest: 'Latest',
        popular: 'Popular',
        trending: 'Trending',
        recommend: 'Recommended for You',
        hot: 'Hot'
      },
      common: {
        confirm: 'Confirm',
        cancel: 'Cancel',
        delete: 'Delete',
        edit: 'Edit',
        save: 'Save',
        loading: 'Loading...',
        noData: 'No Data'
      }
    }
  }

  const t = (key: string) => {
    const keys = key.split('.')
    let value: any = messages[locale.value as keyof typeof messages]
    
    for (const k of keys) {
      if (value && typeof value === 'object') {
        value = value[k]
      } else {
        return key
      }
    }
    
    return value || key
  }

  const setLocale = (newLocale: string) => {
    locale.value = newLocale
    localStorage.setItem('locale', newLocale)
  }

  const isZh = computed(() => locale.value === 'zh-CN')

  return {
    locale,
    t,
    setLocale,
    isZh
  }
})
