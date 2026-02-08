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
            
            <div class="file-structure" v-if="!loading && Object.keys(appInfo.content).length > 0">
              <div class="structure-header">
                <h3>项目结构</h3>
                <a-tag v-if="!appInfo.isOpenSource" color="orange">闭源</a-tag>
              </div>
              <div class="structure-content" v-if="appInfo.isOpenSource">
                <a-tree
                  :tree-data="treeData"
                  :show-icon="true"
                  :default-expand-all="true"
                  block-node
                  class="file-tree"
                >
                  <template #switcherIcon="{ dataRef, expanded }">
                    <DownOutlined v-if="!dataRef.isLeaf && expanded" style="font-size: 10px" />
                    <RightOutlined v-else-if="!dataRef.isLeaf" style="font-size: 10px" />
                  </template>
                  <template #icon="{ dataRef }">
                    <FolderOutlined v-if="!dataRef.isLeaf && !dataRef.expanded" />
                    <FolderOpenOutlined v-else-if="!dataRef.isLeaf && dataRef.expanded" />
                    <FileOutlined v-else />
                  </template>
                </a-tree>
              </div>
              <div class="structure-content empty" v-else>
                <div class="protected-msg">
                  <LockOutlined style="font-size: 24px; margin-bottom: 8px" />
                  <p>该应用代码已保护，不可查看</p>
                </div>
              </div>
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
                :disabled="!appInfo.isOpenSource"
              >
                <CopyOutlined />
                {{ appInfo.isOpenSource ? '使用该模板' : '代码已保护' }}
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
  EditOutlined,
  FileOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  RightOutlined,
  LockOutlined
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
  content: {},
  isOpenSource: true
})

interface TreeNode {
  title: string
  key: string
  isLeaf?: boolean
  children?: TreeNode[]
  expanded?: boolean
}

const buildTree = (files: Record<string, string>): TreeNode[] => {
  const root: TreeNode[] = []
  const nodeMap: Record<string, TreeNode> = {}
  
  const filenames = Object.keys(files).sort()
  
  filenames.forEach(filename => {
    const parts = filename.split('/')
    let currentPath = ''
    let parentPath = ''
    
    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isLast = i === parts.length - 1
      
      currentPath = currentPath ? `${currentPath}/${part}` : part
      
      if (isLast) {
        if (part !== '.placeholder') {
          const fileNode: TreeNode = {
            title: part,
            key: currentPath,
            isLeaf: true
          }
          
          if (parentPath && nodeMap[parentPath]) {
            nodeMap[parentPath].children?.push(fileNode)
          } else {
            root.push(fileNode)
          }
        }
      } else {
        if (!nodeMap[currentPath]) {
          const dirNode: TreeNode = {
            title: part,
            key: currentPath,
            isLeaf: false,
            children: []
          }
          nodeMap[currentPath] = dirNode
          
          if (parentPath && nodeMap[parentPath]) {
            nodeMap[parentPath].children?.push(dirNode)
          } else {
            root.push(dirNode)
          }
        }
      }
      parentPath = currentPath
    }
  })
  
  return root
}

const treeData = computed(() => {
  if (!appInfo.value.content) return []
  return buildTree(appInfo.value.content)
})

const previewHtml = computed(() => {
  if (!appInfo.value.content || Object.keys(appInfo.value.content).length === 0) {
    return '<div style="display: flex; justify-content: center; align-items: center; height: 100vh; color: #999;">暂无预览内容</div>'
  }
  
  // 准备文件数据
  const filesJson = JSON.stringify(appInfo.value.content)
  const filesEncoded = encodeURIComponent(filesJson)
  
  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
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
      </style>
    </head>
    <body>
      <div id="app"></div>
      <script>
        const files = JSON.parse(decodeURIComponent("${filesEncoded}"));
        
        const options = {
          moduleCache: {
            vue: window.Vue,
            'vue-router': window.VueRouter,
            'pinia': window.Pinia,
            'ant-design-vue': window.antd
          },
          async getFile(url) {
            // 简单的路径处理
            // url 可能是 '/App.vue', './components/Header.vue' 等
            // 我们需要将其映射到 files 对象的 key
            
            let path = url;
            if (path.startsWith('/')) path = path.slice(1);
            if (path.startsWith('./')) path = path.slice(2);
            
            // 处理相对路径引用 (简化版，假设都是相对于根目录或当前目录的简单引用)
            // 实际项目中可能需要更复杂的路径解析逻辑
            
            const content = files[path];
            if (!content) {
              // 尝试查找是否有匹配的 key (处理可能的路径差异)
              const foundKey = Object.keys(files).find(key => key.endsWith(path) || path.endsWith(key));
              if (foundKey) return files[foundKey];
              
              console.error('File not found:', url);
              return Promise.reject(new Error('File not found: ' + url));
            }
            return content;
          },
          addStyle(textContent) {
            const style = document.createElement('style');
            style.textContent = textContent;
            document.head.appendChild(style);
          },
        }
        
        const { loadModule } = window['vue3-sfc-loader'];
        
        loadModule('/App.vue', options).then(App => {
          const app = window.Vue.createApp(App);
          if (window.antd) app.use(window.antd);
          if (window.Pinia) app.use(window.Pinia.createPinia());
          
          const router = window.VueRouter.createRouter({
            history: window.VueRouter.createWebHashHistory(),
            routes: []
          });
          app.use(router);

          app.mount('#app');
        });
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
        content: res.data.content || {},
        isOpenSource: res.data.is_open_source !== 0 // Default to true if not present or 1
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
  display: flex;
  flex-direction: column;
}

.preview-box {
  position: relative;
  height: 600px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.file-structure {
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.structure-header {
  padding: 12px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.structure-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.structure-content {
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.protected-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
}

:deep(.file-tree) {
  background: transparent;
}

.loading-box {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #fafafa;
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
