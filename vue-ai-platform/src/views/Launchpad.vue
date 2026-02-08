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
          <a-radio-group v-model:value="selectedCategory" @change="handleCategoryChange">
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

    <div v-if="launchingApp" class="app-window">
      <div class="app-window-header">
        <div class="window-controls">
          <span class="control close" @click="closeApp"></span>
          <span class="control minimize"></span>
          <span class="control maximize"></span>
        </div>
        <div class="window-title">{{ launchingApp.name }}</div>
        <div class="window-actions">
          <a-button type="text" size="small" @click="toggleFullscreen">
            <FullscreenOutlined v-if="!isFullscreen" />
            <FullscreenExitOutlined v-else />
          </a-button>
        </div>
      </div>
      <div class="app-window-content">
        <iframe
          ref="appFrame"
          class="app-frame"
          :srcdoc="appHtml"
          sandbox="allow-scripts allow-same-origin"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  CloseOutlined,
  AppstoreOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined
} from '@ant-design/icons-vue'
import { getMarketApps } from '@/api'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const searchKeyword = ref('')
const selectedCategory = ref('')
const apps = ref<any[]>([])
const loading = ref(true)
const hoveringAppId = ref<number | null>(null)
const launchingApp = ref<any>(null)
const appFrame = ref<HTMLIFrameElement | null>(null)
const isFullscreen = ref(false)

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

const appHtml = computed(() => {
  if (!launchingApp.value?.content) {
    return '<div style="display: flex; justify-content: center; align-items: center; height: 100vh; color: #999;"><span>加载中...</span></div>'
  }

  const filesJson = JSON.stringify(launchingApp.value.content)
  const filesEncoded = encodeURIComponent(filesJson)

  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>${launchingApp.value?.name || 'App'}</title>
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
          
          console.warn('File not found:', url, 'Searched:', normalizedPath);
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
            
            console.error('File not found:', url);
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
            console.error('App load error:', err);
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
})

const handleMouseEnter = (id: number) => {
  hoveringAppId.value = id
}

const handleMouseLeave = () => {
  hoveringAppId.value = null
}

const handleSearch = () => {
}

const handleCategoryChange = () => {
}

const launchApp = (app: any) => {
  launchingApp.value = app
  if (appFrame.value) {
    appFrame.value.focus()
  }
}

const closeApp = () => {
  launchingApp.value = null
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const handleClose = () => {
  if (!launchingApp.value) {
    emit('close')
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (launchingApp.value) {
      closeApp()
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

.app-window {
  position: fixed;
  top: 50px;
  left: 50px;
  right: 50px;
  bottom: 50px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.app-window:fullscreen {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 0;
}

.app-window-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #e8e8e8;
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

.control.close {
  background: #ff5f57;
}

.control.minimize {
  background: #febc2e;
}

.control.maximize {
  background: #28c840;
}

.window-title {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.window-actions {
  width: 60px;
  text-align: right;
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
</style>
