<template>
  <div class="market-app-detail">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <div class="header-left">
          <a-button type="text" @click="goBack" class="back-btn">
            <LeftOutlined /> 返回
          </a-button>
          <div class="logo">Vue AI Platform</div>
        </div>
        <div class="header-right">
          <a-button type="primary" @click="launchApp">
            <PlayCircleOutlined /> 启动应用
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content class="content" v-if="app">
        <div class="app-main">
          <div class="app-preview">
            <iframe
              ref="previewFrame"
              class="preview-frame"
              :srcdoc="previewHtml"
              sandbox="allow-scripts allow-same-origin"
            />
          </div>
          
          <div class="app-sidebar">
            <div class="app-card">
              <div class="app-icon">
                <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
                <div v-else class="default-icon">
                  <AppstoreOutlined />
                </div>
              </div>
              <h2>{{ app.name }}</h2>
              <p class="author">
                <UserOutlined /> {{ app.author_name }}
              </p>
              <div class="tags" v-if="app.tags && app.tags.length">
                <a-tag v-for="tag in app.tags" :key="tag">{{ tag }}</a-tag>
              </div>
              <div class="stats">
                <span><EyeOutlined /> {{ app.views || 0 }}</span>
                <span><LikeOutlined /> {{ app.likes || 0 }}</span>
                <span><StarOutlined /> {{ avgRating?.avg_rating?.toFixed(1) || '暂无评分' }}</span>
              </div>
              <a-button type="primary" block @click="launchApp">
                <PlayCircleOutlined /> 立即体验
              </a-button>
              <a-space style="width: 100%; margin-top: 12px;">
                <a-button block @click="toggleFavorite" :class="{ favorited: isFavorited }">
                  <StarOutlined :style="isFavorited ? { color: '#faad14' } : {}" />
                  {{ isFavorited ? '已收藏' : '收藏' }}
                </a-button>
                <a-button block @click="toggleLike">
                  <LikeOutlined :style="isLiked ? { color: '#1890ff' } : {}" />
                  {{ isLiked ? '已赞' : '点赞' }}
                </a-button>
              </a-space>
            </div>

            <a-divider>应用介绍</a-divider>
            
            <div class="app-description">
              <p>{{ app.description }}</p>
            </div>

            <a-divider>评论 ({{ comments.length }})</a-divider>
            
            <div class="comment-form">
              <a-rate v-model:value="newComment.rating" allow-half />
              <a-textarea
                v-model:value="newComment.content"
                placeholder="写下你的评论..."
                :rows="3"
              />
              <a-button type="primary" @click="submitComment" :loading="submitting">
                发布评论
              </a-button>
            </div>

            <div class="comments-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-header">
                  <a-avatar :src="comment.user_avatar" :size="36">
                    {{ comment.user_name?.charAt(0) }}
                  </a-avatar>
                  <div class="comment-meta">
                    <span class="username">{{ comment.user_name }}</span>
                    <span class="time">{{ formatTime(comment.create_time) }}</span>
                  </div>
                  <a-rate :value="comment.rating" readonly allow-half size="small" />
                </div>
                <div class="comment-content">{{ comment.content }}</div>
              </div>
              <a-empty v-if="comments.length === 0" description="暂无评论" />
            </div>
          </div>
        </div>
      </a-layout-content>

      <a-spin v-else class="loading" />
    </a-layout>

    <TransitionGroup name="window-slide" tag="div" class="windows-container">
      <div
        v-for="win in windows"
        :key="win.id"
        v-show="!win.minimized"
        class="app-window"
        :class="{ 'is-fullscreen': win.isFullscreen }"
        :style="win.isFullscreen ? {} : win.position"
      >
        <div class="app-window-header" @mousedown="startDragHeader($event, win)">
          <div class="window-controls">
            <span class="control close" @click.stop="closeWindow(win)"></span>
            <span class="control minimize" @click.stop="minimizeWindow(win)"></span>
            <span class="control maximize" @click.stop="toggleFullscreen(win)"></span>
          </div>
          <div class="window-title">{{ win.name }}</div>
        </div>
        <div class="app-window-content">
          <iframe class="app-frame" :srcdoc="win.html" sandbox="allow-scripts allow-same-origin" />
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  LeftOutlined,
  PlayCircleOutlined,
  UserOutlined,
  EyeOutlined,
  LikeOutlined,
  StarOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'
