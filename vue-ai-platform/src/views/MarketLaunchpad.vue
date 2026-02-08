<template>
  <div class="market-launchpad">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <div class="header-left">
          <div class="logo" @click="goHome">Vue AI Platform</div>
          <a-menu theme="dark" mode="horizontal" :selectedKeys="['market']" :style="{ lineHeight: '64px' }">
            <a-menu-item key="editor" @click="goHome">编辑器</a-menu-item>
            <a-menu-item key="market">应用市场</a-menu-item>
            <a-menu-item key="my-apps" @click="goMyApps" v-if="userStore.isLoggedIn">我的应用</a-menu-item>
            <a-menu-item key="favorites" @click="goFavorites" v-if="userStore.isLoggedIn">我的收藏</a-menu-item>
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
                <a-menu-item @click="goProfile" v-if="!userStore.isGuest">
                  <UserOutlined />
                  个人中心
                </a-menu-item>
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

      <a-layout-content class="launchpad-content">
        <div class="market-header">
          <h1>应用市场</h1>
          <p>点击应用图标即可启动应用，点击卡片查看详情</p>
        </div>

        <div class="search-section">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索应用..."
            size="large"
            @search="handleSearch"
            allowClear
          />
        </div>

        <div class="filter-section">
          <a-space>
            <span>分类：</span>
            <a-radio-group v-model:value="selectedCategory" @change="handleCategoryChange" buttonStyle="solid">
              <a-radio-button value="">全部</a-radio-button>
              <a-radio-button value="工具">工具</a-radio-button>
              <a-radio-button value="游戏">游戏</a-radio-button>
              <a-radio-button value="展示">展示</a-radio-button>
              <a-radio-button value="其他">其他</a-radio-button>
            </a-radio-group>
          </a-space>
        </div>

        <a-spin :spinning="loading">
          <div class="apps-grid">
            <div
              v-for="app in filteredApps"
              :key="app.id"
              class="app-item"
              @click="launchApp(app)"
              @mouseenter="hoveredAppId = app.id"
              @mouseleave="hoveredAppId = null"
            >
              <div class="app-icon-wrapper" :class="{ 'is-hovering': hoveredAppId === app.id }">
                <div class="more-btn" v-if="hoveredAppId === app.id" @click.stop="openAppDetail(app)">
                  <MoreOutlined />
                </div>
                <div class="app-icon">
                  <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
                  <div v-else class="default-icon">
                    <AppstoreOutlined />
                  </div>
                </div>
                <div class="app-info">
                  <div class="app-name">{{ app.name }}</div>
                  <div class="app-author">{{ app.author_name }}</div>
                  <div class="app-stats">
                    <span><EyeOutlined /> {{ app.views || 0 }}</span>
                    <span><LikeOutlined /> {{ app.likes || 0 }}</span>
                  </div>
                  <div class="app-tags" v-if="app.tags && app.tags.length">
                    <a-tag v-for="tag in app.tags.slice(0, 2)" :key="tag" size="small">{{ tag }}</a-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-spin>

        <div v-if="!loading && filteredApps.length === 0" class="empty-state">
          <a-empty description="暂无发布应用" />
        </div>
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
        @mousedown="startDrag($event, win)"
      >
        <div class="app-window-header" @mousedown="startDragHeader($event, win)">
          <div class="window-controls">
            <span class="control close" @click.stop="closeWindow(win)"></span>
            <span class="control minimize" @click.stop="minimizeWindow(win)"></span>
            <span class="control maximize" @click.stop="toggleFullscreen(win)"></span>
          </div>
          <div class="window-title">{{ win.name }}</div>
          <div class="window-actions">
            <a-button
              type="text"
              size="small"
              @click.stop="toggleFullscreen(win)"
              :class="{ 'fullscreen-btn': true }"
            >
              <FullscreenOutlined v-if="!win.isFullscreen" />
              <FullscreenExitOutlined v-else />
            </a-button>
            <a-button type="text" size="small" @click.stop="openInNewTab(win)">
              <ExportOutlined />
            </a-button>
          </div>
        </div>
        <div class="app-window-content">
          <iframe
            :ref="el => setIframeRef(el, win.id)"
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
          @mouseenter="hoveredDockId = win.id"
          @mouseleave="hoveredDockId = null"
        >
          <div class="dock-icon">
            <img v-if="win.thumbnail" :src="win.thumbnail" :alt="win.name" />
            <div v-else class="dock-default-icon">
              <AppstoreOutlined />
            </div>
          </div>
          <div class="dock-indicator" v-if="!win.minimized"></div>
        </div>
        <div class="dock-separator" v-if="windows.length > 0"></div>
        <div class="dock-trash" @click="closeAllWindows" title="关闭所有">
          <DeleteOutlined />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  DownOutlined,
  LogoutOutlined,
  LoginOutlined,
  AppstoreOutlined,
  MoreOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  ExportOutlined,
  DeleteOutlined,
  EyeOutlined,
  LikeOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMarketApps, getMarketAppDetail } from '@/api'
