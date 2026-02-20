<template>
  <div class="my-favorites-page">
    <a-layout style="min-height: 100vh">
      <AppHeader />
      
      <a-layout-content class="content">
        <div class="page-header">
          <h1>我的收藏</h1>
          <p>收藏的应用会显示在这里</p>
        </div>

        <a-spin :spinning="loading">
          <div class="apps-grid" v-if="favorites.length > 0">
            <div
              v-for="app in favorites"
              :key="app.id"
              class="app-item"
              @click="launchApp(app)"
              @mouseenter="hoveredAppId = app.id"
              @mouseleave="hoveredAppId = null"
            >
              <div class="app-icon-wrapper" :class="{ 'is-hovering': hoveredAppId === app.id }">
                <div class="app-icon">
                  <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.app_name" />
                  <div v-else class="default-icon">
                    <AppstoreOutlined />
                  </div>
                </div>
                <div class="app-info">
                  <div class="app-name">{{ app.app_name }}</div>
                  <div class="app-author">{{ app.author_name }}</div>
                  <div class="app-stats">
                    <span><EyeOutlined /> {{ app.views || 0 }}</span>
                    <span><LikeOutlined /> {{ app.likes || 0 }}</span>
                  </div>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click.stop="removeFavorite(app.id)"
                  >
                    取消收藏
                  </a-button>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else-if="!loading" class="empty-state">
            <a-empty description="暂无收藏的应用">
              <template #image>
                <StarOutlined style="font-size: 64px; color: #faad14" />
              </template>
              <a-button type="primary" @click="goMarket">去应用市场看看</a-button>
            </a-empty>
          </div>
        </a-spin>
      </a-layout-content>
    </a-layout>

    <AppDetailModal
      v-model:visible="detailModalVisible"
      :appId="selectedAppId"
      @launch="launchApp"
    />

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
          <div class="window-actions">
            <a-button type="text" size="small" @click.stop="toggleFullscreen(win)">
              <FullscreenOutlined v-if="!win.isFullscreen" />
              <FullscreenExitOutlined v-else />
            </a-button>
          </div>
        </div>
        <div class="app-window-content">
          <iframe
            class="app-frame"
            :srcdoc="win.html"
            sandbox="allow-scripts allow-same-origin"
          />
        </div>
      </div>
    </TransitionGroup>

    <div class="dock-bar" v-if="windows.length > 0">
      <div class="dock-container">
        <div
          v-for="win in windows"
          :key="win.id"
          class="dock-item"
          :class="{ 'is-active': !win.minimized, 'is-hovering': hoveredDockId === win.id }"
          @click="handleDockClick(win)"
        >
          <div class="dock-icon">
            <img v-if="win.thumbnail" :src="win.thumbnail" :alt="win.name" />
            <div v-else class="dock-default-icon">
              <AppstoreOutlined />
            </div>
          </div>
          <div class="dock-indicator" v-if="!win.minimized"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import AppHeader from '@/components/AppHeader.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useUserStore } from '@/stores/user'
import { getUserFavorites, toggleAppFavorite, getMarketAppDetail } from '@/api'
import { AppDetailModal } from '@/components/community'

interface WindowState {
  id: number
  name: string
  content: any
  thumbnail?: string
  html: string
  minimized: boolean
  isFullscreen: boolean
  position: { top: string; left: string; width: string; height: string; zIndex: number }
}

interface FavoriteApp {
  id: number
  app_id: number
  app_name: string
  description: string
  thumbnail?: string
  author_name: string
  likes?: number
  views?: number
}

const router = useRouter()
const userStore = useUserStore()

const favorites = ref<FavoriteApp[]>([])
const loading = ref(false)
const hoveredAppId = ref<number | null>(null)
const hoveredDockId = ref<number | null>(null)
const windows = ref<WindowState[]>([])
const detailModalVisible = ref(false)
const selectedAppId = ref<number | undefined>(undefined)

let zIndexCounter = 1000

const goLogin = () => {
  userStore.logout()
  router.push('/login')
}