import {
  getMarketAppDetail,
  getAppComments,
  addAppComment,
  toggleAppFavorite,
  checkAppFavorite,
  toggleAppLike
} from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const app = ref<any>(null)
const comments = ref<any[]>([])
const avgRating = ref<any>(null)
const isFavorited = ref(false)
const isLiked = ref(false)
const submitting = ref(false)
const windows = ref<any[]>([])
const previewHtml = ref('')

const newComment = ref({ content: '', rating: 5 })
let zIndexCounter = 1000

const goBack = () => router.back()

const loadData = async () => {
  const appId = parseInt(route.params.id as string)
  if (!appId) return

  try {
    const res: any = await getMarketAppDetail(appId)
    if (res.code === 200) {
      app.value = res.data
      previewHtml.value = generatePreviewHtml(res.data.content)
    }
  } catch (error) {
    message.error('加载应用失败')
  }

  try {
    const commentsRes: any = await getAppComments(appId)
    if (commentsRes.code === 200) {
      comments.value = commentsRes.data || []
      avgRating.value = commentsRes.stats
    }
  } catch (error) {
    console.error('加载评论失败', error)
  }

  if (userStore.currentUser?.id) {
    try {
      const favRes: any = await checkAppFavorite(appId, userStore.currentUser.id)
      if (favRes.code === 200) isFavorited.value = favRes.data?.favorited
    } catch (error) {
      console.error('检查收藏状态失败', error)
    }
  }
}

