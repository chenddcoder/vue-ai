<template>
  <div class="launchpad-overlay" @click.self="handleClose">
    <div class="launchpad-container">
      <div class="launchpad-header">
        <h2>应用启动台</h2>
        <a-button type="text" @click="handleClose">
          <CloseOutlined />
        </a-button>
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
          <a-radio-group v-model:value="selectedCategory" @change="handleCategoryChange" buttonStyle="solid">
            <a-radio-button value="">全部</a-radio-button>
            <a-radio-button value="工具">工具</a-radio-button>
            <a-radio-button value="游戏">游戏</a-radio-button>
            <a-radio-button value="展示">展示</a-radio-button>
            <a-radio-button value="其他">其他</a-radio-button>
          </a-radio-group>
        </a-space>
      </div>

      <div class="apps-grid" v-if="!loading">
        <div
          v-for="app in filteredApps"
          :key="app.id"
          class="app-icon-item"
          @click="launchApp(app)"
          @mouseenter="handleMouseEnter(app.id)"
          @mouseleave="handleMouseLeave"
        >
          <div class="app-icon-wrapper" :class="{ 'is-hovering': hoveringAppId === app.id }">
            <div class="app-icon">
              <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
              <div v-else class="default-icon">
                <AppstoreOutlined />
              </div>
            </div>
            <div class="app-name">{{ app.name }}</div>
          </div>
        </div>
      </div>

      <div v-if="!loading && filteredApps.length === 0" class="empty-state">
        <a-empty description="暂无应用" />
      </div>

      <div v-if="loading" class="loading-state">
        <a-spin size="large" tip="加载应用中..." />
      </div>
    </div>

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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  CloseOutlined,
  AppstoreOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { getMarketApps } from '@/api'

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

const emit = defineEmits<{
  (e: 'close'): void
}>()

const searchKeyword = ref('')
const selectedCategory = ref('')
const apps = ref<any[]>([])
const loading = ref(true)
const hoveringAppId = ref<number | null>(null)
const hoveredDockId = ref<number | null>(null)
const windows = ref<WindowState[]>([])
const iframeRefs = ref<Record<number, HTMLIFrameElement | null>>({})

let zIndexCounter = 1000
let isDragging = false
let dragWindow: WindowState | null = null
let dragOffset = { x: 0, y: 0 }

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

  let appContent = content
  if (typeof content === 'string') {
    try {
      appContent = JSON.parse(content)
    } catch (e) {
      console.error('Failed to parse app content:', e)
    }
  }

  const filesJson = JSON.stringify(appContent)
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
      <script src="https://unpkg.com/file-saver/dist/FileSaver.min.js"><\/script>
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
            'ant-design-vue': window.antd,
            'file-saver': { saveAs: window.saveAs }
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

const handleMouseEnter = (id: number) => {
  hoveringAppId.value = id
}

const handleMouseLeave = () => {
  hoveringAppId.value = null
}

const handleSearch = () => {}
const handleCategoryChange = () => {}

const launchApp = (app: any) => {
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
    content: app.content,
    thumbnail: app.thumbnail,
    html: generateAppHtml(app.content, app.name),
    minimized: false,
    isFullscreen: false,
    isActive: true,
    position: {
      top: `${80 + offset * 30}px`,
      left: `${100 + offset * 40}px`,
      width: 'calc(100vw - 100px)',
      height: 'calc(100vh - 130px)',
      zIndex: ++zIndexCounter
    }
  }
  
  windows.value.forEach(w => w.isActive = false)
  windows.value.push(newWindow)
  
  nextTick(() => {
    bringToFront(newWindow)
  })
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

const handleClose = () => {
  if (windows.value.length === 0) {
    emit('close')
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (windows.value.length > 0) {
      const activeWindow = windows.value.find(w => w.isActive && !w.minimized)
      if (activeWindow) {
        minimizeWindow(activeWindow)
      }
    } else {
      emit('close')
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

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.launchpad-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(20px);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 60px 80px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.launchpad-container {
  width: 100%;
  max-width: 1400px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.launchpad-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.launchpad-header h2 {
  color: white;
  font-size: 2rem;
  font-weight: 600;
  margin: 0;
}

.launchpad-header .ant-btn {
  color: white;
  font-size: 20px;
}

.search-section {
  max-width: 500px;
  margin: 0 auto 20px;
  width: 100%;
}

.filter-section {
  text-align: center;
  margin-bottom: 40px;
}

.filter-section :deep(.ant-radio-button-wrapper) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.8);
}

.filter-section :deep(.ant-radio-button-wrapper:hover) {
  color: white;
}

.filter-section :deep(.ant-radio-button-wrapper-checked) {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 40px 32px;
  padding: 20px 0;
  overflow-y: auto;
  flex: 1;
}

.app-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

.app-icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  transition: all 0.2s ease;
}

.app-icon-wrapper.is-hovering {
  background: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.app-icon {
  width: 80px;
  height: 80px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  transition: all 0.2s ease;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-icon-wrapper.is-hovering .app-icon {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
  transform: scale(1.08);
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
  font-size: 36px;
  color: white;
}

.app-name {
  margin-top: 12px;
  color: white;
  font-size: 14px;
  text-align: center;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
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
