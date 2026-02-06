<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="header">
      <div class="header-left">
        <div class="logo" @click="goHome">Vue AI Platform</div>
        <a-menu theme="dark" mode="horizontal" :selectedKeys="[currentMenuKey]" :style="{ lineHeight: '64px' }">
          <a-menu-item key="editor" @click="goHome">编辑器</a-menu-item>
          <a-menu-item key="market" @click="goMarket">应用市场</a-menu-item>
          <a-menu-item key="my-apps" @click="goMyApps" v-if="userStore.isLoggedIn">我的应用</a-menu-item>
        </a-menu>
      </div>
      
      <div class="header-right">
        <a-dropdown v-if="userStore.currentUser">
          <a-button type="text" class="user-btn">
            <UserOutlined />
            {{ userStore.currentUser.username }}
            <span v-if="userStore.isGuest" class="guest-badge">游客</span>
            <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item v-if="userStore.isGuest" @click="goLogin">
                <LoginOutlined />
                登录/注册
              </a-menu-item>
              <a-menu-divider v-if="userStore.isGuest" />
              <a-menu-item @click="logout">
                <LogoutOutlined />
                退出
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else type="primary" @click="goLogin">登录</a-button>
      </div>
    </a-layout-header>

    <a-layout-content class="detail-content">
      <div class="back-nav">
        <a-button type="link" @click="goBack">
          <ArrowLeftOutlined />
          返回市场
        </a-button>
      </div>

      <a-row :gutter="[32, 32]">
        <a-col :xs="24" :lg="16">
          <div class="preview-section">
            <div class="preview-box">
              <div v-if="loading" class="loading-box">
                <a-spin size="large" tip="加载中..." />
              </div>
              <iframe 
                v-else
                ref="previewFrame"
                class="preview-frame"
                :srcdoc="previewHtml"
                sandbox="allow-scripts allow-same-origin"
              />
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="8">
          <div class="info-section">
            <div class="app-header">
              <h1>{{ appInfo.name }}</h1>
              <div class="app-stats">
                <a-space>
                  <span><EyeOutlined /> {{ appInfo.views }} 浏览</span>
                  <span><LikeOutlined /> {{ appInfo.likes }} 点赞</span>
                </a-space>
              </div>
            </div>

            <a-divider />

            <div class="app-desc">
              <h3>应用介绍</h3>
              <p>{{ appInfo.description }}</p>
            </div>

            <div class="app-tags" v-if="appInfo.tags && appInfo.tags.length">
              <h3>标签</h3>
              <div class="tags-list">
                <a-tag v-for="tag in appInfo.tags" :key="tag" color="blue">{{ tag }}</a-tag>
              </div>
            </div>

            <a-divider />

            <div class="author-info">
              <h3>作者</h3>
              <div class="author-card">
                <a-avatar :size="48" :src="appInfo.authorAvatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <div class="author-detail">
                  <div class="author-name">{{ appInfo.author }}</div>
                  <div class="author-date">发布于 {{ appInfo.publishDate }}</div>
                </div>
              </div>
            </div>

            <div class="action-buttons">
              <a-button 
                type="primary" 
                size="large" 
                block 
                @click="handleLike"
                :loading="likeLoading"
              >
                <LikeOutlined />
                {{ isLiked ? '已点赞' : '点赞' }}
              </a-button>
              <a-button 
                type="default" 
                size="large" 
                block 
                @click="useApp"
                style="margin-top: 12px"
              >
                <CopyOutlined />
                使用该模板
              </a-button>
              <a-button 
                v-if="canEdit"
                type="dashed" 
                size="large" 
                block 
                @click="editApp"
                style="margin-top: 12px"
                danger
              >
                <EditOutlined />
                编辑应用
              </a-button>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, 
  DownOutlined,
  LogoutOutlined,
  LoginOutlined,
  ArrowLeftOutlined,
  LikeOutlined,
  EyeOutlined,
  CopyOutlined,
  EditOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import { getMarketAppDetail, toggleAppLike } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const projectStore = useProjectStore()

const loading = ref(true)
const likeLoading = ref(false)
const isLiked = ref(false)
const previewFrame = ref<HTMLIFrameElement | null>(null)

const appInfo = ref<any>({
  id: 0,
  name: '',
  description: '',
  author: '',
  authorAvatar: '',
  publishDate: '',
  likes: 0,
  views: 0,
  tags: [],
  content: {}
})