import { AppDetailModal } from '@/components/community'

interface WindowState {
  id: number
  name: string
  content: any
  thumbnail?: string
  html: string
  minimized: boolean
  isFullscreen: boolean
  isActive: boolean
  position: { top: string; left: string; width: string; height: string; zIndex: number }
}

const router = useRouter()
const userStore = useUserStore()

const apps = ref<any[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const selectedCategory = ref('')
const hoveredAppId = ref<number | null>(null)
const hoveredDockId = ref<number | null>(null)
const windows = ref<WindowState[]>([])
const iframeRefs = ref<Record<number, HTMLIFrameElement | null>>({})
const detailModalVisible = ref(false)
const selectedAppId = ref<number | undefined>(undefined)

let zIndexCounter = 1000
let isDragging = false
let dragWindow: WindowState | null = null
let dragOffset = { x: 0, y: 0 }
let dragStartPos = { x: 0, y: 0 }

const setIframeRef = (el: any, id: number) => {
  iframeRefs.value[id] = el as HTMLIFrameElement
}

const filteredApps = computed(() => {
  return apps.value.filter(app => {
    const matchKeyword = !searchKeyword.value ||
      app.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      app.description.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchCategory = !selectedCategory.value || 
      (app.tags && app.tags.some((t: string) => t.includes(selectedCategory.value)))
    return matchKeyword && matchCategory
  })
})

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
      <script src="https://unpkg.com/dayjs/plugin/customParseFormat.js"><\/script>
      <script src="https://unpkg.com/dayjs/plugin/weekday.js"><\/script>
      <script src="https://unpkg.com/dayjs/plugin/localeData.js"><\/script>
      <script src="https://unpkg.com/dayjs/plugin/weekOfYear.js"><\/script>
      <script src="https://unpkg.com/dayjs/plugin/weekYear.js"><\/script>
      <script src="https://unpkg.com/dayjs/plugin/advancedFormat.js"><\/script>
      <script src="https://unpkg.com/dayjs/plugin/quarterOfYear.js"><\/script>
      <script>
        dayjs.extend(window.dayjs_plugin_customParseFormat);
        dayjs.extend(window.dayjs_plugin_weekday);
        dayjs.extend(window.dayjs_plugin_localeData);
        dayjs.extend(window.dayjs_plugin_weekOfYear);
        dayjs.extend(window.dayjs_plugin_weekYear);
        dayjs.extend(window.dayjs_plugin_advancedFormat);
        dayjs.extend(window.dayjs_plugin_quarterOfYear);
      <\/script>
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
        
        const fileKeys = Object.keys(files);
        
        const normalizePath = (path) => {
          if (path.startsWith('/')) path = path.slice(1);
          if (path.startsWith('./')) path = path.slice(2);
          return path;
        };
        
        const getFileContent = (url) => {
          const normalizedPath = normalizePath(url);
          
          if (files[normalizedPath]) {
            return files[normalizedPath];
          }
          
          for (const key of fileKeys) {
            if (key.endsWith('/' + normalizedPath) || key === normalizedPath) {
              return files[key];
            }
            
            if (normalizedPath.endsWith('.vue')) {
              const fileName = normalizedPath.split('/').pop();
              if (key.endsWith(fileName)) {
                return files[key];
              }
            }
          }
          
          const foundKey = fileKeys.find(key => 
            key.toLowerCase() === normalizedPath.toLowerCase() ||
            key.toLowerCase().endsWith('/' + normalizedPath.toLowerCase())
          );
          
          if (foundKey) {
            return files[foundKey];
          }
          
          return null;
        };
        
        const options = {
          moduleCache: {
            vue: window.Vue,
            'vue-router': window.VueRouter,
            'pinia': window.Pinia,
            'ant-design-vue': window.antd
          },
          async getFile(url) {
            const content = getFileContent(url);
            
            if (content) {
              return content;
            }
            
            await new Promise(resolve => setTimeout(resolve, 100));
            const retryContent = getFileContent(url);
            if (retryContent) return retryContent;
            
            return \`<template><div style="padding:20px;color:#666;">组件加载失败: \${url}</div></template>\`;
          },
          addStyle(textContent) {
            const style = document.createElement('style');
            style.textContent = textContent;
            document.head.appendChild(style);
          },
        }
        
        const { loadModule } = window['vue3-sfc-loader'];
        
        const startApp = () => {
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
          }).catch(err => {
            document.getElementById('app').innerHTML = '<div style="padding:20px;color:#f5222d;">应用加载失败</div>';
          });
        };
        
        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', startApp);
        } else {
          startApp();
        }
      <\/script>
    </body>
    </html>
  `
}

const goHome = () => {
  router.push('/project/new')
}

const goMyApps = () => {
  router.push('/my-apps')
}

const goFavorites = () => {
  router.push('/favorites')
}

const goProfile = () => {
  router.push('/profile')
}

const goLogin = () => {
  userStore.logout()
  router.push('/login')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
  message.success('已退出登录')
}

const handleSearch = () => {}
const handleCategoryChange = () => {}

const openAppDetail = (app: any) => {
  selectedAppId.value = app.id
  detailModalVisible.value = true
}

const launchApp = async (app: any) => {
  // 关闭详情弹窗
  detailModalVisible.value = false
  
  const existingWindow = windows.value.find(w => w.id === app.id)
  
  if (existingWindow) {
    if (existingWindow.minimized) {
      restoreWindow(existingWindow)
    } else {
      bringToFront(existingWindow)
    }
    return
  }

  const offset = windows.value.length % 5
  const newWindow: WindowState = {
    id: app.id,
    name: app.name,
    content: null,
    thumbnail: app.thumbnail,
    html: generateAppHtml(null, app.name),
    minimized: false,
    isFullscreen: false,
    isActive: true,
    position: {
      top: `${80 + offset * 30}px`,
      left: `${100 + offset * 40}px`,
      width: '90vw',
      height: '85vh',
      zIndex: ++zIndexCounter
    }
  }
  
  windows.value.forEach(w => w.isActive = false)
  windows.value.push(newWindow)
  
  nextTick(() => {
    bringToFront(newWindow)
  })

  try {
    const res: any = await getMarketAppDetail(app.id)
    if (res.code === 200) {
      const targetWindow = windows.value.find(w => w.id === app.id)
      if (targetWindow) {
        targetWindow.content = res.data.content
        targetWindow.html = generateAppHtml(res.data.content, res.data.name)
      }
    } else {
      message.error(res.message || '加载应用失败')
      const targetWindow = windows.value.find(w => w.id === app.id)
      if (targetWindow) {
        closeWindow(targetWindow)
      }
    }
  } catch (error: any) {
    message.error('加载应用失败: ' + (error.message || '未知错误'))
    const targetWindow = windows.value.find(w => w.id === app.id)
    if (targetWindow) {
      closeWindow(targetWindow)
    }
  }
}

const closeWindow = (win: WindowState) => {
  const index = windows.value.findIndex(w => w.id === win.id)
  if (index > -1) {
    windows.value.splice(index, 1)
  }
}

const minimizeWindow = (win: WindowState) => {
  win.minimized = true
  win.isActive = false
  
  const activeWindow = windows.value.find(w => !w.minimized)
  if (activeWindow) {
    activeWindow.isActive = true
    bringToFront(activeWindow)
  }
}

const restoreWindow = (win: WindowState) => {
  win.minimized = false
  windows.value.forEach(w => w.isActive = w.id === win.id)
  bringToFront(win)
}

const handleDockClick = (win: WindowState) => {
  if (win.minimized) {
    restoreWindow(win)
  } else if (!win.isActive) {
    bringToFront(win)
    windows.value.forEach(w => w.isActive = w.id === win.id)
  } else {
    minimizeWindow(win)
  }
}

const toggleFullscreen = (win: WindowState) => {
  win.isFullscreen = !win.isFullscreen
  if (win.isFullscreen) {
    bringToFront(win)
  }
}

const bringToFront = (win: WindowState) => {
  win.position.zIndex = ++zIndexCounter
  windows.value.forEach(w => w.isActive = w.id === win.id)
}

const openInNewTab = (win: WindowState) => {
  const url = `/market/app/${win.id}`
  window.open(url, '_blank')
}

const closeAllWindows = () => {
  windows.value = []
}

const startDrag = (e: MouseEvent, win: WindowState) => {
  if (win.isFullscreen) return
  bringToFront(win)
}

const startDragHeader = (e: MouseEvent, win: WindowState) => {
  if (win.isFullscreen) return
  isDragging = true
  dragWindow = win
  dragOffset = {
    x: e.clientX - parseInt(win.position.left),
    y: e.clientY - parseInt(win.position.top)
  }
  dragStartPos = {
    x: e.clientX,
    y: e.clientY
  }
  bringToFront(win)
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging || !dragWindow) return
  
  const newLeft = Math.max(0, e.clientX - dragOffset.x)
  const newTop = Math.max(64, e.clientY - dragOffset.y)
  
  dragWindow.position = {
    ...dragWindow.position,
    top: `${newTop}px`,
    left: `${newLeft}px`
  }
}

const stopDrag = () => {
  isDragging = false
  dragWindow = null
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    const activeWindow = windows.value.find(w => w.isActive && !w.minimized)
    if (activeWindow) {
      minimizeWindow(activeWindow)
    }
  }
}