const loadFavorites = async () => {
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    router.push('/login')
    return
  }
  
  loading.value = true
  try {
    const res: any = await getUserFavorites(userStore.currentUser.id)
    if (res.code === 200) {
      favorites.value = res.data || []
    }
  } catch (error) {
    message.error('加载收藏失败')
  } finally {
    loading.value = false
  }
}

const removeFavorite = async (appId: number) => {
  Modal.confirm({
    title: '取消收藏',
    content: '确定要取消收藏这个应用吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await toggleAppFavorite(appId, userStore.currentUser!.id)
        if (res.code === 200) {
          message.success('已取消收藏')
          loadFavorites()
        }
      } catch (error) {
        message.error('操作失败')
      }
    }
  })
}

const openAppDetail = (app: FavoriteApp) => {
  selectedAppId.value = app.app_id
  detailModalVisible.value = true
}

const launchApp = async (app: any) => {
  const offset = windows.value.length % 5
  const newWindow: WindowState = {
    id: app.app_id,
    name: app.app_name,
    content: null,
    thumbnail: app.thumbnail,
    html: '',
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
  
  windows.value.push(newWindow)
  nextTick(() => bringToFront(newWindow))

  if (!app.app_id) {
    message.error('应用ID无效')
    closeWindow(newWindow)
    return
  }

  try {
    const res: any = await getMarketAppDetail(app.app_id)
    if (res.code === 200) {
      const targetWindow = windows.value.find(w => w.id === app.app_id)
      if (targetWindow) {
        targetWindow.content = res.data.content
        targetWindow.html = generateAppHtml(res.data.content, res.data.name)
      }
    } else {
      message.error('加载应用失败')
      closeWindow(newWindow)
    }
  } catch (error) {
    message.error('加载应用失败')
    closeWindow(newWindow)
  }
}

const generateAppHtml = (content: any, name: string) => {
  if (!content) {
    return '<div style="display: flex; justify-content: center; align-items: center; height: 100vh; color: #999;"><span>加载中...</span></div>'
  }

  const filesJson = JSON.stringify(content)
  const filesEncoded = encodeURIComponent(filesJson)

  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>${name}</title>
      <script src="https://unpkg.com/vue@3/dist/vue.global.js"><\/script>
      <script src="https://unpkg.com/vue-router@4/dist/vue-router.global.js"><\/script>
      <script src="https://unpkg.com/dayjs/dayjs.min.js"><\/script>
      <script src="https://unpkg.com/ant-design-vue@4/dist/antd.min.js"><\/script>
      <link rel="stylesheet" href="https://unpkg.com/ant-design-vue@4/dist/reset.css">
      <script src="https://unpkg.com/pinia@2/dist/pinia.global.js"><\/script>
      <script src="https://cdn.jsdelivr.net/npm/vue3-sfc-loader/dist/vue3-sfc-loader.js"><\/script>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        #app { height: 100vh; }
      </style>
    </head>
    <body>
      <div id="app"></div>
      <script>
        const files = JSON.parse(decodeURIComponent("${filesEncoded}"));
        const { loadModule } = window['vue3-sfc-loader'];
        const options = {
          moduleCache: {
            vue: window.Vue,
            'vue-router': window.VueRouter,
            'pinia': window.Pinia,
            'ant-design-vue': window.antd
          },
          async getFile(url) {
            const normalizedPath = url.replace(/^\.?\//, '');
            const fileKey = Object.keys(files).find(k => k.endsWith('/' + normalizedPath) || k === normalizedPath);
            return fileKey ? files[fileKey] : null;
          },
          addStyle(textContent) {
            const style = document.createElement('style');
            style.textContent = textContent;
            document.head.appendChild(style);
          },
        }
        
        loadModule('/App.vue', options).then(App => {
          const app = window.Vue.createApp(App);
          if (window.antd) app.use(window.antd);
          if (window.Pinia) app.use(window.Pinia.createPinia());
          const router = window.VueRouter.createRouter({
            history: window.VueRouter.createMemoryHistory(),
            routes: []
          });
          app.use(router);
          app.mount('#app');
        });
      <\/script>
    </body>
    </html>
  `
}

const closeWindow = (win: WindowState) => {
  const index = windows.value.findIndex(w => w.id === win.id)
  if (index > -1) windows.value.splice(index, 1)
}

const minimizeWindow = (win: WindowState) => {
  win.minimized = true
}

const restoreWindow = (win: WindowState) => {
  win.minimized = false
  bringToFront(win)
}

const handleDockClick = (win: WindowState) => {
  if (win.minimized) {
    restoreWindow(win)
  } else {
    minimizeWindow(win)
  }
}

const toggleFullscreen = (win: WindowState) => {
  win.isFullscreen = !win.isFullscreen
  if (win.isFullscreen) bringToFront(win)
}

const bringToFront = (win: WindowState) => {
  win.position.zIndex = ++zIndexCounter
}

let isDragging = false
let dragWindow: WindowState | null = null
let dragOffset = { x: 0, y: 0 }

const startDragHeader = (e: MouseEvent, win: WindowState) => {
  if (win.isFullscreen) return
  isDragging = true
  dragWindow = win
  dragOffset = {
    x: e.clientX - parseInt(win.position.left),
    y: e.clientY - parseInt(win.position.top)
  }
  bringToFront(win)
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging || !dragWindow) return
  const newLeft = Math.max(0, e.clientX - dragOffset.x)
  const newTop = Math.max(64, e.clientY - dragOffset.y)
  dragWindow.position = { ...dragWindow.position, top: `${newTop}px`, left: `${newLeft}px` }
}

const stopDrag = () => {
  isDragging = false
  dragWindow = null
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.my-favorites-page .header {
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

.content {
  background: #f0f2f5;
  padding: 24px 48px;
  min-height: calc(100vh - 64px);
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 2rem;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
}

.app-item {
  cursor: pointer;
  transition: all 0.3s ease;
}

.app-item:hover {
  transform: translateY(-4px);
}

.app-icon-wrapper {
  background: white;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.app-icon-wrapper.is-hovering {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transform: translateY(-4px);
}

.app-icon {
  width: 80px;
  height: 80px;
  border-radius: 18px;
  overflow: hidden;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.app-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-icon {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-size: 36px;
  color: white;
}

.app-info {
  text-align: center;
  width: 100%;
}

.app-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.app-stats {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
}

.app-stats span {
  display: flex;
  align-items: center;
  gap: 2px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.windows-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1000;
}

.app-window {
  position: absolute;
  width: 90vw;
  height: 85vh;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  pointer-events: auto;
}

.app-window.is-fullscreen {
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  border-radius: 0;
}

.app-window-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(180deg, #f8f8f8 0%, #e8e8e8 100%);
  border-bottom: 1px solid #d9d9d9;
  cursor: move;
  user-select: none;
}

.window-controls {
  display: flex;
  gap: 8px;
}

.control {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  cursor: pointer;
}

.control.close { background: #ff5f57; }
.control.minimize { background: #febc2e; }
.control.maximize { background: #28c840; }

.window-title {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.window-actions {
  display: flex;
  gap: 4px;
}

.app-window-content {
  flex: 1;
  overflow: hidden;
}

.app-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.dock-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
}

.dock-container {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: rgba(40, 40, 40, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.dock-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.dock-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-8px) scale(1.15);
}

.dock-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.dock-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dock-default-icon {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.dock-indicator {
  width: 4px;
  height: 4px;
  background: #1890ff;
  border-radius: 50%;
  margin-top: 4px;
}

.window-slide-enter-active,
.window-slide-leave-active {
  transition: all 0.3s ease;
}

.window-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.window-slide-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 深色模式样式 */
.dark-theme .content {
  background: #141414;
}
.dark-theme .page-header h1 {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .page-header p {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme .app-card {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0,0,0,0.45);
}
.dark-theme .app-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.65);
}
.dark-theme .app-name {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .app-author {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme .app-stats {
  color: rgba(255, 255, 255, 0.45);
}
.dark-theme .app-window {
  background: #1f1f1f;
  box-shadow: 0 10px 40px rgba(0,0,0,0.5);
}
.dark-theme .app-window-header {
  background: linear-gradient(180deg, #2a2a2a 0%, #1f1f1f 100%);
  border-color: #303030;
}
.dark-theme .window-title {
  color: rgba(255, 255, 255, 0.85);
}
</style>