const previewHtml = computed(() => {
  if (!appInfo.value.content || !appInfo.value.content['App.vue']) {
    return '<div style="display: flex; justify-content: center; align-items: center; height: 100vh; color: #999;">暂无预览</div>'
  }
  
  const appVue = appInfo.value.content['App.vue']
  // 提取template和script
  const templateMatch = appVue.match(/<template>([\s\S]*)<\/template>/)
  const scriptMatch = appVue.match(/<script setup>([\s\S]*)<\/script>/)
  const styleMatch = appVue.match(/<style>([\s\S]*)<\/style>/)
  
  const template = templateMatch ? templateMatch[1] : ''
  const script = scriptMatch ? scriptMatch[1] : ''
  const style = styleMatch ? styleMatch[1] : ''
  
  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <script src="https://unpkg.com/vue@3/dist/vue.global.js"><\/script>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        ${style}
      </style>
    </head>
    <body>
      <div id="app">${template}</div>
      <script>
        const { createApp, ref, reactive, computed, onMounted } = Vue;
        createApp({
          setup() {
            ${script}
            return {};
          }
        }).mount('#app');
      <\/script>
    </body>
    </html>
  `
})

const canEdit = computed(() => {
  return userStore.isLoggedIn && appInfo.value.authorId === userStore.currentUser?.id
})

const currentMenuKey = computed(() => 'market')

// 返回市场
const goBack = () => {
  router.push('/market')
}

// 跳转到首页
const goHome = () => {
  router.push('/project/new')
}

// 跳转到市场
const goMarket = () => {
  router.push('/market')
}

// 跳转到我的应用
const goMyApps = () => {
  router.push('/my-apps')
}

// 跳转到登录
const goLogin = () => {
  userStore.logout()
  router.push('/login')
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.push('/login')
  message.success('已退出登录')
}

// 点赞
const handleLike = async () => {
  if (userStore.isGuest) {
    message.warning('请先登录后再点赞')
    return
  }
  
  likeLoading.value = true
  try {
    const res: any = await toggleAppLike(Number(route.params.id), userStore.currentUser?.id)
    if (res.code === 200) {
      isLiked.value = res.data.liked
      appInfo.value.likes = res.data.likes
      message.success(isLiked.value ? '点赞成功！' : '已取消点赞')
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (error: any) {
    message.error('操作失败: ' + (error.message || '未知错误'))
  } finally {
    likeLoading.value = false
  }
}

// 使用该模板
const useApp = () => {
  if (appInfo.value.content) {
    projectStore.files = { ...appInfo.value.content }
    message.success('模板已加载到编辑器！')
    router.push('/project/new')
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value.content) {
    projectStore.files = { ...appInfo.value.content }
    router.push(`/project/${appInfo.value.projectId}`)
  }
}

// 加载应用详情
const loadAppDetail = async () => {
  loading.value = true
  try {
    const res: any = await getMarketAppDetail(Number(route.params.id))
    if (res.code === 200) {
      appInfo.value = {
        id: res.data.id,
        projectId: res.data.project_id,
        name: res.data.name,
        description: res.data.description,
        author: res.data.author_name,
        authorId: res.data.author_id,
        authorAvatar: res.data.author_avatar,
        publishDate: res.data.publish_time,
        likes: res.data.likes || 0,
        views: res.data.views || 0,
        tags: res.data.tags || [],
        content: res.data.content || {}
      }
    } else {
      message.error(res.message || '加载应用详情失败')
    }
  } catch (error: any) {
    message.error('加载应用详情失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAppDetail()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  color: white;
  font-size: 1.2rem;
  margin-right: 2rem;
  font-weight: 600;
  cursor: pointer;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-btn {
  color: white;
}

.guest-badge {
  background: #ff4d4f;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 4px;
}

.detail-content {
  background: #f0f2f5;
  padding: 24px 48px;
}

.back-nav {
  margin-bottom: 16px;
}

.preview-section {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.preview-box {
  height: 600px;
  background: #fafafa;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.loading-box {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.info-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  font-size: 1.8rem;
  margin-bottom: 8px;
}

.app-stats {
  color: #666;
  font-size: 14px;
}

.app-desc h3,
.app-tags h3,
.author-info h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #333;
}

.app-desc p {
  color: #666;
  line-height: 1.6;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.author-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.author-name {
  font-weight: 500;
  color: #333;
}

.author-date {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.action-buttons {
  margin-top: 24px;
}
</style>
