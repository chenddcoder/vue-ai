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

      <a-layout-content class="launchpad-content">
        <div class="market-header">
          <h1>应用市场</h1>
          <p>点击应用图标即可启动应用</p>
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
                <div class="app-icon">
                  <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
                  <div v-else class="default-icon">
                    <AppstoreOutlined />
                  </div>
                </div>
                <div class="app-info">
                  <div class="app-name">{{ app.name }}</div>
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

    <div v-if="launchingApp" class="app-modal">
      <div class="modal-backdrop" @click="closeApp"></div>
      <div class="app-window">
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
            <a-button type="text" size="small" @click="openInNewTab">
              <ExportOutlined />
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  DownOutlined,
  LogoutOutlined,
  LoginOutlined,
  AppstoreOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMarketApps, getMarketAppDetail } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const apps = ref<any[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const selectedCategory = ref('')
const hoveredAppId = ref<number | null>(null)
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
        
        const resolvePath = (basePath, relativePath) => {
          const parts = basePath.split('/').filter(Boolean);
          const relativeParts = relativePath.split('/').filter(Boolean);
          
          for (const part of relativeParts) {
            if (part === '..') {
              parts.pop();
            } else if (part !== '.') {
              parts.push(part);
            }
          }
          
          return parts.join('/');
        };
        
        const normalizePath = (path) => {
          if (path.startsWith('/')) path = path.slice(1);
          if (path.startsWith('./')) path = path.slice(2);
          return path;
        };
        
        const getFileContent = (path) => {
          const normalizedPath = normalizePath(path);
          
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
          
          console.warn('File not found:', path, 'Searched:', normalizedPath);
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

const goHome = () => {
  router.push('/project/new')
}

const goMyApps = () => {
  router.push('/my-apps')
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

const handleSearch = () => {
}

const handleCategoryChange = () => {
}

const launchApp = async (app: any) => {
  // 先显示加载状态
  launchingApp.value = { ...app, content: null }
  
  try {
    // 获取应用详情（包含完整代码内容）
    const res: any = await getMarketAppDetail(app.id)
    if (res.code === 200) {
      launchingApp.value = { 
        ...res.data, 
        content: res.data.content || {} 
      }
      
      if (appFrame.value) {
        appFrame.value.focus()
      }
    } else {
      message.error(res.message || '加载应用失败')
      launchingApp.value = null
    }
  } catch (error: any) {
    message.error('加载应用失败: ' + (error.message || '未知错误'))
    launchingApp.value = null
  }
}

const closeApp = () => {
  launchingApp.value = null
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const openInNewTab = () => {
  if (launchingApp.value) {
    const url = `/market/app/${launchingApp.value.id}`
    window.open(url, '_blank')
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    closeApp()
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
}

.app-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.app-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
}

.app-window {
  position: relative;
  width: 90vw;
  height: 85vh;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
  z-index: 1001;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.app-window:fullscreen {
  width: 100vw;
  height: 100vh;
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
</style>