const generatePreviewHtml = (content: any) => {
  if (!content) return '<div style="padding: 40px; text-align: center; color: #999;">暂无内容</div>'
  const filesJson = JSON.stringify(content)
  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>${app.value?.name || '预览'}</title>
      <script src="https://unpkg.com/vue@3/dist/vue.global.js"><\/script>
      <script src="https://unpkg.com/vue-router@4/dist/vue-router.global.js"><\/script>
      <script src="https://unpkg.com/dayjs/dayjs.min.js"><\/script>
      <script src="https://unpkg.com/ant-design-vue@4/dist/antd.min.js"><\/script>
      <link rel="stylesheet" href="https://unpkg.com/ant-design-vue@4/dist/reset.css">
      <script src="https://unpkg.com/pinia@2/dist/pinia.global.js"><\/script>
      <script src="https://cdn.jsdelivr.net/npm/vue3-sfc-loader/dist/vue3-sfc-loader.js"><\/script>
      <style>body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }</style>
    </head>
    <body>
      <div id="app"></div>
      <script>
        const files = JSON.parse(decodeURIComponent("${encodeURIComponent(filesJson)}"));
        const { loadModule } = window['vue3-sfc-loader'];
        const options = {
          moduleCache: {
            vue: window.Vue, 'vue-router': window.VueRouter, 'pinia': window.Pinia, 'ant-design-vue': window.antd
          },
          async getFile(url) {
            const key = Object.keys(files).find(k => k.endsWith(url.replace(/^\.?\//, '')));
            return key ? files[key] : null;
          },
          addStyle(textContent) {
            const style = document.createElement('style');
            style.textContent = textContent;
            document.head.appendChild(style);
          },
        };
        loadModule('/App.vue', options).then(App => {
          const appInstance = window.Vue.createApp(App);
          if (window.antd) appInstance.use(window.antd);
          if (window.Pinia) appInstance.use(window.Pinia.createPinia());
          appInstance.mount('#app');
        });
      <\/script>
    </body>
    </html>
  `
}

const toggleFavorite = async () => {
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  const res: any = await toggleAppFavorite(app.value.id, userStore.currentUser.id)
  if (res.code === 200) {
    isFavorited.value = res.data?.favorited
    message.success(res.data?.message)
  }
}

const toggleLike = async () => {
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  const res: any = await toggleAppLike(app.value.id, userStore.currentUser.id)
  if (res.code === 200) {
    isLiked.value = res.data?.liked
    if (app.value) app.value.likes = res.data?.likes
  }
}

const submitComment = async () => {
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  if (!newComment.value.content.trim()) {
    message.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    const res: any = await addAppComment({
      appId: app.value.id,
      userId: userStore.currentUser.id,
      userName: userStore.currentUser.username,
      userAvatar: userStore.currentUser.avatar,
      content: newComment.value.content,
      rating: newComment.value.rating
    })
    if (res.code === 200) {
      message.success('评论成功')
      newComment.value.content = ''
      newComment.value.rating = 5
      loadData()
    }
  } finally {
    submitting.value = false
  }
}

const launchApp = () => {
  if (!app.value) return
  const offset = windows.value.length % 5
  const win = {
    id: app.value.id,
    name: app.value.name,
    content: app.value.content,
    thumbnail: app.value.thumbnail,
    html: generatePreviewHtml(app.value.content),
    minimized: false,
    isFullscreen: false,
    position: {
      top: `${80 + offset * 30}px`,
      left: `${100 + offset * 40}px`,
      width: '90vw',
      height: '85vh',
      zIndex: ++zIndexCounter
    }
  }
  windows.value.push(win)
  nextTick(() => {
    win.position.zIndex = ++zIndexCounter
  })
}

const closeWindow = (win: any) => {
  windows.value = windows.value.filter(w => w.id !== win.id)
}

const minimizeWindow = (win: any) => {
  win.minimized = true
}

const toggleFullscreen = (win: any) => {
  win.isFullscreen = !win.isFullscreen
}

let isDragging = false
let dragWin: any = null
let dragOffset = { x: 0, y: 0 }

const startDragHeader = (e: MouseEvent, win: any) => {
  if (win.isFullscreen) return
  isDragging = true
  dragWin = win
  dragOffset = { x: e.clientX - parseInt(win.position.left), y: e.clientY - parseInt(win.position.top) }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging || !dragWin) return
  dragWin.position = {
    ...dragWin.position,
    top: `${Math.max(64, e.clientY - dragOffset.y)}px`,
    left: `${Math.max(0, e.clientX - dragOffset.x)}px`
  }
}

const stopDrag = () => {
  isDragging = false
  dragWin = null
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const formatTime = (time: string) => time ? new Date(time).toLocaleDateString('zh-CN') : ''

onMounted(loadData)
</script>

<style scoped>
.market-app-detail { min-height: 100vh; background: #f0f2f5; }
.header { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; background: #001529; }
.header-left { display: flex; align-items: center; gap: 16px; }
.back-btn { color: white; }
.logo { color: white; font-size: 18px; font-weight: 600; }
.content { padding: 24px; }
.app-main { display: flex; gap: 24px; max-width: 1400px; margin: 0 auto; }
.app-preview { flex: 1; min-width: 0; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.preview-frame { width: 100%; height: 600px; border: none; }
.app-sidebar { width: 360px; flex-shrink: 0; }
.app-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.app-icon { width: 80px; height: 80px; border-radius: 16px; overflow: hidden; margin: 0 auto 12px; }
.app-icon img { width: 100%; height: 100%; object-fit: cover; }
.default-icon { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); font-size: 36px; color: white; }
.app-card h2 { text-align: center; margin-bottom: 8px; }
.app-card .author { text-align: center; color: #666; margin-bottom: 12px; }
.tags { display: flex; justify-content: center; gap: 4px; margin-bottom: 12px; }
.stats { display: flex; justify-content: center; gap: 16px; color: #666; margin-bottom: 16px; }
.stats span { display: flex; align-items: center; gap: 4px; }
.favorited { border-color: #faad14; color: #faad14; }
.app-description { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.comment-form { margin-bottom: 20px; }
.comment-form .ant-rate { margin-bottom: 8px; }
.comment-form .ant-btn { margin-top: 8px; }
.comments-list { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.comment-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.comment-item:last-child { border-bottom: none; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-meta { flex: 1; }
.username { font-weight: 500; margin-right: 8px; }
.time { color: #999; font-size: 12px; }
.comment-content { color: #333; line-height: 1.5; }
.loading { display: flex; justify-content: center; align-items: center; height: 400px; }

.windows-container { position: fixed; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; z-index: 1000; }
.app-window { position: absolute; width: 90vw; height: 85vh; background: white; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.25); display: flex; flex-direction: column; overflow: hidden; pointer-events: auto; }
.app-window.is-fullscreen { top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; border-radius: 0; }
.app-window-header { display: flex; align-items: center; padding: 12px 16px; background: linear-gradient(180deg, #f8f8f8 0%, #e8e8e8 100%); border-bottom: 1px solid #d9d9d9; cursor: move; user-select: none; }
.window-controls { display: flex; gap: 8px; }
.control { width: 12px; height: 12px; border-radius: 50%; cursor: pointer; }
.control.close { background: #ff5f57; }
.control.minimize { background: #febc2e; }
.control.maximize { background: #28c840; }
.window-title { flex: 1; text-align: center; font-size: 14px; color: #666; }
.app-window-content { flex: 1; overflow: hidden; }
.app-frame { width: 100%; height: 100%; border: none; }
.window-slide-enter-active, .window-slide-leave-active { transition: all 0.3s ease; }
.window-slide-enter-from { opacity: 0; transform: translateY(20px); }
.window-slide-leave-to { opacity: 0; transform: scale(0.9); }
</style>