const loadApps = async () => {
  loading.value = true
  try {
    const res: any = await getMarketApps({
      keyword: searchKeyword.value,
      category: selectedCategory.value,
      page: 1,
      pageSize: 100
    })
    
    if (res.code === 200) {
      apps.value = res.data?.list || []
    } else {
      message.error(res.message || '加载应用列表失败')
    }
  } catch (error: any) {
    message.error('加载应用列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadApps()
  document.addEventListener('keydown', handleKeydown)
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

.launchpad-content {
  background: #f0f2f5;
  padding: 24px 48px;
}

.market-header {
  text-align: center;
  margin-bottom: 32px;
}

.market-header h1 {
  font-size: 2rem;
  margin-bottom: 8px;
}

.market-header p {
  color: #666;
  font-size: 1rem;
}

.search-section {
  max-width: 600px;
  margin: 0 auto 24px;
}

.filter-section {
  margin-bottom: 32px;
  text-align: center;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
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
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  position: relative;
}

.more-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
  z-index: 2;
}

.more-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #1890ff;
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
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.app-tags {
  display: flex;
  justify-content: center;
  gap: 4px;
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
  transition: box-shadow 0.3s ease;
}

.app-window:hover {
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.3);
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

.app-window-header:hover {
  background: linear-gradient(180deg, #f0f0f0 0%, #e0e0e0 100%);
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
  transition: transform 0.15s ease;
}

.control:hover {
  transform: scale(1.1);
}

.control.close {
  background: #ff5f57;
}

.control.close:hover {
  background: #ff3b30;
}

.control.minimize {
  background: #febc2e;
}

.control.minimize:hover {
  background: #f5a623;
}

.control.maximize {
  background: #28c840;
}

.control.maximize:hover {
  background: #1db954;
}

.window-title {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.window-actions {
  display: flex;
  gap: 4px;
}

.window-actions .ant-btn {
  color: #666;
}

.window-actions .ant-btn:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
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
  pointer-events: auto;
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
  border: 1px solid rgba(255, 255, 255, 0.1);
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

.dock-item.is-active .dock-icon {
  box-shadow: 0 0 12px rgba(24, 144, 255, 0.6);
}

.dock-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.2s ease;
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

.dock-separator {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 8px;
}

.dock-trash {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dock-trash:hover {
  background: rgba(255, 59, 48, 0.3);
  color: #ff3b30;
  transform: translateY(-4px);
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
</style>
